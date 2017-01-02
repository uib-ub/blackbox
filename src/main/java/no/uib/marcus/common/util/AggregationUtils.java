package no.uib.marcus.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import no.uib.marcus.common.Params;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;

import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Utility class for constructing aggregations.
 *
 * @author Hemed Ali Al Ruwehy
 *         University of Bergen
 */
public final class AggregationUtils {
    private static final Logger logger = Logger.getLogger(AggregationUtils.class);
    private static final char AGGS_KEY_VALUE_SEPARATOR = '#';

    public AggregationUtils() {}

    /**
     * The method checks if the facets/aggregations contain a key that has a specified value.
     * Note that the facets must be valid JSON array. For example [
     * {"field": "status", "size": 15, "operator" : "AND", "order":
     * "term_asc"}, {"field" :"assigned_to" , "order" : "term_asc"}]
     *
     * @param aggregations aggregations as JSON string
     * @param field        field in which key-value exists
     * @param key          a facet key
     * @param value        a value of specified key
     * @return <code>true</code> if the key, in that field, has a specified value
     */
    public static boolean contains(String aggregations, String field, String key, String value) {
        try {
            //If there is no aggregations, no need to continue.
            if (!Strings.hasText(aggregations)) {
                return false;
            }
            JsonElement facets = new JsonParser().parse(aggregations);
            for (JsonElement e : facets.getAsJsonArray()) {
                JsonObject facet = e.getAsJsonObject();
                if (facet.has("field") && facet.has(key)) {
                    String currentField = facet.get("field").getAsString();
                    String currentValue = facet.get(key).getAsString();
                    if (currentField.equals(field) && currentValue.equalsIgnoreCase(value)) {
                        return true;
                    }
                }
            }
        } catch (JsonParseException e) {
            logger.warn("Aggregations should be valid JSON array, check the syntax for [" + aggregations + "]");
            return false;
        }
        return false;
    }

