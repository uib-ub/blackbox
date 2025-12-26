package no.uib.marcus.common.util;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;

import java.util.logging.Level;
import no.uib.marcus.common.Params;
import no.uib.marcus.range.DateRange;

import java.util.logging.Logger;

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.*;

/**
 * Utility class for building Elasticsearch filters
 *
 * @author Hemed Ali Al Ruwehy
 * Last modified: 19-08-2017
 *
 * @author Øyvind Gjesdal
 */

public final class FilterUtils {
    private static final Logger logger = Logger.getLogger(FilterUtils.class.getName());
    private static final String TOP_FILTER = "top_filter";
    private static final String POST_FILTER = "post_filter";
    private static final char FILTER_KEY_VALUE_SEPARATOR = '#';

    //Prevent this class from being initialized
    private FilterUtils() {
    }

    /**
     * A wrapper for building filter without dates
     ***/
    public static Map<String, BoolQuery.Builder> buildBoolFilter(@NotNull Map<String, List<String>> filterMap, String aggs) {
        return buildBoolFilter(filterMap, aggs, null);
    }

    /**
     * Gets the top filter from the given filter map or empty filter if it does not exist
     */
    public static BoolQuery.Builder getTopFilter(@NotNull Map<String, List<String>> selectedFacets, String aggs, DateRange dateRange) {

        Map<String, BoolQuery.Builder> filter = FilterUtils.buildBoolFilter(selectedFacets, aggs, dateRange);
        return filter.getOrDefault(TOP_FILTER, QueryBuilders.bool());
    }

    /**
     * Gets post-filter from the given filter map or empty filter if it does not exist
     */
    public static BoolQuery.Builder getPostFilter(@NotNull Map<String, List<String>> selectedFacets, String aggs) {
        Map<String, BoolQuery.Builder> filter = FilterUtils.buildBoolFilter(selectedFacets, aggs);
        return filter.getOrDefault(POST_FILTER, QueryBuilders.bool());
    }

