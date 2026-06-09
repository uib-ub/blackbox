# Blackbox — Security Review

**Scope:** `src/main/` (application code, `web.xml`, `docker/Dockerfile`) plus the
`marcus-search-client/blackbox` Kubernetes deployment. Tests excluded.

**Environment (assumptions confirmed during review):**

- Exposed to the public internet via ingress, **no WAF / no rate-limiting** in front.
- Elasticsearch API key is **read-only and restricted to an allowlist of indices**.
- Output: findings report only (no code/manifest changes made for this review).

**Net assessment:** No memory-safety or DSL-injection bugs — `aggs` are parsed into typed
objects rather than string-concatenated into the query. The real exposure is
**availability / abuse of a public, unthrottled endpoint** and **information disclosure**,
plus a **platform-hardening gap** (read-only root filesystem). The read-only, allowlisted
API key meaningfully caps the worst-case outcomes.

> Premise correction discovered during review: `readOnlyRootFilesystem` is **commented out
> in both** `blackbox/deployment.yaml` and `blackbox-admin/deployment.yaml` (line 36), so it
> is **not enforced today**. The working reference pattern is the SKA blackbox deployment
> (`base/skeivtarkiv/ska-search/deployment-blackbox.yaml`): an `emptyDir` volume + an init
> container seeding `/usr/local/tomcat`, mounted writable, with `readOnlyRootFilesystem: true`
> on the container. The sibling *frontend* deployments (`marcus/`, `admin/`, `naturen/`) do
> set it — only the two blackbox pods don't.

---

## High

### H1 — Public endpoint, no rate limiting, expensive queries + unbounded aggregations → ES DoS

**Locations:** `MarcusSearchBuilder` / `WabSearchBuilder` (`query_string`), `AggregationUtils`
(no caps), `SearchServlet` (`from` uncapped, no ES-side timeout).

- `q` is fed to **`query_string`** (Marcus, and — after the migration — WAB, which previously
  used the safer `simple_query_string`). `query_string` permits leading wildcards, `regexp`,
  fuzzy, and large boolean expansions. The signature logic *intentionally* uses leading
  wildcards (`*token*`), so expensive queries can't simply be disabled cluster-wide.
- `aggs` has **no cap on `size` nor on the number of aggregation objects** — one request can
  ask for huge terms aggregations or many date histograms.
- `from` is capped only at `Integer.MAX_VALUE`; `size` is capped at 1000. `trackTotalHits` is
  forced to 500 000 (Marcus) / 100 000 (WAB), making every query do expensive hit-counting.
- No search-level `timeout` or `terminate_after`; requests use `RequestOptions.DEFAULT`.

With a public endpoint and nothing throttling it, a handful of crafted requests can saturate
the ES cluster and Tomcat's thread pool.

**Recommended (in priority order):**
1. Ingress rate-limiting / connection limits — highest-value control given there is no WAF.
2. Add `timeout` + `terminate_after` to the query body to bound ES work regardless of HTTP timeouts.
3. Cap `aggs` `size` and the number of aggregation objects.
4. Cap `from` (e.g. reject `from + size > 10 000`, matching ES `max_result_window`); lower `trackTotalHits`.
5. `q` validation — see the status note below (a length cap is the cheap win; `simple_query_string` is **not** a viable swap here).

**Status (implemented on this branch):**

- `timeout` = `Params.SEARCH_TIMEOUT` (`3s`) and `terminate_after` = `Params.TERMINATE_AFTER`
  (`500_000`, an upper guard >= the `trackTotalHits` ceiling, so no count/facet accuracy change)
  set on Marcus, WAB, and Naturen (inherited via `super`).
- `q` length cap = `Params.MAX_QUERY_LENGTH` (`512`), enforced in `SearchServlet` and
  `SuggestionServlet` -> 400 JSON.
- **Still open:** ingress rate-limiting (the main residual DoS lever — aggregate concurrent
  load); `aggs` size/count caps and a `from` cap were deprioritized (H1 deemed good enough).

**Note on `q` / `query_string`:** Blocklist/regex validation of `q` was intentionally *not*
added — it is fragile and bypassable, and Lucene/ES already bound the catastrophic single-query
cases (regex `max_determinized_states`, boolean `max_clause_count`, fuzzy <= 2), with the 3s
timeout + `terminate_after` covering slow execution. A single crafted Lucene query cannot hang
the cluster; residual DoS is aggregate load -> rate-limiting. The app intentionally depends on
`query_string` **leading wildcards** (Marcus `*bros-2000*`, WAB `*some-ref*`), so
`simple_query_string` / disabling leading wildcards is **not** viable without breaking signature
search. The remaining `q` concern is **field targeting** (`anyField:value`, `_exists_:field`),
which is acceptable here because the public indices are confirmed public-only (see M2).

### H2 — Raw Elasticsearch error bodies streamed to clients → information disclosure

**Location:** `SearchServlet` `catch (ResponseException)` → `e.getResponse().getEntity().writeTo(out)`
with the ES status code.

ES error JSON typically contains index names, field names, mappings, analyzer details,
shard/cluster internals, ES version, and sometimes stack traces. On a public endpoint this
hands an attacker a map of the cluster and schema. (This is a behavior change from the old
code, which returned a generic error.)

**Recommended:** return a generic JSON error to the client (e.g.
`{"error":"search failed","status":...}`) and log the full ES body server-side at WARNING.
Keep the existing 400 path for `IllegalArgumentException` (those messages are app-controlled).

**Status (implemented on this branch):** `SearchServlet`'s `ResponseException` handler now logs
the full ES error server-side and returns a generic `{"error":...,"status":N}` body (preserving
the ES status code) instead of streaming the ES entity. This closes the search-path leak.

