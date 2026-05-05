/# Code Review — blackbox (ES 1.7 → 9.x migration)

## Resolved on `blackbox-elasticsearch-upgrade`

- **Naturen filter crash** (not in original review) — `NaturenSearchBuilder.constructSearchRequest()` triggered `IllegalStateException: Object builders can only be used once` because the parent (`MarcusSearchBuilder`) consumed the `BoolQuery.Builder` from `getFilter()`, then Naturen built it again. Fixed by changing `AbstractSearchBuilder` to store filter/postFilter as built `BoolQuery` instead of `BoolQuery.Builder` (immutable, reusable). Servlet pre-builds before passing to the builder.
- **Items 1, 10, 12, 16** — `SkaSearchBuilder`, `ServiceName.SKA`, and `SearchBuilderFactory.skaSearch()` deleted. Confirmed unreachable: both legacy and new SKA frontends omit the `service=` parameter (verified with live URL on `ska-katalog.bgo1.test.rail.uib.no`), so requests fall through to `MarcusSearchBuilder`.

---

## Bug — `SkaSearchBuilder` filter is silently ignored

**Note: `SkaSearchBuilder` appears to be dormant in production.** The SKA frontend no longer passes `service=ska`, so requests fall through to the default `MarcusSearchBuilder` which handles filters correctly. Verified against live URLs — no `service=` parameter is present. The bug is still latent code and should be fixed or the class removed.

`SkaSearchBuilder.java:58-61` — When a filter is set, the TODO comment blocks its application and the `else` branch (which sets the query) is also skipped. If `SkaSearchBuilder` were ever invoked with AND-type facets active, it would return unfiltered results:

```java
if (getFilter() != null) {
   //@todo searchRequest.query(...); // ← nothing happens here
} else {
    searchRequest.query(query); // ← also skipped when filter exists
}
```

The fix mirrors how `WabSearchBuilder` and `NaturenSearchBuilder` handle this — wrap query + filter in a bool:

```java
if (getFilter() != null) {
    BoolQuery filterQuery = getFilter().build();
    searchRequest.query(QueryBuilders.bool().must(query)
        .filter(List.of(filterQuery._toQuery())).build()._toQuery());
} else {
    searchRequest.query(query);
}
```

---

## Bug — `MarcusSearchBuilder` random-picture boost is overridden

`MarcusSearchBuilder.java:103-106` — In the no-query branch, `functionScoreQueryBuilder` is built with `[fotoFs, randomFs]`. But line 106 then calls `.functions(List.of(fotoFs))` on the same builder, replacing the function list with only `fotoFs`. The random picture boost is never applied.

```java
// no-query branch sets both functions:
functionScoreQueryBuilder = QueryBuilders.functionScore()...functions(List.of(fotoFs, randomBoost));
// ...then immediately overwrites with only fotoFs:
query = functionScoreQueryBuilder.functions(List.of(fotoFs)).build()._toQuery();
```

For the with-query branch, `fotoFs` should be the only function score, so line 106 is correct there. The fix is to apply `fotoFs` inside each branch rather than unconditionally after:

```java
if (StringUtils.hasText(getQueryString())) {
    query = QueryBuilders.functionScore()
        .query(QueryUtils.buildMarcusQueryString(getQueryString()).build()._toQuery())
        .functions(List.of(fotoFs)).build()._toQuery();
} else {
    String randomQueryString = randomPictures[ThreadLocalRandom.current().nextInt(randomPictures.length)];
    query = QueryBuilders.functionScore()
        .query(QueryBuilders.matchAll().build()._toQuery())
        .functions(List.of(fotoFs, new FunctionScore.Builder()
            .filter(QueryBuilders.simpleQueryString().query(randomQueryString).build()._toQuery())
            .weight(2.0).build()))
        .build()._toQuery();
}
```

---

## Critical — `config.template.json` missing `api_key` — startup crash for file-based config

`ElasticsearchClientFactory.java:50-51` — `createElasticsearchClient` always reads `api_key` from the properties map:

```java
new BasicHeader("Authorization", "ApiKey " + BlackboxUtils.getValueAsString(properties, "api_key"))
```

`getValueAsString` throws `NullPointerException` if the key is absent. The file-based config (`config.template.json`, `config.template.example.json`) has neither `api_key` nor the correct REST port (they still show port `9300`, the old transport port; the REST port is `9200` or `443`). Any deployment not using the `ELASTICSEARCH_CLUSTER_NAME` env var will crash at startup.

Add `api_key` to the config template and update the port:

```json
{
  "cluster": {
    "name": "...",
    "host": "...",
    "port": "9200",
    "api_key": "your-api-key-here"
  }
}
```

---

## Critical — `fixedInterval` used for calendar-type intervals in date histogram aggregations

`AggregationUtils.java:224` — ES 8+ strictly separates fixed intervals (`1d`, `7d`, expressed in time units) from calendar intervals (`year`, `month`, `quarter`, `week`). The old `DateHistogram.Interval` handled both. The new code always calls `fixedInterval`:

