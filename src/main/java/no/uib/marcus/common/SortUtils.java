
package no.uib.marcus.common;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

/**
 * @author Hemed
 */
public class SortUtils {
         private static final Logger logger = Logger.getLogger(AggregationUtils.class);
         
        /**
         * Building a fieldSort.
         *
         * @param sortString a string that contains a field and sort type in
         * the form of "field:asc" or "field:desc"
         **/
        public static SortBuilder getFieldSort(String sortString) {
                SortBuilder sortBuilder = null;
                SortOrder sortOrder = null;
                try {
                        int lastIndex = sortString.lastIndexOf(':');
                        String field = sortString.substring(0, lastIndex).trim();
                        String order = sortString.substring(lastIndex + 1, sortString.length()).trim();

                        if (order.equalsIgnoreCase("asc")) {
                                sortOrder = SortOrder.ASC;
                        }
                        if (order.equalsIgnoreCase("desc")) {
                                sortOrder = SortOrder.DESC;
                        }
                        /* Build sort */
                        sortBuilder = SortBuilders
                                .fieldSort(field)
                                .missing("_last");

                        if (sortBuilder != null) {
                                sortBuilder.order(sortOrder);
                        }

                } catch (ElasticsearchException e) {
                        logger.error("Sorting cannot be constructed. " + e.getDetailedMessage());
                } catch (StringIndexOutOfBoundsException e) {
                        logger.error("The sort string does not contain a colon, hence cannot be split into field-value pair. "
                                + "The method expects to find a colon that separate a field and it's sort type "
                                + "but found: " + sortString);
                }
                return sortBuilder;
        }

}
