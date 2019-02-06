package no.uib.marcus.common.util;

import no.uib.marcus.common.Params;
import no.uib.marcus.range.DateRange;
import org.apache.log4j.Logger;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.joda.time.LocalDate;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for building Elasticsearch filters
 *
 * @author Hemed Ali Al Ruwehy
 * Last modified: 19-08-2017
 */

public final class FilterUtils {
    private static final Logger logger = Logger.getLogger(FilterUtils.class);

    //Prevent this class from being initialized
    private FilterUtils() {
    }

    /**
     * Append date range filter to a bool_filter based on the University Library's logic for a date range.
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
     * Therefore, if it happens, we are living the blame to the data, and we are assuming the condition always holds.
     *
     * @param dateRange date range object that will be appended to range filters.
     * @return a bool_filter with date ranges appended
     */

    public static BoolFilterBuilder appendDateRangeFilter(BoolFilterBuilder boolFilter, DateRange dateRange) {
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
                boolFilter.should(FilterBuilders.rangeFilter(Params.DateField.CREATED).gte(fromDate).lte(toDate));


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
                boolFilter.should(FilterBuilders.boolFilter()
                        .must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).gte(fromDate).lte(toDate)));
                //.must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).lte(toDate)))


               /*
                 Here the condition in which madeBefore >= from_date and madeBefore <= to_date, we don't care
                 about madeAfter.
                                                                  from_date-----------------to_date
                 from_date------------------------------------------------------------------------to_date
                                                            from_date|------|to_date
                                   madeAfter================================|madeBefore
                */
                boolFilter.should(FilterBuilders.boolFilter()
                        .must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(fromDate).lte(toDate)));
                //.must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).lte(toDate)))


                 /*
                  This is a case that fromDate and toDate are within madeAfter and madeBefore range.
                  Pre condition:  both fromDate and toDate must have values AND fromDate <= toDate.
                  In other words, we must have positive range for this condition to satisfy.

                      from_date|---------------------------------------------------|to_date
                                  from_date|-----------------------|to_date
                     made_after|---------------------------------------------------|made_before
                 */
                if (dateRange.isPositive()) {
                    boolFilter.should(FilterBuilders.boolFilter()
                            .must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).lte(fromDate))
                            .must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(toDate)));

                }
            }
        }
        return boolFilter;
    }


    /**
     * A wrapper for building filter without dates
     ***/
    public static Map<String, BoolFilterBuilder> buildBoolFilter(@NotNull Map<String, List<String>> filterMap, String aggs) {
        return buildBoolFilter(filterMap, aggs, null);
    }

    /**
     * A wrapper method for building BoolFilter based on the aggregation settings.
     */
    public static Map<String, BoolFilterBuilder> buildBoolFilter(HttpServletRequest request) {
        //Get corresponding request parameters
        String fromDate = request.getParameter(Params.FROM_DATE);
        String toDate = request.getParameter(Params.TO_DATE);
        String[] selectedFilters = request.getParameterValues(Params.SELECTED_FILTERS);
        String aggregations = request.getParameter(Params.AGGREGATIONS);

        //Build a filter map based on selected facets. In the result map
        //keys are "fields" and values are "terms"
        //e.g {"subject.exact" = ["Flyfoto" , "Birkeland"], "type" = ["Brev"]}
        Map<String, List<String>> selectedFacets = AggregationUtils.buildFilterMap(selectedFilters);

        return buildBoolFilter(selectedFacets, aggregations, new DateRange(fromDate, toDate));
    }


    /**
     * A method for building BoolFilter based on the aggregation settings.
     *
     * @param filterMap a map of selected facets with keys as fields and values as terms.
     * @param aggs      aggregations
     * @param dateRange a date range to be applied to a range filter
     * @return a map which contains AND and OR bool filters based on the aggregations
     */
    public static Map<String, BoolFilterBuilder> buildBoolFilter(@NotNull Map<String, List<String>> filterMap,
                                                                 @Nullable String aggs,
                                                                 DateRange dateRange) {
        Map<String, BoolFilterBuilder> boolFilterMap = new HashMap<>();
        BoolFilterBuilder topFilter = FilterBuilders.boolFilter();
        BoolFilterBuilder postFilter = FilterBuilders.boolFilter();

        try {
            //Building a filter based on the user selected facets. Starting with AND filter
            for (Map.Entry<String, List<String>> entry : filterMap.entrySet()) {
                if (entry.getValue() != null && entry.getValue().size() > 0) {
                    if (AggregationUtils.contains(aggs, entry.getKey(), "operator", "OR")) {
                        //Building "OR" filter that will be used as post_filter.
                        //post_filter only affects search results but NOT aggregations.
                        postFilter.must(FilterBuilders.termsFilter(entry.getKey(), entry.getValue()));
                    } else if (entry.getKey().startsWith(BlackboxUtils.MINUS)) {
                        //Exclude any filter that begins with minus sign
                        topFilter.mustNot(FilterBuilders.termsFilter(entry.getKey().substring(1), entry.getValue()));
                    } else { //default is "AND" filter that will be used as top_filter
                        for (Object value : entry.getValue()) {
                            if (entry.getKey().startsWith(BlackboxUtils.MINUS)) {
                                //Exclude any filter that begins with minus sign
                                topFilter.mustNot(FilterBuilders.termFilter(entry.getKey().substring(1), value));
                            } else {//Building "AND" filter
                                topFilter.must(FilterBuilders.termFilter(entry.getKey(), value));
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Exception occurred when constructing bool_filter [ " + ex + " ]");
            throw ex;
        }

        if (dateRange != null) { //append date range filter on top_filter
            appendDateRangeFilter(topFilter, dateRange);
        }
        boolFilterMap.put(Params.TOP_FILTER, topFilter);
        boolFilterMap.put(Params.POST_FILTER, postFilter);

        return boolFilterMap;
    }
}
