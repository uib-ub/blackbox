package no.uib.marcus.common.util;

import no.uib.marcus.common.Params;
import org.apache.log4j.Logger;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Hemed Ali
 */

public final class FilterUtils {
    private static final Logger logger = Logger.getLogger(FilterUtils.class);
    private FilterUtils() {}

    /**
     * Append date range filter to a bool_filter based on the University Library's logic for a date range.
     * @param boolFilter  a filter in which date ranges will be appended to.
     * @param fromDate from_date as a string in the form of YYYY or YYYY-MM or YYYY-MM-DD
     * @param toDate to_date as a string in the form of YYYY or YYYY-MM or YYYY-MM-DD
     ***/
    public static BoolFilterBuilder appendDateRangeFilter(BoolFilterBuilder boolFilter, String fromDate, String toDate){
        if(boolFilter == null){
            throw new IllegalArgumentException("We cannot append date ranges to a NULL bool_filter");
        }
        //Building date filter
        if (Strings.hasText(fromDate) || Strings.hasText(toDate)) {
            boolFilter
                    //Range within "available" field
                    //.should(FilterBuilders.rangeFilter(Params.DateField.AVAILABLE).gte(fromDate).lte(toDate))
                    //Range within "created" field
                    .should(FilterBuilders.rangeFilter(Params.DateField.CREATED).gte(fromDate).lte(toDate))
                    //madeBefore >= from_date and madeBefore <= to_date
                    .should(FilterBuilders.boolFilter()
                            .must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(fromDate))
                            .must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).lte(toDate)))
                    //madeAfter >= from_date and madeAfter <= to_date
                    .should(FilterBuilders.boolFilter()
                            .must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).gte(fromDate))
                            .must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).lte(toDate)));
        }
        return boolFilter;
    }

    /**
     * A wrapper for building OR filter
     **/
    public static Map<String, BoolFilterBuilder> buildBoolFilter(@NotNull Map<String, List<String>> filterMap) {
       return  buildBoolFilter(filterMap, null, null, null);
    }

    /**
     * A wrapper for building filter without dates
     ***/
    public static Map<String, BoolFilterBuilder> buildBoolFilter(@NotNull Map<String, List<String>> filterMap, String aggs) {
        return  buildBoolFilter(filterMap, aggs, null, null);
    }



    /**
     * A method for building BoolFilter based on the aggregation settings.
     *  @param filterMap a list of selected filters
     *  @param aggs
     *  @param fromDate
     *  @param toDate
     */
    public static Map<String, BoolFilterBuilder> buildBoolFilter(@NotNull Map<String, List<String>> filterMap,
                                                                 @Nullable String aggs,
                                                                 @Nullable String fromDate,
                                                                 @Nullable String toDate) {
        Map<String, BoolFilterBuilder> boolFilterMap = new HashMap<>();
        BoolFilterBuilder andBoolFilter = FilterBuilders.boolFilter();
        BoolFilterBuilder orBoolFilter = FilterBuilders.boolFilter();
        try {
            //TODO: Make AND as a default filter?.
            //Map<String, List<String>> filterMap = AggregationUtils.buildFilterMap(selectedFilters);
            //Building a filter based on the user selected facets
            for (Map.Entry<String, List<String>> entry : filterMap.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    if (AggregationUtils.contains(aggs, entry.getKey(), "operator", "AND")) {
                        for (Object value : entry.getValue()) {
                            //Building "AND" filter with "term" filter.
                            if (entry.getKey().startsWith("-")) {
                                //Exclude any filter that begins with minus sign ("-") by using MUST NOT filter;
                                andBoolFilter.mustNot(FilterBuilders.termFilter(entry.getKey().substring(1), value));
                            } else {
                                andBoolFilter.must(FilterBuilders.termFilter(entry.getKey(), value));
                            }
                        }
                    }//Building "OR" filter using "terms" filter (which is default)
                    else if (entry.getKey().startsWith("-")) {
                        //Exclude any filter that begins with minus sign ("-") by using MUST NOT filter;
                        orBoolFilter.mustNot(FilterBuilders.termsFilter(entry.getKey().substring(1), entry.getValue()));
                    }
                    else {
                        orBoolFilter.must(FilterBuilders.termsFilter(entry.getKey(), entry.getValue()));
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Exception occurred when constructing bool_filter [ " + ex + " ]");
        }
        //Append date range filter to this filter
        appendDateRangeFilter(andBoolFilter, fromDate, toDate);

         boolFilterMap.put(Params.AND_BOOL_FILTER , andBoolFilter);
         boolFilterMap.put(Params.OR_BOOL_FILTER , orBoolFilter);

        return boolFilterMap;
    }

}
