# Blackbox — context for Claude

## What it is

Blackbox is a Java web application (WAR) that acts as a query-building proxy between search frontends and Elasticsearch. Clients send simple HTTP GET requests; Blackbox translates them into Elasticsearch Query DSL and returns the raw ES response. It exists primarily to hide ES from the public internet and to centralize query logic (boosting, signature detection, facet routing) that would otherwise be duplicated across multiple frontends.

Deployed at `jambo.uib.no/blackbox` (production) and on a test instance at `bgo1.test.rail.uib.no`. Frontends include Marcus (`marcus.uib.no`), Skeivt Arkiv (`ska-katalog`), WAB (Wittgenstein Archives), and Naturen.

## Tech stack

- **Java 21**, compiled and packaged with Maven as a WAR
- **Tomcat 11** (Jakarta EE 11 / Servlet 6.1)
- **Elasticsearch 9.x** via the official `co.elastic.clients:elasticsearch-java` high-level client and `org.elasticsearch.client:elasticsearch-rest-client` low-level REST client (both used together — see Architecture notes)
- **Jackson** (`jackson-databind`) for JSON parsing; replaced Gson from ES 1.7 era
- `java.util.logging` throughout (replaces the old Log4j dependency); configured via `src/main/resources/logging.properties`
- Docker image: `maven:3-eclipse-temurin-21` builder → `tomcat:11-jre21` runtime; CI via `.gitlab-ci.yml`

## Endpoints

| URL pattern | Servlet | Purpose |
|---|---|---|
| `/search` | `SearchServlet` | Main search — GET only, returns raw ES JSON response |
| `/suggest` | `SuggestionServlet` | Completion suggestions — GET only, returns JSON array of strings |

POST returns 405 JSON on both. CORS is open (`*`) via `web.xml`.

## Request parameters (`/search`)

| Parameter | Description |
|---|---|
| `q` | Query string |
| `index` | One or more ES index names (repeatable) |
| `service` | Selects search builder: `MARCUS` (default), `WAB`, `NATUREN` (see Services) |
| `from` | Offset, default 0 |
| `size` | Page size, default 10, max 1000 |
| `from_date` / `to_date` | Date range in `yyyy`, `yyyy-MM`, or `yyyy-MM-dd` format |
| `filter` | Facet filter as `field#value` (repeatable). Prefix field with `-` to exclude: `-type.exact#Fotografi` |
| `aggs` | Aggregations as a JSON array (see Aggregations) |
| `sort` | Sort as `field:asc` or `field:desc`; `_score:desc` for relevance |
| `pretty` | `true` to pretty-print response |
| `index_boost` | Index name to boost (factor 5×) |

## Services and search builders

`SearchBuilderFactory.getSearchBuilder(service, client)` maps the `service` param to a builder:

```
MARCUS / MARCUS_ADMIN → MarcusSearchBuilder   (default when no service param is given)
WAB                   → WabSearchBuilder
NATUREN               → NaturenSearchBuilder  (extends MarcusSearchBuilder)
```

`WAB` is known to be actively passed by its frontend. Whether `NATUREN` is still passed by its frontend is not confirmed. A former `SKA` service was removed during the ES upgrade: the Skeivt Arkiv frontend never passed `service=SKA`, so its requests already fell through to the default `MarcusSearchBuilder`.

### MarcusSearchBuilder
Default service. Uses `query_string` query with AND operator across `identifier`, `label`, `all`, `all.exact`. Boosts `type=fotografi` documents (weight 3×). On empty query, applies a random `function_score` boost from a curated picture list. Appends wildcard to UBB signatures (`ubb-*`, `ubm-*`, `sab-*`) and to tokens containing a hyphen.

### WabSearchBuilder
For Wittgenstein Archives. Uses `simple_query_string` with AND operator across `label`, `publishedIn`, `publishedInPart`, `all`. Appends trailing wildcard to WAB signatures (`ms-*`, `ts-*`).

### NaturenSearchBuilder
Extends `MarcusSearchBuilder`. Calls `super.constructSearchRequest()` for infrastructure (indices, pagination, aggregations, sort), then overrides the query to add text highlighting on `textContent`. Boosts issues that have a thumbnail on empty queries.

## Aggregations

The `aggs` parameter is a JSON array of aggregation config objects:

```json
[
  {"field": "type.exact", "size": 40, "operator": "OR",  "order": "count_desc", "min_doc_count": 0},
  {"field": "maker.exact", "size": 10, "operator": "AND", "order": "count_desc", "min_doc_count": 1}
]
```