```java
dateHistBuilder.fixedInterval(new Time.Builder().time(facet.path("interval").asText()).build());
```

If any aggregation config passes `"interval": "year"` or `"interval": "month"`, Elasticsearch rejects the request with a parse error. If calendar intervals are in use, they need to be routed to `calendarInterval` instead:

```java
if (facet.has("interval")) {
    String interval = facet.path("interval").asText();
    try {
        dateHistBuilder.calendarInterval(CalendarInterval._DESERIALIZER.parse(interval));
    } catch (Exception e) {
        dateHistBuilder.fixedInterval(new Time.Builder().time(interval).build());
    }
}
```

---

## Medium — No error JSON body on exceptions in `SearchServlet`

`SearchServlet.java:161-171` — Both catch blocks re-throw the exception, which means Tomcat produces an HTML error page while `Content-Type` is already set to `application/json`. Callers expecting JSON get an HTML response on errors. Set a 500 status and write a JSON error body before re-throwing (or instead of re-throwing), as already done in `doPost`.

---

## Medium — `closeClient()` closes the transport twice

`ElasticsearchClientFactory.java:124-128`:

```java
elasticsearchClient._transport().close();  // closes transport
elasticsearchClient.close();               // also closes transport internally
```

`ElasticsearchClient.close()` already closes the underlying transport. The explicit `_transport().close()` is redundant and may log an error or throw on the second close. Remove the first line.

---

## Medium — `FilterUtils.getPostFilter(@NotNull)` called with null selectedFacets

`AggregationUtils.java:147` — The single-argument overload of `addAggregations` delegates with `null`:

```java
return addAggregations(searchRequest, aggregations, null);
```

The three-argument overload immediately calls `FilterUtils.getPostFilter(selectedFacets, aggregations)`, which is annotated `@NotNull` but receives `null`. Inside `buildBoolFilter`, iterating `filterMap.entrySet()` throws `NullPointerException`.

Safe in the Servlet path (always supplies an empty map), but will crash any test or library consumer calling `addAggregations` without selected facets. Add a null guard:

```java
BoolQuery.Builder aggsFilter = FilterUtils.getPostFilter(
    selectedFacets != null ? selectedFacets : Collections.emptyMap(), aggregations);
```

---

## Medium — OR-aggregation sub-filter not applied to unselected facets

`AggregationUtils.java:161-183` — The old code applied sub-aggregation filters to all OR-type facets (both those with active selections and those without), so that facet counts for unselected values were accurate when sibling OR facets had active selections. The new code only adds sub-filters when `selectedFacets.containsKey(facetField)`, leaving unselected OR facets without a sub-filter. If you use multi-select OR facets, the counts for unselected values in those facets will not account for selections made in sibling OR facets.

---

## Minor — `WabSearchBuilder:22` — parsing a string literal as int

```java
private final TrackHits trackHits = new TrackHits.Builder().count(Integer.parseInt("100000")).build();
```

Should be `.count(100000)`.

---

## Minor — `SkaSearchBuilder` — empty `finally` block

`SkaSearchBuilder.java:87-89` — The empty `finally {}` serves no purpose and should be removed.

---

## Minor — `AbstractSearchBuilder.toString():333` — NPE if indices are null

```java
getIndices().length == 0  // throws NullPointerException if getIndices() returns null
```

`getIndices()` can return null (field defaults to null, `setIndices` may never be called). Should be:

```java
getIndices() == null || getIndices().length == 0 ? "" : Arrays.toString(getIndices())
```

---

## Minor — `SkaSearchBuilder.BoostType.INTERVJU` is defined but never used

