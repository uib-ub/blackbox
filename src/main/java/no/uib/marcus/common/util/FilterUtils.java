package no.uib.marcus.common.util;

import no.uib.marcus.common.Params;
import no.uib.marcus.common.Settings;
import org.apache.log4j.Logger;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.joda.FormatDateTimeFormatter;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.joda.time.DateTime;
import org.elasticsearch.common.joda.time.DateTimeUtils;
import org.elasticsearch.common.joda.time.LocalDate;
import org.elasticsearch.common.joda.time.LocalDateTime;
import org.elasticsearch.common.joda.time.format.DateTimeFormat;
import org.elasticsearch.common.joda.time.format.DateTimeParser;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

import javax.servlet.http.HttpServletRequest;
import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Utility class for building Elasticsearch filters
 * @author Hemed Ali
 */

public final class FilterUtils {
    private static final Logger logger = Logger.getLogger(FilterUtils.class);

    /**Prevent this class be initialized**/
    private FilterUtils() {}

    /**
     * Append date range filter to a bool_filter based on the University Library's logic for a date range.
     * Logic: Given a date range, fromDate and toDate, find any resource in which this range lies.
     * In other words, any resource that fulfill the following conditions:-
     *
                                  from_date----------to_date
         from_date-----------------to_date                  from_date-------------to_date
         from_date----------------------------------------------------------------to_date
                        from_date---------------------------------to_date
                        from_date-----to_date       from_date----to_date
                        madeAfter================================madeBefore


     * @param boolFilter  a filter in which date ranges will be appended to.
     * @param fromDate from_date as a string in the form of YYYY or YYYY-MM or YYYY-MM-DD
     * @param toDate to_date as a string in the form of YYYY or YYYY-MM or YYYY-MM-DD
     *
     * @return a bool_filter with date ranges
     ***/
    public static BoolFilterBuilder appendDateRangeFilter(BoolFilterBuilder boolFilter, String fromDate, String toDate) {
        if(boolFilter == null){
            throw new IllegalArgumentException("We cannot append date ranges to a null bool_filter");
        }

        //Building date filter
        if (Strings.hasText(fromDate) || Strings.hasText(toDate)) {

            boolFilter
                    //Range within "available" field
                    //.should(FilterBuilders.rangeFilter(Params.DateField.AVAILABLE).gte(fromDate).lte(toDate))
                    //Range within "created" field
                    .should(FilterBuilders.rangeFilter(Params.DateField.CREATED).gte(fromDate).lte(toDate))

                    /*.should(FilterBuilders.boolFilter()
                            //.should(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).gte(fromDate).lte(toDate))
                            //.should(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(fromDate).lte(toDate))
                            .must(FilterBuilders.boolFilter()
                                    .must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(fromDate).gte(toDate))
                                    .must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).lte(fromDate).lte(toDate))));
                    */

                    /*

                         Here, the condition in which madeAfter >= from_date and madeAfter <= to_date is taken care.
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
                     Here the condition in which madeBefore >= from_date and madeBefore <= to_date


                                                                      from_date-----------------to_date
                     from_date------------------------------------------------------------------------to_date
                                                                 from_date----to_date
                                       madeAfter================================madeBefore


                    */
                    .should(FilterBuilders.boolFilter()
                            .must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(fromDate).lte(toDate)));
                            //.must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).lte(toDate)))


                   appendBoolFilterIfValidRange(boolFilter, fromDate, toDate);


                    /*
                          This is a case that fromDate and toDate are within madeAfter and madeBefore range:

                          from_date-------------------------------------------------------to_date
                                      from_date-----------------------to_date
                         made_after---------------------------------------------------made_before
                    */
                    /*.should(FilterBuilders.boolFilter()
                                    .must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(fromDate).gte(toDate))
                                    .must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).lte(fromDate).lte(toDate))
                            //.must(FilterBuilders.scriptFilter("doc['madeAfter'].value < doc['madeBefore'].value"))
                            //.must(FilterBuilders.scriptFilter("toDate > fromDate").addParam("toDate", 10).addParam("fromDate", 90))
                    )

                    //.must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(toDate))
                                    //.must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).lte(toDate)))

                     ;


                    /*


                         from_date <= madeBefore AND to_date => madeAfter

                                                    from_date----------to_date
                         from_date-----------------to_date               from_date-----------------to_date
                         from_date---------------------------------------------------------------------to_date
                                       from_date---------------------------------to_date
                                       from_date-----to_date       from_date----to_date
                                       madeAfter================================madeBefore
                     */

                    /*.should(FilterBuilders.boolFilter()
                            .must(FilterBuilders.rangeFilter(fromDate).lte(Params.DateField.MADE_BEFORE))
                            .must(FilterBuilders.rangeFilter(toDate).gte(Params.DateField.MADE_AFTER)));*/



                    /*.should(FilterBuilders.boolFilter()
                            .must(FilterBuilders.rangeFilter(fromDate).lte(Params.DateField.MADE_BEFORE))
                            .must(FilterBuilders.rangeFilter(toDate).gte(Params.DateField.MADE_BEFORE)))
                            */


                    /*
                       from_date <= madeAfter AND to_date => madeAfter

                     */
                    /*.should(FilterBuilders.boolFilter()
                                    .must(FilterBuilders.rangeFilter(fromDate).lte(Params.DateField.MADE_AFTER))
                                    .must(FilterBuilders.rangeFilter(toDate).gte(Params.DateField.MADE_AFTER)))
                                    */









        }
        return boolFilter;
    }


    /**
     *  Validate date range
     *
     * @param fromDate from date
     * @param toDate to date
     * @return {@code true } if toDate is greater or equal to fromDate, otherwise {@code false}
     * @throws ParseException
     */
    public static boolean isValidRange (String fromDate, String toDate) throws ParseException {
        if(Strings.hasText(fromDate) && Strings.hasText(toDate)) {
            DateTime from = Joda.forPattern("yyyy-mm-dd||yyyy-mm||yyyy").parser().parseDateTime(fromDate);
            DateTime to = Joda.forPattern("yyyy-mm-dd||yyyy-mm||yyyy").parser().parseDateTime(toDate);

            if(from.isBefore(to) || from.equals(to)){
                return true;
            }
        }
        return false;
    }


    private  static BoolFilterBuilder appendBoolFilterIfValidRange(BoolFilterBuilder boolFilter, String fromDate, String toDate) {
        try {
            if(isValidRange(fromDate, toDate)) {
                boolFilter.should(FilterBuilders.boolFilter()
                                .must(FilterBuilders.rangeFilter(Params.DateField.MADE_BEFORE).gte(fromDate).gte(toDate))
                                .must(FilterBuilders.rangeFilter(Params.DateField.MADE_AFTER).lte(fromDate).lte(toDate)));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return boolFilter;
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
    public static Map<String, BoolFilterBuilder> buildBoolFilter(@NotNull Map<String, List<String>> filterMap, String aggs) {
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
         *  @param filterMap a map of selected facets with keys as fields and values as terms.
         *  @param aggs
         *  @param fromDate
         *  @param toDate
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
            //Building a filter based on the user selected facets.
            //Starting with AND filter
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
         //Append date range filter to this filter
         appendDateRangeFilter(filter, fromDate, toDate);
         boolFilterMap.put(Params.TOP_FILTER, filter);
         boolFilterMap.put(Params.POST_FILTER, postFilter);

        return boolFilterMap;
    }
}
