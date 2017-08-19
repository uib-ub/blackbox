package no.uib.marcus.common.util;

import no.uib.marcus.common.Params;
import no.uib.marcus.common.Settings;
import org.apache.log4j.Logger;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.joda.time.LocalDate;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for building Elasticsearch filters
 * @author Hemed Ali Al Ruwehy
 * Last modified: 19-08-2017
 */

public final class FilterUtils {
    private static final Logger logger = Logger.getLogger(FilterUtils.class);

    //Prevent this class from being initialized
    private FilterUtils() {}

    /**
     * Append date range filter to a bool_filter based on the University Library's logic for a date range.
     *
     * Logic: Given a date range, fromDate to toDate, find any resource in which this range or it's subset
     * lies within it.
     * In other words, any resource that fulfill the following conditions:-
     *
                              from_date|----------|to_date
     from_date|-----------------|to_date        from_date|------------------|to_date
        from_date|----------------------------------------------------------------|to_date
                        from_date|--------------------------------|to_date
                      from_date|-------|to_date    from_date|---|to_date

                       madeAfter|================================|madeBefore (resource)

     Note that, our assumption is {@code madeAfter <= madeBefore}, and this algorithm will provide
     unexpected result if that is not the case. Checking the integrity of these fields maybe be done using
     Elasticsearch Script Filters but it is a heavy process and we would have to expose Elasticsearch cluster
     to script injection in which we don't want to do it.
     Therefore, if it happens, we are living the blame to the data, and we are assuming the condition always holds.

     * @param boolFilter  a filter in which date ranges will be appended to.
     * @param fromDate from_date as a string in the form of YYYY or YYYY-MM or YYYY-MM-DD
     * @param toDate to_date as a string in the form of YYYY or YYYY-MM or YYYY-MM-DD
     *
     * @return a bool_filter with date ranges
     */

    public static BoolFilterBuilder appendDateRangeFilter(BoolFilterBuilder boolFilter, String fromDate, String toDate) {
        if(boolFilter == null){
            throw new NullPointerException("Cannot append date ranges to a null bool_filter");
        }

        if (Strings.hasText(fromDate) || Strings.hasText(toDate)) {
            boolFilter
                    //Range within "available" field
                    //.should(FilterBuilders.rangeFilter(Params.DateField.AVAILABLE).gte(fromDate).lte(toDate))

                    //Range within "created" field
                    .should(FilterBuilders.rangeFilter(Params.DateField.CREATED).gte(fromDate).lte(toDate))

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
                     .should(FilterBuilders.boolFilter()
                            .must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).gte(fromDate).lte(toDate)))
                            //.must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).lte(toDate)))