    /**
     * A method for building BoolFilter based on the aggregation settings.
     *
     * @param filterMap    a map of selected facets with keys as fields and values as terms.
     * @param aggregations aggregations
     * @param dateRange    a date range to be applied to a range filter
     * @return a map which contains AND and OR bool filters based on the aggregations, with the keys
     * "top_filter" and "post_filter" respectively
     */
    private static Map<String, BoolQuery.Builder> buildBoolFilter(
            @NotNull Map<String, List<String>> filterMap,
            @Nullable String aggregations,
            DateRange dateRange
    ) {
        Map<String, BoolQuery.Builder> boolFilterMap = new HashMap<>();
        BoolQuery.Builder topFilter = QueryBuilders.bool();
        BoolQuery.Builder postFilter = QueryBuilders.bool();

        try {
            //Building a filter based on the user-selected facets. Starting with AND filter
            for (Map.Entry<String, List<String>> entry : filterMap.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    TermsQueryField entryTerms = new TermsQueryField.Builder()
                            .value(entry.getValue().stream().map(FieldValue::of).toList())
                        .build();

                    if (hasOROperator(aggregations, entry.getKey())) {
                        //Building "OR" filter that will be used as post_filter.
                        //post_filter only affects search results but NOT aggregations.
                        postFilter.must(QueryBuilders.terms().field(entry.getKey()).terms(entryTerms).build()._toQuery());
                    } else if (entry.getKey().startsWith(BlackboxUtils.MINUS)) {
                        //Exclude any filter that begins with the minus sign
                        topFilter.mustNot(QueryBuilders.terms().field(entry.getKey().substring(1)).terms(entryTerms).build()._toQuery());
                    } else { //default is "AND" filter that will be used as top_filter
                        //Building "AND" filter - add one must clause per term
                        for (FieldValue fv : entryTerms.value()) {
                            topFilter.must(QueryBuilders.term().field(entry.getKey()).value(fv).build()._toQuery());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE,"Exception occurred when constructing bool_filter", ex);
        }

        if (Objects.nonNull(dateRange)) { //append date range filter on top_filter
            addDateRangeFilter(topFilter, dateRange);
        }
        boolFilterMap.put(TOP_FILTER, topFilter);
        boolFilterMap.put(POST_FILTER, postFilter);

        return boolFilterMap;
    }


    /**
     * Appends date range filter to a bool_filter based on the University Library's logic for a date range.
     * <p>
     * Logic: Given a date range, fromDate to toDate, find any resource in which this range, or it's subset
     * lies within it.
     * In other words, any resources that fulfill the following conditions:
     * <p>
     * from_date|----------|to_date
     * from_date|-----------------|to_date        from_date|------------------|to_date
     * from_date|----------------------------------------------------------------|to_date
     * from_date|--------------------------------|to_date
     * from_date|-------|to_date    from_date|---|to_date
     * <p>
     * madeAfter|================================|madeBefore (resource)
     * <p>
     * Note that, our assumption is {@code madeAfter <= madeBefore}, and this algorithm will provide
     * an unexpected result if that is not the case. Checking the integrity of these fields maybe be done using
     * Elasticsearch Script Filters, but it is a heavy process, and we would have to expose Elasticsearch cluster
     * to script injection in which we don't want to do it.
     * Therefore, if it happens, we are leaving the blame to the data, and we are assuming the condition always holds.
     *
     * @param dateRange date range object that will be appended to range filters.
     * @return a bool_filter with date ranges appended
     */

    public static BoolQuery.Builder addDateRangeFilter(BoolQuery.Builder boolFilter, DateRange dateRange) {
        if (boolFilter == null) {
            throw new NullPointerException("Cannot append date ranges to a null bool_filter");
        }

        List<Query> queries = new ArrayList<>();

        if (Objects.nonNull(dateRange)) {
            //Get the lower boundary for this range
            LocalDate fromDate = dateRange.getFrom();

            //Get the upper boundary for this range
            LocalDate toDate = dateRange.getTo();
            if (Objects.nonNull(fromDate) || Objects.nonNull(toDate)) {

              logger.log(Level.FINE, "Adding date-range from {0} ",  fromDate);
              logger.log(Level.FINE,"Adding date-range to {0}",toDate);

                //Range within the "created" field
                var createdRange = new DateRangeQuery.Builder().field(Params.DateField.CREATED);
                if (fromDate != null) {
                    createdRange.gte(fromDate.toString());
                    logger.log(Level.FINE, "Adding date-range from: {0} to created field", fromDate);
                }
                if (toDate != null ) {
                    createdRange.lte(toDate.toString());
                    logger.log(Level.FINE, "Adding date-range to: {0} to created field", toDate);
                }
                logger.fine("adding createdRange query");

                queries.add(createdRange.build()._toRangeQuery()._toQuery());


                /*
                  Here, the condition in which madeAfter >= from_date and madeAfter <= to_date is taken care of, and
                  we don't care about madeBefore. But our assumption is always madeBefore >= madeAfter
                  See the drawing below:-

                     from_date-----------------to_date
                     from_date---------------------------------------------------------------------to_date
                                   from_date---------------------------------to_date
                                   from_date-----to_date
                                   madeAfter================================madeBefore
                */
                var range = new DateRangeQuery.Builder().field(Params.DateField.MADE_AFTER);
                if (fromDate != null) {
                    range.gte(fromDate.toString());
                }
                if (toDate != null) {
                    range.lte(toDate.toString());
                }
                queries.add(range.build()._toRangeQuery()._toQuery());
                /*
                 Here the condition in which madeBefore >= from_date and madeBefore <= to_date, we don't care
                 about madeAfter.
                                                                  from_date-----------------to_date
                 from_date------------------------------------------------------------------------to_date
                                                            from_date|------|to_date
                                   madeAfter================================|madeBefore
                */
                var rangeIgnoreMadeAfter = new DateRangeQuery.Builder().field(Params.DateField.MADE_BEFORE);
                if (fromDate != null) {
                    rangeIgnoreMadeAfter.gte(fromDate.toString());
                }
                if (toDate != null) {
                    rangeIgnoreMadeAfter.lte(toDate.toString());
                }

                if (fromDate != null) {
                    queries.add(rangeIgnoreMadeAfter.build()._toRangeQuery()._toQuery());
                }
                /*
                  This is a case that fromDate and toDate are within the madeAfter and madeBefore range.
                  Precondition:  both fromDate and toDate must have values AND fromDate <= toDate.
                  In other words, we must have the positive range for this condition to satisfy.

                      from_date|---------------------------------------------------|to_date
                                  from_date|-----------------------|to_date
                     made_after|---------------------------------------------------|made_before
                 */
                if (dateRange.isPositive() && fromDate != null && toDate != null) {
                    var rangeMadeBefore = new DateRangeQuery.Builder().field(Params.DateField.MADE_BEFORE);
                    rangeMadeBefore.gte(fromDate.toString());
                    var rangeMadeAfter = new DateRangeQuery.Builder().field(Params.DateField.MADE_AFTER);
                    rangeMadeAfter.lte(toDate.toString());

                    Query madeBeforeAfter = QueryBuilders.bool().must(List.of(rangeMadeAfter.build()._toRangeQuery()._toQuery(),
                        rangeMadeBefore.build()._toRangeQuery()._toQuery())).build()._toQuery();
                    // adding both conditions (before/after) to one query
                    queries.add(madeBeforeAfter);
                }
            }
        }
        if (!queries.isEmpty()){
            logger.fine("hasClauses: Adding date-range to bool_filter");
            // the logic is that dateFilters should be AND between each other, while the different queries
            // of dateRanges should be OR between each other.
           // It is enough that one of the dateFilters matches the dateRange.
            boolFilter.must(QueryBuilders.bool().should(queries).build());
        }
        return boolFilter;
    }

    /**
     * Checks if an aggregation key has the OR operator
     */
    private static boolean hasOROperator(String aggregations, String key) {
        return AggregationUtils.contains(aggregations, key, "operator", "OR");
    }

    /**
     * A method to build a map based on the selected filters. If no filter is
     * selected, return an empty map.
     *
     * @param selectedFilters a string of selected filters in the form of "field#value"
     * @return a filter map in the form of
     * e.g {"subject.exact" = ["Flyfoto" , "Birkeland"], "type.exact" = ["Brev"]}
     */
    @NotNull
    public static Map<String, List<String>> buildFilterMap(@Nullable String[] selectedFilters) {
        Map<String, List<String>> filters = new HashMap<>();
        try {
            if (selectedFilters == null || selectedFilters.length == 0) {
                return Collections.emptyMap();
            }
            for (String entry : selectedFilters) {
                Objects.requireNonNull(entry);
                if (entry.lastIndexOf(FILTER_KEY_VALUE_SEPARATOR) != -1) {
                    //Get the index for the last occurrence of a separator
                    int lastIndex = entry.lastIndexOf(FILTER_KEY_VALUE_SEPARATOR);
                    String key = entry.substring(0, lastIndex).trim();
                    String value = entry.substring(lastIndex + 1).trim();
                  filters.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
                }
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception occurred while constructing a map from selected filters: " + ex);
        }
      if (logger.isLoggable(Level.FINE)) {
        logger.log(Level.FINE, "returning filters {0} ", filters);
      }
        return filters;
    }
}