    /**
     * A method to build a map based on the selected filters. If no filter is
     * selected, return an empty map.
     *
     * @param selectedFilters a string of selected filters in the form of "field#value"
     * @return a filter map in the form of e.g {"subject.exact" = ["Flyfoto" , "Birkeland"], "type.exact" = ["Brev"]}
     */
    @NotNull
    public static Map<String, List<String>> buildFilterMap(@Nullable String[] selectedFilters) {
        Map<String, List<String>> filters = new HashMap<>();
        try {
            if (selectedFilters == null || selectedFilters.length == 0) {
                return Collections.emptyMap();
            }
            for (String entry : selectedFilters) {
                if (entry.lastIndexOf(AGGS_KEY_VALUE_SEPARATOR) != -1) {
                    //Get the index for the last occurrence of a separator
                    int lastIndex = entry.lastIndexOf(AGGS_KEY_VALUE_SEPARATOR);
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
            logger.error("Exception occurred while constructing a map from selected filters: " + ex.getMessage());
        }
        return filters;
    }


    /**
     * A method to append aggregations to the search request builder.
     *
     * @param searchRequest a search request builder
     *                      @param aggregations aggregations as JSON array string
     * @return the same search request where aggregations have been added to
     * it.
     */
    public static SearchRequestBuilder addAggregations(SearchRequestBuilder searchRequest, String aggregations) {
        return addAggregations(searchRequest, aggregations, null);
    }

    /**
     * A method to append aggregations to the search request builder.
     *
     * @param searchRequest a search request builder
     * @param selectedFacets     a map that contains selected facets
     * @return the same search request where aggregations have been added to
     * it.
     */
    public static SearchRequestBuilder addAggregations(SearchRequestBuilder searchRequest,
                                                       String aggregations,
                                                       Map<String, List<String>> selectedFacets)
            throws JsonParseException, IllegalStateException {

        JsonElement jsonElement = new JsonParser().parse(aggregations);
        for (JsonElement facets : jsonElement.getAsJsonArray()) {
            JsonObject currentFacet = facets.getAsJsonObject();
            if (currentFacet.has("field")) {
                //Add DateHistogram aggregations
                if (currentFacet.has("type") && currentFacet.get("type").getAsString().equals("date_histogram")) {
                    searchRequest.addAggregation(
                            AggregationUtils.getDateHistogramAggregation(currentFacet));
                } else {
                    AggregationBuilder termsAggs = AggregationUtils.getTermsAggregation(currentFacet);

                    if (selectedFacets != null && !selectedFacets.isEmpty()) {
                        //Get current field
                        String facetField = currentFacet.get("field").getAsString();

                        // Logic: Whenever a user selects value from an "OR" aggregations,
                        // you add a corresponding filter to all aggregations as sub aggregations (here aggs_filter)
                        // EXCEPT for the aggregation in which the selection was done in.
                        if (selectedFacets.containsKey(facetField)) {
                            //&& currentFacet.has("operator")
                            //&& currentFacet.get("operator").getAsString().equalsIgnoreCase("OR")){
                            //Make a copy of this map.
                            Map<String, List<String>> selectedFacetCopy = new HashMap<>(selectedFacets);
                            //Remove the facet that aggregation was performed in from the map
                            selectedFacetCopy.remove(facetField);

                            //Build bool_filter for the copy of the selected facets.
                            //Since we did not pass aggregations as argument in buildBolFilter() method,
                            // we are sure it is an OR_BOOL_FILTER (terms bool_filter)
                            BoolFilterBuilder boolFilterCopy = FilterUtils.buildBoolFilter(selectedFacetCopy).get(Params.OR_BOOL_FILTER);
                            if (boolFilterCopy.hasClauses()) {
                                termsAggs.subAggregation(
                                        AggregationBuilders.filter("aggs_filter").filter(boolFilterCopy));
                            }
                        } else {
                            //Build a top level filter
                            BoolFilterBuilder boolFilter = FilterUtils.buildBoolFilter(selectedFacets).get(Params.OR_BOOL_FILTER);
                            if (boolFilter.hasClauses()) {
                                termsAggs.subAggregation(
                                        AggregationBuilders.filter("aggs_filter").filter(boolFilter)
                                );
                            }
                        }
                    }
                    searchRequest.addAggregation(termsAggs);
                }

            }
        }
        return searchRequest;
    }


    /**
     * A method to build a date histogram aggregation
     *
     * @param facet a JSON object
     * @return a date histogram builder
     */
    public static DateHistogramBuilder getDateHistogramAggregation(JsonObject facet) {
        String field = facet.get("field").getAsString();
        //Create date histogram
        DateHistogramBuilder dateHistogram = AggregationBuilders
                .dateHistogram(field)
                .field(field);
        //Set date format
        if (facet.has("format")) {
            dateHistogram.format(facet.get("format").getAsString());
        }
        //Set interval
        if (facet.has("interval")) {
            dateHistogram.interval(new DateHistogram.Interval(facet.get("interval").getAsString()));
        }
        //Set number of minimum documents that should be returned
        if (facet.has("min_doc_count")) {
            dateHistogram.minDocCount(facet.get("min_doc_count").getAsLong());
        }
        //Set order
        if (facet.has("order")) {
            Histogram.Order order = Histogram.Order.KEY_ASC;
            if (facet.get("order").getAsString().equalsIgnoreCase("count_asc")) {
                order = Histogram.Order.COUNT_ASC;
            } else if (facet.get("order").getAsString().equalsIgnoreCase("count_desc")) {
                order = Histogram.Order.COUNT_DESC;
            } else if (facet.get("order").getAsString().equalsIgnoreCase("key_desc")) {
                order = Histogram.Order.KEY_DESC;
            }
            dateHistogram.order(order);
        }
        return dateHistogram;
    }

    /**
     * A method to build terms aggregations
     *
     * @param facet a JSON object
     * @return a term builder
     */
    public static TermsBuilder getTermsAggregation(JsonObject facet) {
        String field = facet.get("field").getAsString();
        TermsBuilder termsBuilder = AggregationBuilders
                .terms(field)
                .field(field);
        //Set size
        if (facet.has("size")) {
            int size = facet.get("size").getAsInt();
            termsBuilder.size(size);
        }
        //Set order
        if (facet.has("order")) {
            Terms.Order order = Terms.Order.count(false);//default to count descending
            if (facet.get("order").getAsString().equalsIgnoreCase("count_asc")) {
                order = Terms.Order.count(true);
            } else if (facet.get("order").getAsString().equalsIgnoreCase("term_asc")) {
                order = Terms.Order.term(true);
            } else if (facet.get("order").getAsString().equalsIgnoreCase("term_desc")) {
                order = Terms.Order.term(false);
            }
            termsBuilder.order(order);
        }
        //Set number of minimum documents that should be returned
        if (facet.has("min_doc_count")) {
            long minDocCount = facet.get("min_doc_count").getAsLong();
            termsBuilder.minDocCount(minDocCount);
        }
        return termsBuilder;
    }

}