---

## Medium

### M1 — `readOnlyRootFilesystem` not enforced (platform-hardening gap)

**Location:** `marcus-search-client/blackbox/deployment.yaml:36` (and `blackbox-admin/deployment.yaml:36`)
— commented out. `runAsUser` also commented (mitigated by image `USER 10001` + `runAsNonRoot: true`).

The container root FS is writable, so a code-exec foothold can drop tooling, tamper with the
deployed WAR / Tomcat config, or persist. This is defense-in-depth that was believed to be on
but is actually off.

**Recommended:** mirror the SKA blackbox pattern — add `emptyDir` volume(s) for the writable
paths Tomcat needs (`/usr/local/tomcat/work`, `/usr/local/tomcat/temp`, and `/var/log/blackbox`
if the access-log valve is enabled), then set `readOnlyRootFilesystem: true`. SKA mounts the
whole `/usr/local/tomcat` from an `emptyDir` seeded by an init container; narrower mounts for
`work`/`temp`/`logs` are cleaner if feasible. Apply to **both** blackbox and blackbox-admin.

### M2 — No field/document allowlist; aggregations & `query_string` can enumerate any field

Even with index allowlisting, a caller can aggregate on *any* field (terms aggs return the
actual top values) or probe with `field:value` / `_exists_:field`. If the allowlisted indices
contain any non-public fields or unpublished documents, those values/records are reachable —
the app adds no "is-public/published" filter.

**Recommended:** confirm the public search indices contain **only** publicly viewable documents
and fields (key assumption to validate). If not, add a server-side filter (e.g. `published:true`)
and/or an allowlist of aggregatable/searchable fields.

**Status:** Confirmed — the public `marcus-search-client` indices contain only publicly viewable
content. Restricted/admin content lives behind the separate `blackbox-admin` deployment, which is
password- and IP-restricted. No app-side change needed for the public proxy.

### M3 — `automountServiceAccountToken` not disabled

**Location:** both deployments. The default service-account token is mounted; the app never
calls the Kubernetes API. Post-exploit, that token aids lateral movement.

**Recommended:** set `automountServiceAccountToken: false` on the pod spec.

### M4 — Container image scanning exists in CI but is disabled

**Location:** `.gitlab-ci.yml` — the `security_scan` job has `parallel: matrix: []` (empty),
so no scan jobs run. Dependencies (ES client 9.3.3, jackson-databind 2.21.2, Jakarta APIs) and
the base image (`tomcat:11.0.22-jdk25-temurin`) ship unscanned.

**Recommended:** enable the Trivy job (populate the matrix), pin the base image by digest, and
keep Tomcat / Jackson on current patch releases. (log4j is already removed — no Log4Shell exposure.)

### M5 — CORS `*` on all paths

**Location:** `web.xml` Tomcat `CorsFilter`, `cors.allowed.origins=*`, mapped to `/*`.

No credentials are used and the data is public, so impact is limited — but it lets any site
read responses in a browser, which amplifies H2 if errors leak.

**Recommended:** restrict to the known frontend origins (marcus/ska/wab/naturen).

---

## Low / hardening

- **L1 — Public architecture disclosure:** `/blackbox/index.html` plus
  `images/class_diagram*.png` and `elasticsearch-setup-ub.png` are served publicly
  (DefaultServlet + welcome-file) and reveal internal design / ES topology. Remove from the WAR
  or restrict. Note: `index.html` is also the liveness-probe target — change the probe if removing it.
- **L2 — Incomplete `index` validation (defense-in-depth):** `SearchServlet.buildEndpoint` blocks
  `..`, `/`, `_`-prefix, and `*`, but allows `.`-prefixed names (`.security*`), spaces, and `,`.
  Low impact now (the read-only allowlisted key gates access, and the trailing `/_search`
  neutralizes most `?` tricks). Prefer an index allowlist or the high-level client's encoding
  (as `SuggestionServlet` already does).
- **L3 — No NetworkPolicy:** restrict pod egress to the ES host:443 and ingress to the ingress
  controller, to contain a compromised pod.
- **L4 — Legacy descriptors:** `WEB-INF/sun-web.xml` and `glassfish-web.xml` are dead GlassFish
  cruft (not exploitable) — remove.
- **L5 — Query strings logged on ES error** (`logger.warning("... query [" + queryString + "]")`):
  search terms only, low risk; be aware if logs are shipped externally.

---

## What's already solid

- Runs non-root (`USER 10001` + `runAsNonRoot: true`), `seccompProfile: RuntimeDefault`,
  `capabilities: drop: ALL`, `allowPrivilegeEscalation: false`, CPU/memory limits set.
- Default Tomcat webapps removed with a build-time guard.
- API key delivered via Kubernetes Secret and **not logged**.
- TLS to ES on 443 with default certificate verification (no trust-all).
- GET-only endpoints; POST → 405.
- `aggs` parsed into typed objects (no DSL string injection); `size` capped at 1000,
  suggestion size capped at 15.
- After the round-2 fixes: malformed `aggs` fail closed with a 400, and filter construction
  rethrows rather than silently returning unfiltered results.

---

## Suggested fix order

1. **H1** — ingress rate-limiting + query `timeout`/`terminate_after` + `aggs`/`from` caps
   (biggest real risk on a public endpoint).
2. **H2** — stop leaking ES error bodies.
3. **M1** — enable read-only root FS on both blackbox pods; **M3** — disable SA-token automount.
4. **M4** — re-enable the Trivy scan; **M2** — confirm index is public-only / add a field allowlist.
5. **M5 / L-series** — CORS tightening, static-content removal, NetworkPolicy, cruft cleanup.