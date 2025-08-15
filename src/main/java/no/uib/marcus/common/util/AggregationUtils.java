package no.uib.marcus.common.util;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation.Builder.ContainerBuilder;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramAggregation;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

import co.elastic.clients.elasticsearch.core.SearchRequest;

import co.elastic.clients.util.NamedValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import no.uib.marcus.search.IllegalParameterException;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Utility class for constructing aggregations.
 *
 * @author Hemed Ali Al Ruwehy
 *
 * University of Bergen
 *
 * @author Øyvind Gjesdal
 *
 * University of Bergen
 *
 * TODO aggregation logic currently work in progress
 *
 */
public final class AggregationUtils {
    private static final Logger logger = Logger.getLogger(AggregationUtils.class.getName());
    private static final String AGGS_FILTER_KEY = "aggs_filter";

    //Enforce non-instatiability
    private AggregationUtils() {
    }


    /**
     * Validate aggregations
     *
     * @param jsonString aggregations as JSON string
     * @ expects input to be valid json. Throws an exception if the string is not parseable as a json array
     * @throws IllegalParameterException if string is not JSON array
     **/
    public static void validateAggregations(String jsonString) {
        // rewrite from AI assistant using Jackson
        ObjectMapper mapper = new ObjectMapper();
        try {
        JsonNode node = mapper.readTree(jsonString);
        if (!node.isArray()){
            throw new IllegalParameterException(
                    "Aggregations must be valid JSON. Expected JSON Array of objects but found : [" + jsonString + "]");
        }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
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
            if (!StringUtils.hasText(aggregations)) {
                return false;
            }
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode facets = mapper.readTree(aggregations);
                for (JsonNode facet : facets)
                    if (facet.has("field") && facet.has(key)) {
                         String currentField = facet.get("field").asText();
                         String currentValue = facet.get(key).asText();
                         if (currentField.equals(field) && currentValue.equalsIgnoreCase(value)) {
                             return true;
                         }
                     }
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        } catch (RuntimeException e) {
            throw new RuntimeException(e);
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
    public static SearchRequest.Builder addAggregations(SearchRequest.Builder searchRequest, String aggregations) {
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
    public static SearchRequest.Builder addAggregations(SearchRequest.Builder searchRequest,
                                                        String aggregations,
                                                        Map<String, List<String>> selectedFacets) {
        JsonMapper mapper = new JsonMapper();
        JsonNode facets ;
        try {
            facets = mapper.readTree(aggregations);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Map<String, Aggregation> aggregationMap = new HashMap<>() ;
        for (JsonNode facet : facets) {
            if (facet.has("field")) {
              BoolQuery.Builder aggsFilter = FilterUtils.getPostFilter(selectedFacets, aggregations);

              //Add DateHistogram aggregations
                //@todo add map of Map<String, Aggregation> and send once
                if (facet.has("type") && facet.get("type").asText().equals("date_histogram")) {
                    aggregationMap.put(facet.get("field").asText(),
                            AggregationUtils.getDateHistogramAggregation(facet).build()._toAggregation());
                } else {
                    Aggregation agg;
                    ContainerBuilder termsAggs = constructTermsAggregation(facet);
                    logger.info("key for termsAggs: " + facet.get("field").asText());

                    if (selectedFacets != null && !selectedFacets.isEmpty()) {
                        //Get current field
                        String facetField = facet.get("field").asText();
                        // Logic: Whenever a user selects value from an "OR" aggregation,
                        // you add a corresponding filter (here aggs_filter) to all aggregations as sub aggregation
                        // EXCEPT for the aggregation in which the selection was performed in.
                        if (selectedFacets.containsKey(facetField)) {
                            //Make a copy of the map
                            Map<String, List<String>> selectedFacetCopy = new HashMap<>(
                                selectedFacets);
                            //Remove the facet that aggregation was performed in from the map
                            selectedFacetCopy.remove(facetField);

                            //Build bool_filter for the copy of the selected facets.
                            //We build sub aggregation filter only for "OR" facets
                            if (aggsFilter.hasClauses()){

                              BoolQuery.Builder aggsFilter2 = FilterUtils.getPostFilter(selectedFacetCopy, aggregations);

                              logger.info("Aggregations aggsfilter added to search request: " + aggsFilter2.toString());
                          agg = addSubAggregationFilter(aggsFilter2, facet
                              );
                          termsAggs.aggregations(AGGS_FILTER_KEY, agg);
                              aggregationMap.put(facet.get("field").asText(), agg);
                            }
                        }
                        else {
                            agg = addSubAggregationFilter(aggsFilter,facet);
                            aggregationMap.put(facet.get("field").asText(),agg);
                        }
                        }
                    aggregationMap.put(facet.get("field").asText(), termsAggs.build());


                }
            }
        }
        logger.info("Aggregations added to search request: " + aggregationMap.toString());
        return searchRequest.aggregations(aggregationMap);
    }

    /**
     * Adds sub aggregation filter with the name "aggs_filter". Sub aggregations are added only to "OR" facets
     */
    private static Aggregation addSubAggregationFilter(BoolQuery.Builder aggsFilter, JsonNode currentFacet) {

        return new Aggregation.Builder().filter(aggsFilter.build()._toQuery()).build();
    }
            // create a sub aggregation to add to an aggregation
            /**
             *  Map<String, Aggregation> map = new HashMap<>();
             *
             *     Aggregation subAggregation = new Aggregation.Builder()
             *         .avg(new AverageAggregation.Builder().field("revenue").build())
             *         .build();
             *
             *     Aggregation aggregation = new Aggregation.Builder()
             *         .terms(new TermsAggregation.Builder().field("director.keyword").build())
             *         .aggregations(new HashMap<>() {{
             *           put("avg_renevue", subAggregation);
             *         }}).build();
             *
             *     map.put("agg_director", aggregation);
             *
             *     SearchRequest searchRequest = new SearchRequest.Builder()
             *         .index("idx_name")
             *         .size(0)
             *         .aggregations(map)
             *         .build();
             */
         //   aggBuilder.term


    /**
     * A method to build a date histogram aggregation
     *
     * @param facet a JSON object
     * @return a date histogram builder
     */
    public static DateHistogramAggregation.Builder getDateHistogramAggregation(JsonNode facet) {
        String field = facet.get("field").asText();
        //Create date histogram
        DateHistogramAggregation.Builder dateHistBuilder = new DateHistogramAggregation.Builder();

         dateHistBuilder.field(field);

        //Set date format
        if (facet.has("format")) {
            logger.info("datehistogram has format");
            dateHistBuilder.format(facet.get("format").asText());
        }
        //Set interval
        if (facet.has("interval")) {
            logger.info("datehistogram has interval");
            dateHistBuilder.fixedInterval(new Time.Builder().time(facet.get("interval").asText()).build());
        }
        //Set number of minimum documents that should be returned
        if (facet.has("min_doc_count")) {
            dateHistBuilder.minDocCount(facet.get("min_doc_count").asInt());
        }
        //Set order
        if (facet.has("order")) {
            NamedValue<SortOrder> order ;

            if (facet.get("order").asText().equalsIgnoreCase("count_asc")) {
                order = new NamedValue<>("_count", SortOrder.Asc);
            } else if (facet.get("order").asText().equalsIgnoreCase("count_desc")) {
                order = new NamedValue<>("_count", SortOrder.Desc);
            } else if (facet.get("order").asText().equalsIgnoreCase("key_desc")) {
                order = new NamedValue<>("_key", SortOrder.Desc);
            }
            else {
                order = new NamedValue<>("_key", SortOrder.Asc);
            }
            dateHistBuilder.order(List.of(order));
        }
        return dateHistBuilder;
    }

    /**
     * Builds terms aggregations and their corresponding sort options
     *
     * @param facet                a JSON object
     * @param sortBySubAggregation a flag whether to sort by sub aggregation filter
     * @return a term builder
     *
     **/
    public static ContainerBuilder constructTermsAggregation(JsonNode facet, boolean sortBySubAggregation) {
        String field = facet.get("field").asText();

        Aggregation.Builder termsBuilder = new Aggregation.Builder();
        TermsAggregation.Builder termsAggregationBuilder = new TermsAggregation.Builder();
        logger.info("field for termsAggs2: " + field);

        termsAggregationBuilder.field(field);

       // move down termsBuilder.terms(new TermsAggregation.Builder().field(field));
        //Set size
        if (facet.has("size")) {
            int size = facet.get("size").asInt();
            termsAggregationBuilder.size(size);
        }
        //Set order
        /**
         *  Map<String, Aggregation> map = new HashMap<>();
         *
         *     Aggregation subAggregation = new Aggregation.Builder()
         *         .avg(new AverageAggregation.Builder().field("revenue").build())
         *         .build();
         *
         *     Aggregation aggregation = new Aggregation.Builder()
         *         .terms(new TermsAggregation.Builder().field("director.keyword").build())
         *         .aggregations(new HashMap<>() {{
         *           put("avg_renevue", subAggregation);
         *         }}).build();
         *
         *     map.put("agg_director", aggregation);
         *
         *     SearchRequest searchRequest = new SearchRequest.Builder()
         *         .index("idx_name")
         *         .size(0)
         *         .aggregations(map)
         *         .build();
         */

        NamedValue<SortOrder> subAggregationOrder = new NamedValue<>("_count", SortOrder.Asc);
      //  termsBuilder.order();

      //  Terms.Order subAggregationOrder = Terms.Order.aggregation(AGGS_FILTER_KEY, false);
        if (facet.has("order")) {

            NamedValue<SortOrder> order = new NamedValue<>("_count",SortOrder.Desc);//default order (count descending)
            if (facet.get("order").asText().equalsIgnoreCase("count_asc")) {
                order = new NamedValue<>("_count",SortOrder.Asc);
             //   subAggregationOrder = Terms.Order.aggregation(AGGS_FILTER_KEY, true);
            } else if (facet.get("order").asText().equalsIgnoreCase("term_asc")) {

            //    order = Terms.Order.term(true);
            //    subAggregationOrder = order; // for term_asc, sort by parent aggregation
            } else if (facet.get("order").asText().equalsIgnoreCase("term_desc")) {
           //     order = Terms.Order.term(false);
           //     subAggregationOrder = order; // for term_desc, sort by parent aggregation
            }
            if (sortBySubAggregation) {//Sort using sub aggregation
            //    termsBuilder.order(subAggregationOrder);
            } else { //sort normally using top aggregation
                termsAggregationBuilder.order(order);
            }
        } else {//if order is not specified
            if (sortBySubAggregation) { //use sub aggregation order, otherwise let Elasticsearch decides
                termsAggregationBuilder.order(subAggregationOrder);
            }
        }
        //Set number of minimum documents that should be returned
        if (facet.has("min_doc_count")) {
            int minDocCount = facet.get("min_doc_count").asInt();
            termsAggregationBuilder.minDocCount(minDocCount);
        }

        return termsBuilder.terms(termsAggregationBuilder.build()) ;
    }

    /**
     * A method to build terms aggregations and sort options on the parent aggregations
     *
     * @param facet a JSON object
     * @return a term builder
     */
    public static ContainerBuilder constructTermsAggregation(JsonNode facet) {
        return constructTermsAggregation(  facet, false);
    }

}