Supported fields per aggregation object: `field`, `size`, `operator` (`AND`/`OR`), `order` (`count_asc`, `count_desc`, `term_asc`, `term_desc`), `min_doc_count`, `type` (`date_histogram`).

The `operator` field drives filter routing (see Filter routing below).

`AggregationUtils.addAggregations()` builds the aggregation map. For OR-type aggregations where the user has an active selection, a sub-aggregation filter (`aggs_filter`) is added to that bucket to show accurate counts excluding its own field's selection.

## Filter routing — AND vs OR

This is the core logic in `FilterUtils.buildBoolFilter()`:

- **`operator: OR`** → selection goes into `post_filter` (filters search hits, does not affect aggregation counts)
- **`operator: AND`** (default) → selection goes into `topFilter` (wraps the query in a bool filter, affects both hits and aggregation counts)
- **`-field#value`** (minus prefix) → exclusion, goes into `topFilter` as a `mustNot`
- **`from_date` / `to_date`** → always goes into `topFilter` as a date range

Multiple OR selections on the **same field** are combined as a `terms` query (OR within field). OR selections on **different fields** are separate `must` clauses in `post_filter` (AND across fields). This gives the standard faceted search behaviour: multi-select within a facet is additive, selections across facets are intersecting.

## Elasticsearch client pattern

Requests are built using the type-safe high-level `ElasticsearchClient`, then serialized to JSON via `JacksonJsonpGenerator`, then re-executed via the low-level `RestClient.performRequest()`. This avoids a double JSON round-trip and ensures aggregation keys are serialized without ES type-name prefixes (e.g. `sterms#field` → `field`).

The high-level client is also used directly for completion suggestions (`SuggestionServlet`).

## Configuration

Connection settings are loaded once at startup (double-checked locking singleton in `ElasticsearchClientFactory`). **File-based configuration is intended to be removed** — environment variables are the only supported configuration path going forward.

**Env-var mode**: set `ELASTICSEARCH_CLUSTER_NAME`; all five vars are then required:
```
ELASTICSEARCH_CLUSTER_NAME
ELASTICSEARCH_CLUSTER_NODE_NAME
ELASTICSEARCH_CLUSTER_HOST
ELASTICSEARCH_CLUSTER_PORT          (REST port, typically 9200 or 443)
ELASTICSEARCH_CLUSTER_API_KEY
```

**File mode** (legacy, to be removed): if `ELASTICSEARCH_CLUSTER_NAME` is not set, reads `src/main/resources/config.template.json`. The example file `config.template.example.json` can be used as a starting point. Note that this file predates the API key requirement and is missing the `api_key` field.

`ApplicationShutdownListener` closes the client cleanly on undeploy/shutdown.

## Build and run

```bash
mvn clean install          # builds target/blackbox-9.0-SNAPSHOT.war
# or via Docker:
docker build -f docker/Dockerfile -t blackbox .
```

The Dockerfile sets `relaxedQueryChars='"{}[]'` on the Tomcat connector so ES JSON in query strings is accepted without percent-encoding.

## Key source files

```
src/main/java/no/uib/marcus/
  client/
    ElasticsearchClientFactory.java   — singleton ES client, env/file config
  common/
    Params.java                       — all request parameter name constants
    ServiceName.java                  — enum for service routing
    loader/JsonFileLoader.java        — reads config.template.json (legacy)
    util/
      AggregationUtils.java           — builds ES aggregation map from aggs JSON
      FilterUtils.java                — AND/OR filter routing, date range logic
      QueryUtils.java                 — query_string / simple_query_string builders
      SignatureUtils.java             — UBB/WAB signature wildcard logic
      SortUtils.java                  — sort string → SortOptions
      StringUtils.java                — hasText, parseIntWithDefault, etc.
  range/
    DateRange.java                    — parses from_date/to_date, partial dates ok
  search/
    SearchBuilder.java                — interface
    AbstractSearchBuilder.java        — shared state and setters
    MarcusSearchBuilder.java          — default query builder
    WabSearchBuilder.java
    NaturenSearchBuilder.java
    SearchBuilderFactory.java         — factory
  servlet/
    SearchServlet.java                — /search endpoint
    SuggestionServlet.java            — /suggest endpoint
    ApplicationShutdownListener.java  — closes ES client on shutdown
src/main/resources/
  config.template.json                — local connection config (not committed with real credentials)
  config.template.example.json       — safe example to copy from
  logging.properties                  — JUL config; application logs at INFO by default
```
