
package no.uib.marcus.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;

/**
 * @author Hemed Ali Al Ruwehy
 * University of Bergen
 */
public class AggregationUtil {
         private static final Logger logger = Logger.getLogger(AggregationUtil.class);
         
        /**
         * The method checks if the facets/aggregations contain a key that has a specified value. 
         * <br/>
         * Note that the facets must be valid JSON array. For example [
         * {"field": "status", "size": 15, "operator" : "AND", "order":
         * "term_asc"}, {"field" :"assigned_to" , "order" : "term_asc"}]
         * 
         * @param aggregation - aggregations as JSON string
         * @param  key a facet key
         * @param  value a value of specified key
         * 
         * @return true if the key has specified value.
         *
         */
        public static boolean contains(String aggregations, String key, String value) {
                try {
                        JsonElement facets = new JsonParser().parse(aggregations);
                        for (JsonElement e : facets.getAsJsonArray()) {
                                JsonObject facet = e.getAsJsonObject();
                                if (facet.has(key)) {
                                        String operator = facet.get(key).getAsString();
                                        if (operator.equalsIgnoreCase(value)) {
                                                return true;
                                        }
                                }
                        }
                } catch (ElasticsearchException e) {
                        throw e;
                } catch (Exception e) {
                        logger.error("Facets could not be processed. Please check the syntax."
                                + "Facets need to be valid JSON array: " + aggregations);
                        return false;
                }
                return false;
        }
        
        
        /**
         * A method to get a map based on the selected filters. If no filter is
         * selected, return an empty map.
         *
         * @param selectedFilters  a string of selected filters in the form of
         * "field.value"
         */
        public static Map getFilterMap(String[] selectedFilters) {
                Map<String, List<String>> filters = new HashMap<>();
                try {
                        if (selectedFilters == null) {
                                return Collections.emptyMap();
                        }
                        for (String entry : selectedFilters) {
                                if (entry.lastIndexOf('.') != -1) {
                                        //Get the index for the last occurence of a dot
                                        int lastIndex = entry.lastIndexOf('.');
                                        String key = entry.substring(0, lastIndex).trim();
                                        String value = entry.substring(lastIndex + 1, entry.length()).trim();
                                        //Should we allow empty values? maybe :) 
                                        if (!filters.containsKey(key)) {
                                                List<String> valuesList = new ArrayList<>();
                                                valuesList.add(value);
                                                filters.put(key, valuesList);
                                        } else {
                                                filters.get(key).add(value);
                                        }
                                }
                        }
                } catch (Exception ex) {
                        logger.error("Exception occured while constructing a map from selected filters: " + ex.getMessage());
                }
                return filters;
        }
    
}
