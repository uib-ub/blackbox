package no.uib.marcus.common.util;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.TermsQueryField;
import co.elastic.clients.json.JsonData;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.uib.marcus.common.Params;
import no.uib.marcus.range.DateRange;
import java.util.logging.Logger;

import java.time.LocalDate;;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Utility class for building Elasticsearch filters
 *
 * @author Hemed Ali Al Ruwehy
 * Last modified: 19-08-2017
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
     * Gets top filter from the given filter map or empty filter if it does not exist
     */
    public static BoolQuery.Builder getTopFilter(@NotNull Map<String, List<String>> selectedFacets, String aggs, DateRange dateRange) {
        Map<String, BoolQuery.Builder> filter = FilterUtils.buildBoolFilter(selectedFacets, aggs, dateRange);
        return filter.getOrDefault(TOP_FILTER, QueryBuilders.bool());
    }

    /**
     * Gets post filter from the given filter map or empty filter if it does not exist
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
            //Building a filter based on the user selected facets. Starting with AND filter
            for (Map.Entry<String, List<String>> entry : filterMap.entrySet()) {
                if (entry.getValue() != null && entry.getValue().size() > 0) {
                    TermsQueryField entryTerms = new TermsQueryField.Builder()
                            .value(entry.getValue().stream().map(FieldValue::of).toList())
                            .build();

                    if (hasOROperator(aggregations, entry.getKey())) {
                        //Building "OR" filter that will be used as post_filter.
                        //post_filter only affects search results but NOT aggregations.
                        postFilter.must(QueryBuilders.terms().field(entry.getKey()).terms(entryTerms).build()._toQuery());
                    } else if (entry.getKey().startsWith(BlackboxUtils.MINUS)) {
                        //Exclude any filter that begins with minus sign
                        topFilter.mustNot(QueryBuilders.terms().field(entry.getKey().substring(1)).terms(entryTerms).build()._toQuery());
                    } else { //default is "AND" filter that will be used as top_filter
                        for (Object value : entry.getValue()) {
                            if (entry.getKey().startsWith(BlackboxUtils.MINUS)) {
                                //Exclude any filter that begins with minus sign
                                topFilter.mustNot(QueryBuilders.terms().field(entry.getKey().substring(1)).terms(entryTerms).build()._toQuery());
                            } else {//Building "AND" filter
                                topFilter.must(QueryBuilders.terms().field(entry.getKey()).terms(entryTerms).build()._toQuery());}
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.severe("Exception occurred when constructing bool_filter [ " + ex + " ]");
            throw ex;
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
     * Logic: Given a date range, fromDate to toDate, find any resource in which this range or it's subset
     * lies within it.
     * In other words, any resource that fulfill the following conditions:-
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
     * unexpected result if that is not the case. Checking the integrity of these fields maybe be done using
     * Elasticsearch Script Filters but it is a heavy process and we would have to expose Elasticsearch cluster
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

        if (Objects.nonNull(dateRange)) {
            //Get lower boundary for this range
            LocalDate fromDate = dateRange.getFrom();

            //Get upper boundary for this range
            LocalDate toDate = dateRange.getTo();

            if (Objects.nonNull(fromDate) || Objects.nonNull(toDate)) {

                //Range within "created" field
                boolFilter.should(QueryBuilders.range().field(Params.DateField.CREATED).gte(JsonData.of(fromDate)).lte(JsonData.of(toDate)).build()._toQuery());


                /*
                  Here, the condition in which madeAfter >= from_date and madeAfter <= to_date is taken care and
                  we don't care about madeBefore. But our assumptions is always madeBefore >= madeAfter
                  See drawing below:-

                     from_date-----------------to_date
                     from_date---------------------------------------------------------------------to_date
                                   from_date---------------------------------to_date
                                   from_date-----to_date
                                   madeAfter================================madeBefore
                */
                boolFilter.should(QueryBuilders.bool()
                        .must(QueryBuilders.range().field(Params.DateField.MADE_AFTER).gte(JsonData.of(fromDate)).lte(JsonData.of(toDate)).build()._toQuery()).build()._toQuery());

               /*
                 Here the condition in which madeBefore >= from_date and madeBefore <= to_date, we don't care
                 about madeAfter.
                                                                  from_date-----------------to_date
                 from_date------------------------------------------------------------------------to_date
                                                            from_date|------|to_date
                                   madeAfter================================|madeBefore
                */
                boolFilter.should(QueryBuilders.bool()
                        .must(QueryBuilders.range().field(Params.DateField.MADE_BEFORE).gte(JsonData.of(fromDate)).lte(JsonData.of(toDate)).build()._toQuery()).build()._toQuery());

                 /*
                  This is a case that fromDate and toDate are within madeAfter and madeBefore range.
                  Precondition:  both fromDate and toDate must have values AND fromDate <= toDate.
                  In other words, we must have positive range for this condition to satisfy.

                      from_date|---------------------------------------------------|to_date
                                  from_date|-----------------------|to_date
                     made_after|---------------------------------------------------|made_before
                 */
                if (dateRange.isPositive()) {
                    boolFilter.should(QueryBuilders.bool()
                            .must(QueryBuilders.
                                    range().field(Params.DateField.MADE_AFTER).lte(JsonData.of(fromDate)).build()._toQuery())
                            .must(QueryBuilders.
                                    range().field(Params.DateField.MADE_BEFORE).gte(JsonData.of(toDate)).build()._toQuery())
                            .build()._toQuery());

                }
            }
        }
        return boolFilter;
    }

    /**
     * Checks if an aggregation key has OR operator
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
                assert entry != null;
                if (entry.lastIndexOf(FILTER_KEY_VALUE_SEPARATOR) != -1) {
                    //Get the index for the last occurrence of a separator
                    int lastIndex = entry.lastIndexOf(FILTER_KEY_VALUE_SEPARATOR);
                    String key = entry.substring(0, lastIndex).trim();
                    String value = entry.substring(lastIndex + 1).trim();
                    if (!filters.containsKey(key)) {
                        filters.put(key, Lists.newArrayList(value));
                    } else {
                        filters.get(key).add(value);
                    }
                }
            }
        } catch (Exception ex) {
            logger.severe("Exception occurred while constructing a map from selected filters: " + ex.getMessage());
        }
        return filters;
    }
}
