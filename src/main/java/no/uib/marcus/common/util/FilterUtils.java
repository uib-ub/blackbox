package no.uib.marcus.common.util;

import no.uib.marcus.common.RequestParams;
import org.apache.log4j.Logger;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * @author Hemed Ali
 */

public final class FilterUtils {
    private static final Logger logger = Logger.getLogger(FilterUtils.class);

    private FilterUtils(){}

    /**
     * A method for building BoolFilter based on the aggregation settings.
     */
    public static BoolFilterBuilder buildBoolFilter(HttpServletRequest request) {
        //Get corresponding request parameters
        String fromDate = request.getParameter(RequestParams.FROM_DATE);
        String toDate = request.getParameter(RequestParams.TO_DATE);
        String[] selectedFilters = request.getParameterValues(RequestParams.SELECTED_FILTERS);
        String aggregations = request.getParameter(RequestParams.AGGREGATIONS);

        //In this map, keys are "fields" and values are "terms"
        Map<String, List<String>> filterMap = AggregationUtils.getFilterMap(selectedFilters);
        BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();
        try {
            //Building date filter
            if (Strings.hasText(fromDate) || Strings.hasText(toDate)) {
                boolFilter
                        //Range within "available" field
                        .should(FilterBuilders.rangeFilter(RequestParams.DateField.AVAILABLE).gte(fromDate).lte(toDate))
                        //Range within "created" field
                        .should(FilterBuilders.rangeFilter(RequestParams.DateField.CREATED).gte(fromDate).lte(toDate))
                        //madeBefore >= from_date and madeBefore <= to_date
                        .should(FilterBuilders.boolFilter()
                                .must(FilterBuilders.rangeFilter(RequestParams.DateField.MADE_BEFORE).gte(fromDate))
                                .must(FilterBuilders.rangeFilter(RequestParams.DateField.MADE_BEFORE).lte(toDate)))
                        //madeAfter >= from_date and madeAfter <= to_date
                        .should(FilterBuilders.boolFilter()
                                .must(FilterBuilders.rangeFilter(RequestParams.DateField.MADE_AFTER).gte(fromDate))
                                .must(FilterBuilders.rangeFilter(RequestParams.DateField.MADE_AFTER).lte(toDate)));
            }
            //Building a filter based on the user selected facets
            for (Map.Entry<String, List<String>> entry : filterMap.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    if (AggregationUtils.contains(aggregations, entry.getKey(), "operator", "AND")) {
                        for (Object value : entry.getValue()) {
                            //Building "AND" filter.
                            boolFilter.must(FilterBuilders.termFilter(entry.getKey(), value));
                        }
                    } else {
                        //Building "OR" filter.
                        boolFilter.must(FilterBuilders.termsFilter(entry.getKey(), entry.getValue()));
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while constructing Bool filter" + ex.getLocalizedMessage());
        }
        return boolFilter;
    }
}
