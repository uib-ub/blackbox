package no.uib.marcus.common.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import no.uib.marcus.search.IllegalParameterException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.common.Strings;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Utility class for constructing aggregations.
 *
 * @author Hemed Ali Al Ruwehy
 * University of Bergen
 */
public final class AggregationUtils {
    private static final Logger logger = LogManager.getLogger(AggregationUtils.class);
    private static final String AGGS_FILTER_KEY = "aggs_filter";

    //Enforce non-instatiability
    private AggregationUtils() {
    }

    /**
     * Validate aggregations
     *
     * @param jsonString aggregations as JSON string
     * @return true if string is JSON array
     * @throws IllegalParameterException if string is not JSON array
     * @throws JsonParseException        is string is not valid JSON
     **/
    public static void validateAggregations(String jsonString) {
        JsonElement element = new JsonParser().parse(jsonString);
        if (!element.isJsonArray()) {
            throw new IllegalParameterException(
                    "Aggregations must be valid JSON. Expected JSON Array of objects but found : [" + jsonString + "]");
        }
    }

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
     * A method to append aggregations to the search request builder.
     *
     * @param searchRequest a search request builder
     * @param aggregations  aggregations as JSON array string
     * @return the same search request where aggregations have been added to
     * it.
     */
    public static SearchRequestBuilder addAggregations(SearchRequestBuilder searchRequest, String aggregations) {
        return addAggregations(searchRequest, aggregations, null);
    }

    /**
     * A method to append aggregations to the search request builder.
     *
     * @param searchRequest  a search request builder
     * @param selectedFacets a map that contains selected facets
     * @return the same search request where aggregations have been added to
     * it.
     */
    public static SearchRequestBuilder addAggregations(SearchRequestBuilder searchRequest,
                                                       String aggregations,
                                                       Map<String, List<String>> selectedFacets)
            throws JsonParseException, IllegalStateException {
        JsonElement jsonElement = new JsonParser().parse(aggregations);
        for (JsonElement facets : jsonElement.getAsJsonArray()) {
            JsonObject facet = facets.getAsJsonObject();
            if (facet.has("field")) {
                //Add DateHistogram aggregations
                if (facet.has("type") && facet.get("type").getAsString().equals("date_histogram")) {
                    searchRequest.addAggregation(AggregationUtils.getDateHistogramAggregation(facet));
                } else {
                    AggregationBuilder termsAggs = constructTermsAggregation(facet);
                    if (selectedFacets != null && selectedFacets.size() > 0) {
                        //Get current field
                        String facetField = facet.get("field").getAsString();
                        // Logic: Whenever a user selects value from an "OR" aggregation,
                        // you add a corresponding filter (here aggs_filter) to all aggregations as sub aggregation
                        // EXCEPT for the aggregation in which the selection was performed in.
                        if (selectedFacets.containsKey(facetField)) {
                            //Make a copy of the map
                            Map<String, List<String>> selectedFacetCopy = new HashMap<>(selectedFacets);
                            //Remove the facet that aggregation was performed in from the map
                            selectedFacetCopy.remove(facetField);
                            //Build bool_filter for the copy of the selected facets.
                            //We build sub aggregation filter only for "OR" facets
                            termsAggs = addSubAggregationFilter(aggregations, facet, termsAggs, selectedFacetCopy);
                        } else {
                            termsAggs = addSubAggregationFilter(aggregations, facet, termsAggs, selectedFacets);
                        }
                    }
                    searchRequest.addAggregation(termsAggs);
                }
            }
        }
        return searchRequest;
    }

    /**
     * Adds sub aggregation filter with the name "aggs_filter". Sub aggregations are added only to "OR" facets
     */
    private static AggregationBuilder addSubAggregationFilter(String aggs, JsonObject currentFacet,
                                                              AggregationBuilder termsAggs,
                                                              Map<String, List<String>> selectedFacets)
    {
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        query.filter();
        BoolFilterBuilder aggsFilter = FilterUtils.getPostFilter(selectedFacets, aggs);
        if (aggsFilter.hasClauses()) {
            termsAggs = constructTermsAggregation(currentFacet, true);
            termsAggs.subAggregation(AggregationBuilders.filter(AGGS_FILTER_KEY).filter(aggsFilter));
        }
        return termsAggs;
    }


