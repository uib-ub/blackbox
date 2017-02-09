package no.uib.marcus.common.util;

import no.uib.marcus.search.IllegalParameterException;
import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Hemed
 */
public final class SortUtils {
    private static final Logger logger = Logger.getLogger(SortUtils.class);
    private static final char FIELD_SORT_TYPE_SEPARATOR = ':';
    private static final String SORT_FIELD = "sort_field";
    private static final String SORT_ORDER = "sort_order";

    private SortUtils(){}


    /**
     * Extract
     * @param sortString a string that contains a field and sort type in
     * the form of "field:asc" or "field:desc"
     * @return a map with keys sort_field and sort_order
     */
    public static Map<String, String> extractSortField (String sortString) {
        Map<String,String> fieldSortMap = new HashMap<>();
        try {
            int lastIndex = sortString.lastIndexOf(FIELD_SORT_TYPE_SEPARATOR);
            //Fail first, if colon is not found.
            if(lastIndex == -1){
                throw new IllegalParameterException("The sort string does not contain a colon, "
                        + "hence cannot be split into field-value pair. "
                        + "The method expects to find a colon that separate a field and it's sort type "
                        + "but found: " + sortString);
            }
            String field = sortString.substring(0, lastIndex).trim();
            String order = sortString.substring(lastIndex + 1, sortString.length()).trim();
            //Insert values to the map
            fieldSortMap.put(SORT_FIELD, field);
            fieldSortMap.put(SORT_ORDER, order);
        } catch (StringIndexOutOfBoundsException e) {
            logger.error(e.getMessage() + " for input " + sortString);
        }
        return fieldSortMap;
    }

    /**
     * Building a fieldSort.
     **/
    public static SortBuilder getFieldSort(String sortString) {
        SortBuilder sortBuilder = null;
        SortOrder sortOrder = null;
        try {
            if(sortString.equals("_score")){
                return getScoreSort();
            }
            //If it's not score sort, it's field sort
            String field = extractSortField(sortString).get(SORT_FIELD);
            String order = extractSortField(sortString).get(SORT_ORDER);

            if (order.equalsIgnoreCase("asc")) {
                sortOrder = SortOrder.ASC;
            }
            if (order.equalsIgnoreCase("desc")) {
                sortOrder = SortOrder.DESC;
            }
            //Build sort
            sortBuilder = SortBuilders
                    .fieldSort(field)
                    .missing("_last");

            if (sortBuilder != null) {
                sortBuilder.order(sortOrder);
            }
        } catch (ElasticsearchException e) {
            logger.error("Sorting cannot be constructed. " + e.getDetailedMessage());
        }
        return sortBuilder;
    }


    /**
     * Building a score sort. It is descending order by default.
     **/
    public static SortBuilder getScoreSort() {
        SortBuilder sortBuilder = null;
        try {
            //Build socre sorting
            sortBuilder = SortBuilders
                    .scoreSort()
                    .missing("_last");
        } catch (ElasticsearchException e) {
            logger.error("Score sorting cannot be constructed. " + e.getMostSpecificCause());
        }
        return sortBuilder;
    }
}