`SkaSearchBuilder.java:95` — `INTERVJU = "intervju"` is declared but the boost logic at line 54 calls `weight(2.0)` with no filter referencing it, boosting all documents equally (a no-op scaling). Either wire it into the boost function as a filter (matching `NaturenSearchBuilder`'s pattern with `BoostType.ISSUE`), or remove the constant.

---

## Minor — `CompletionSuggestion.main()` in production code

`CompletionSuggestion.java:93-99` — A `public static void main()` method in a production class is a leftover debug tool. It would fail at runtime if called (no live client configured) and clutters the public API. Move any manual testing to a proper test class.

---

## Minor — Unused imports in `ElasticsearchClientFactory`

`ElasticsearchClientFactory.java:22-23` — `java.net.InetAddress` and `java.net.UnknownHostException` are imported but unused. `HttpHost` does not throw `UnknownHostException`; the catch block is dead. Remove both imports and the catch block.

---

## Minor — Obsolete `-Djava.endorsed.dirs` compiler arg

`pom.xml:122` — The endorsed standards mechanism was removed in Java 9 and is a no-op in Java 21. Remove the `<compilerArgs>` block and the `maven-dependency-plugin` execution that copies the jakartaee jar to the endorsed directory:

```xml
<!-- remove these -->
<compilerArgs>
    <arg>-Djava.endorsed.dirs=${endorsed.dir}</arg>
</compilerArgs>
```

---

## Cleanup — Dangling WIP comment in `SearchServlet`

`SearchServlet.java:152-153`:

```java
// Are there things added other than indicies lost between the translation of low level
// and the new rest client?
```

This is a debugging question, not documentation. Remove it. (The hybrid client approach — build via high-level client, execute via low-level REST client — is intentional and worth a proper explanation comment instead.)

---

## Cleanup — Missing newline at end of `docker/Dockerfile`

The diff shows `\ No newline at end of file`. Add a trailing newline.

---

## Cleanup — Dead code candidates

- `QueryUtils.toJsonString()` — builds a JSON string from a `SearchResponse`, but `SearchServlet` now serializes via the low-level REST response directly. Verify nothing calls this; if confirmed unused, remove it.
- `BlackboxUtils.jsonify()` — converts single-element arrays in a map; nothing in the current codebase appears to call it.

---

## Cleanup — `WabSearchBuilder` noisy debug logging

`WabSearchBuilder.java:70-72` — These lines compare filter sizes and equality for no apparent purpose:

```java
logger.fine("compare if filterQuery list is the same as filter() method" + ...);
logger.fine("sizes: " + filterQuery.filter().size() + ...);
```

Debugging artifacts. Remove them or consolidate into a single meaningful log line.

---

## Design note — `NaturenSearchBuilder` calls `super.constructSearchRequest()` redundantly

`NaturenSearchBuilder.java:35` calls `super.constructSearchRequest()`, which fully sets the query (with filter logic). The child then immediately re-evaluates `getFilter()` and calls `searchRequest.query(...)` again, overriding the parent's work. The parent's query setup is wasted. This works correctly because the child's assignment wins, but it's fragile — a maintainer could easily miss the override. Consider having the parent handle only infrastructure (indices, pagination, aggregations, sort) and leave query-building to each concrete class.

---

## Cleanup — `SkaSearchBuilder` / `ServiceName.SKA` appear to be unused

Verified against both test (`bgo1.test.rail.uib.no`) and production (`jambo.uib.no`) URLs: neither passes a `service=` parameter. Without it, `ServiceName.toEnum()` returns the default `MARCUS`, so `SkaSearchBuilder` is never instantiated. `ServiceName.SKA` and the related factory method `SearchBuilderFactory.skaSearch()` are also unreachable.

Options:
- **Remove** `SkaSearchBuilder`, `ServiceName.SKA`, and `SearchBuilderFactory.skaSearch()` if SKA is permanently handled by `MarcusSearchBuilder`.
- **Fix and keep** if there is any intention to use a dedicated SKA service in future (apply the filter fix from item #1 above first).

---

## Summary

| # | Severity | Location | Issue |
|---|----------|----------|-------|
| 1 | **Bug** | `SkaSearchBuilder:58` | Filter never applied — but class is currently unreachable (see cleanup item below) |
| 2 | **Bug** | `MarcusSearchBuilder:106` | Random picture boost unconditionally overridden |
| 3 | **Critical** | `config.template.json` | Missing `api_key` — NPE crash at startup for file-based config |
| 4 | **Critical** | `AggregationUtils:224` | `fixedInterval` used for calendar intervals (year/month/etc. break) |
| 5 | Medium | `SearchServlet:161` | Exceptions re-thrown without JSON error body (Tomcat returns HTML) |
| 6 | Medium | `ElasticsearchClientFactory:124` | `_transport().close()` before `client.close()` double-closes transport |
| 7 | Medium | `AggregationUtils:147` | `getPostFilter(@NotNull)` called with null selectedFacets |
| 8 | Medium | `AggregationUtils:161` | OR-aggregation sub-filter not applied to unselected facets |
| 9 | Minor | `WabSearchBuilder:22` | `Integer.parseInt("100000")` should be `100000` |
| 10 | Minor | `SkaSearchBuilder:87` | Empty `finally` block |
| 11 | Minor | `AbstractSearchBuilder:333` | NPE if `getIndices()` is null |
| 12 | Minor | `SkaSearchBuilder:95` | `INTERVJU` constant unused, boost function has no filter |
| 13 | Minor | `CompletionSuggestion:93` | Debug `main()` in production class |
| 14 | Minor | `ElasticsearchClientFactory:22` | Unused imports (`InetAddress`, `UnknownHostException`) |
| 15 | Minor | `pom.xml:122` | Obsolete `-Djava.endorsed.dirs` compiler arg |
| 16 | Cleanup | `SkaSearchBuilder`, `ServiceName.SKA` | Entire SKA service path appears unused — remove or fix |
| 17 | Cleanup | `SearchServlet:152` | Dangling WIP comment |
| 18 | Cleanup | `docker/Dockerfile:10` | Missing newline at end of file |
| 19 | Cleanup | `QueryUtils`, `BlackboxUtils` | `toJsonString()` and `jsonify()` are likely dead methods |
| 20 | Cleanup | `WabSearchBuilder:70` | Debug log artifacts |