    /**
     * A method to build a date histogram aggregation
     *
     * @param facet a JSON object
     * @return a date histogram builder
     */
    public static DateHistogramAggregationBuilder getDateHistogramAggregation(JsonObject facet) {
        String field = facet.get("field").getAsString();
        //Create date histogram
        DateHistogramAggregationBuilder dateHistogram = AggregationBuilders
                .dateHistogram(field)
                .field(field);
        //Set date format
        if (facet.has("format")) {
            dateHistogram.format(facet.get("format").getAsString());
        }
        //Set interval
        if (facet.has("interval")) {
            dateHistogram.fixedInterval(new DateHistogramInterval(facet.get("interval").getAsString()));
        }
        //Set number of minimum documents that should be returned
        if (facet.has("min_doc_count")) {
            dateHistogram.minDocCount(facet.get("min_doc_count").getAsLong());
        }
        //Set order
        if (facet.has("order")) {
            BucketOrder order = BucketOrder.key(true);
            if (facet.get("order").getAsString().equalsIgnoreCase("count_asc")) {
                order =  BucketOrder.count(true);
            } else if (facet.get("order").getAsString().equalsIgnoreCase("count_desc")) {
                order =  BucketOrder.count(false);
            } else if (facet.get("order").getAsString().equalsIgnoreCase("key_desc")) {
                order =  BucketOrder.key(false);
            }
            dateHistogram.order(order);
        }
        return dateHistogram;
    }

    /**
     * Builds terms aggregations and their corresponding sort options
     *
     * @param facet                a JSON object
     * @param sortBySubAggregation a flag whether to sort by sub aggregation filter
     * @return a term builder
     */
    public static AggregationBuilder constructTermsAggregation(JsonObject facet, boolean sortBySubAggregation) {
        String field = facet.get("field").getAsString();
        Object filterList;
        QueryBuilders.termsQuery("keyword", toLowerCase(filterList));
        AggregationBuilder termsBuilder = AggregationBuilders.terms(field).field(field);
        //Set size
        if (facet.has("size")) {
            int size = facet.get("size").getAsInt();
            termsBuilder.size(size);
        }
        //Set order
        Order subAggregationOrder = Terms.x|(aggregation(AGGS_FILTER_KEY, false);
        if (facet.has("order")) {
            Terms.Order order = Terms.Order.count(false);//default order (count descending)
            if (facet.get("order").getAsString().equalsIgnoreCase("count_asc")) {
                order = Terms.Order.count(true);
                subAggregationOrder = Terms.Order.aggregation(AGGS_FILTER_KEY, true);
            } else if (facet.get("order").getAsString().equalsIgnoreCase("term_asc")) {
                order = Terms.Order.term(true);
                subAggregationOrder = order; // for term_asc, sort by parent aggregation
            } else if (facet.get("order").getAsString().equalsIgnoreCase("term_desc")) {
                order = Terms.Order.term(false);
                subAggregationOrder = order; // for term_desc, sort by parent aggregation
            }
            if (sortBySubAggregation) {//Sort using sub aggregation
                termsBuilder.order(subAggregationOrder);
            } else { //sort normally using top aggregation
                termsBuilder.order(order);
            }
        } else {//if order is not specified
            if (sortBySubAggregation) { //use sub aggregation order, otherwise let Elasticsearch decides
                termsBuilder.order(subAggregationOrder);
            }
        }
        //Set number of minimum documents that should be returned
        if (facet.has("min_doc_count")) {
            long minDocCount = facet.get("min_doc_count").getAsLong();
            termsBuilder.minDocCount(minDocCount);
        }
        return termsBuilder;
    }

    /**
     * A method to build terms aggregations and sort options on the parent aggregations
     *
     * @param facet a JSON object
     * @return a term builder
     */
    public static AggregationBuilder constructTermsAggregation(JsonObject facet) {
        return constructTermsAggregation(facet, false);
    }

}