                   /*
                     Here the condition in which madeBefore >= from_date and madeBefore <= to_date, we don't care
                     about madeAfter.
                                                                      from_date-----------------to_date
                     from_date------------------------------------------------------------------------to_date
                                                                from_date|------|to_date
                                       madeAfter================================|madeBefore
                    */
                    .should(FilterBuilders.boolFilter()
                            .must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(fromDate).lte(toDate)));
                            //.must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).lte(toDate)))

                     /*
                      This is a case that fromDate and toDate are within madeAfter and madeBefore range.
                      Pre condition:  both fromDate and toDate must have values AND fromDate <= toDate

                          from_date|---------------------------------------------------|to_date
                                      from_date|-----------------------|to_date
                         made_after|---------------------------------------------------|made_before
                     */

            if (isValidRange(fromDate, toDate)) {
                boolFilter.should(FilterBuilders.boolFilter()
                        .must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).lte(fromDate))
                        .must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(toDate)));
            }
        }
        return boolFilter;
    }


    /**
     *  Validate date range
     *
     * @param fromDate from date
     * @param toDate to date
     * @return {@code true } if toDate is greater or equal to fromDate, otherwise {@code false}
     */
    public static boolean isValidRange(String fromDate, String toDate) {
        if(Strings.hasText(fromDate) && Strings.hasText(toDate)) {
            LocalDate from = Joda.forPattern(Settings.DEFAULT_DATE_FORMAT).parser().parseLocalDate(fromDate);
            LocalDate to = Joda.forPattern(Settings.DEFAULT_DATE_FORMAT).parser().parseLocalDate(toDate);
            if(from.isBefore(to) || from.equals(to)){
                return true;
            }
        }
        return false;
    }


    /**
     * A wrapper for building OR filter
     * @param filterMap a map of selected facets with keys as fields and values as terms.
     * @see FilterUtils#buildBoolFilter(Map, String, String, String)
     **/
    public static Map<String, BoolFilterBuilder> buildBoolFilter(@NotNull Map<String, List<String>> filterMap) {
       return  buildBoolFilter(filterMap, null, null, null);
    }


    /**
     * A wrapper for building filter without dates
     ***/
    public static Map<String, BoolFilterBuilder> buildBoolFilter(
            @NotNull Map<String,
            List<String>> filterMap,
            String aggs) {

        return  buildBoolFilter(filterMap, aggs, null, null);
    }

    /**
     * A wrapper method for building BoolFilter based on the aggregation settings.
     */
    public static Map<String, BoolFilterBuilder> buildBoolFilter(HttpServletRequest request){
        //Get corresponding request parameters
        String fromDate = request.getParameter(Params.FROM_DATE);
        String toDate = request.getParameter(Params.TO_DATE);
        String[] selectedFilters = request.getParameterValues(Params.SELECTED_FILTERS);
        String aggregations = request.getParameter(Params.AGGREGATIONS);

        //Build a filter map based on selected facets. In the result map
        //keys are "fields" and values are "terms"
        //e.g {"subject.exact" = ["Flyfoto" , "Birkeland"], "type" = ["Brev"]}
        Map<String, List<String>> selectedFacetMap = AggregationUtils.buildFilterMap(selectedFilters);

        return buildBoolFilter(selectedFacetMap, aggregations, fromDate, toDate);
    }




        /**
         * A method for building BoolFilter based on the aggregation settings.
         *
         *  @param filterMap a map of selected facets with keys as fields and values as terms.
         *  @param aggs aggregations
         *  @param fromDate start date
         *  @param toDate   end date
         *
         *  @return  a map which contains AND and OR bool filters based on the aggregations
         */
    public static Map<String, BoolFilterBuilder> buildBoolFilter(@NotNull Map<String, List<String>> filterMap,
                                                                 @Nullable String aggs,
                                                                 @Nullable String fromDate,
                                                                 @Nullable String toDate) {
        Map<String, BoolFilterBuilder> boolFilterMap = new HashMap<>();
        BoolFilterBuilder filter = FilterBuilders.boolFilter();
        BoolFilterBuilder postFilter = FilterBuilders.boolFilter();

        try {
            //Building a filter based on the user selected facets. Starting with AND filter
                for (Map.Entry<String, List<String>> entry : filterMap.entrySet()) {
                    if (!entry.getValue().isEmpty()) {
                        if (AggregationUtils.contains(aggs, entry.getKey(), "operator", "AND")) {
                            for (Object value : entry.getValue()) {
                                //Building "AND" filter with "term" filter.
                                if (entry.getKey().startsWith(Settings.MINUS)) {
                                    //Exclude any filter that begins with minus sign ("-") by using MUST NOT filter;
                                    filter.mustNot(FilterBuilders.termFilter(entry.getKey().substring(1), value));
                                } else {
                                    filter.must(FilterBuilders.termFilter(entry.getKey(), value));
                                }
                            }
                        }
                        //Exclude any filter that begins with minus sign ("-") by using MUST NOT filter;
                        else if (entry.getKey().startsWith(Settings.MINUS)) {
                            filter.mustNot(FilterBuilders.termsFilter(entry.getKey().substring(1), entry.getValue()));
                        }
                        //Building "OR" filter using "terms" filter (default).
                        //Since it is OR, we will use it as post_filter.
                        //post_filter only affects search results but NOT aggregations.
                        else {
                            postFilter.must(FilterBuilders.termsFilter(entry.getKey(), entry.getValue()));
                        }
                    }
                }
        } catch (Exception ex) {
            logger.error("Exception occurred when constructing bool_filter [ " + ex + " ]");
        }

         //Append date range filter
         appendDateRangeFilter(filter, fromDate, toDate);

         boolFilterMap.put(Params.TOP_FILTER, filter);
         boolFilterMap.put(Params.POST_FILTER, postFilter);

        return boolFilterMap;
    }
}
