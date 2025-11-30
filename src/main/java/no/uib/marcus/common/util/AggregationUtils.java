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
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.logging.Level;
import no.uib.marcus.search.IllegalParameterException;

import java.util.logging.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Utility class for constructing aggregations.
 *
 * @author Hemed Ali Al Ruwehy
 * <p>
 * University of Bergen
 * @author Øyvind Gjesdal
 * <p>
 * University of Bergen
 *
 *
 *
 */
public final class AggregationUtils {

  private static final Logger logger = Logger.getLogger(AggregationUtils.class.getName());
  private static final String AGGS_FILTER_KEY = "aggs_filter";
  private static final JsonMapper JSON_MAPPER = new JsonMapper();

  // Sorting Constants
  private static final String ORDER_COUNT_ASC = "count_asc";
  private static final String ORDER_COUNT_DESC = "count_desc";
  private static final String ORDER_TERM_ASC = "term_asc"; // replaced with key in newer ES, but input params are still term_ prefix
  private static final String ORDER_TERM_DESC = "term_desc";
  private static final String KEY_COUNT = "_count";
  private static final String KEY_TERM = "_key"; // Elastic 8+ uses _key instead of _term
  private static final String FIELD = "field"; // Elastic 8+ uses _key instead of _term
  private static final String MIN_DOC_COUNT = "min_doc_count";
  private static final String ORDER = "order";

  //Enforce non-instatiability
  private AggregationUtils() {
  }


  /**
   * Validate aggregations
   *
   * @param jsonString aggregations as JSON string
   * @throws IllegalParameterException if string is not JSON array
   * @ expects input to be valid JSON. Throws an exception if the string is not parseable as a JSON
   * array
   **/
  public static void validateAggregations(String jsonString) {
    // rewrite from AI assistant using Jackson
    try {
      JsonNode node = JSON_MAPPER.readTree(jsonString);
      if (!node.isArray()) {
        throw new IllegalParameterException(
            "Aggregations must be valid JSON. Expected JSON Array of objects but found : ["
                + jsonString + "]");
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * The method checks if the facets/aggregations contain a key that has a specified value. Note
   * that the facets must be valid JSON array. For example [ {"field": "status", "size": 15,
   * "operator": "AND", "order": "term_asc"}, {"field": "assigned_to", "order": "term_asc"}]
   *
   * @param aggregations aggregations as JSON string
   * @param field        field in which key-value exists
   * @param key          a facet key
   * @param value        a value of the specified key
   * @return <code>true</code> if the key, in that field, has a specified value
   */
  public static boolean contains(String aggregations, String field, String key, String value) {
    //If there are no aggregations, no need to continue.
    if (!StringUtils.hasText(aggregations)) {
      return false;
    }
    try {
      JsonNode facets = JSON_MAPPER.readTree(aggregations);
      for (JsonNode facet : facets) {
        if (facet.has(FIELD) && facet.has(key)) {
          String currentField = facet.path(FIELD).asText();
          String currentValue = facet.path(key).asText();
          if (currentField.equals(field) && currentValue.equalsIgnoreCase(value)) {
            return true;
          }
        }
      }
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  /**
   * A method to append aggregations to the search request builder.
   *
   * @param searchRequest a search request builder
   * @param aggregations  aggregations as JSON array string
   * @return the same search request where aggregations have been added to it.
   */
  public static SearchRequest.Builder addAggregations(SearchRequest.Builder searchRequest,
      String aggregations) {
    return addAggregations(searchRequest, aggregations, null);
  }

  /**
   * A method to append aggregations to the search request builder.
   *
   * @param searchRequest  a search request builder
   * @param selectedFacets a map that contains selected facets
   * @return the same search request where aggregations have been added to it.
   */
  public static SearchRequest.Builder addAggregations(SearchRequest.Builder searchRequest,
      String aggregations,
      Map<String, List<String>> selectedFacets) {
    JsonNode facets;
    try {
      facets = JSON_MAPPER.readTree(aggregations);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    Map<String, Aggregation> aggregationMap = new HashMap<>();
    BoolQuery.Builder aggsFilter = FilterUtils.getPostFilter(selectedFacets, aggregations);
    for (JsonNode facet : facets) {
      if (facet.has(FIELD)) {
        //Add DateHistogram aggregations
        //@todo add map of Map<String, Aggregation> and send once
        if (facet.has("type") && facet.path("type").asText().equals("date_histogram")) {

          aggregationMap.put(facet.path(FIELD).asText(),
              AggregationUtils.getDateHistogramAggregation(facet).build()._toAggregation());
        } else {
          Aggregation agg;
          ContainerBuilder termsAggs = constructTermsAggregation(facet);
          logger.log(Level.FINE, "key for termsAggs: {0}", facet.path(FIELD).asText());

          if (selectedFacets != null && !selectedFacets.isEmpty()) {
            //Get the current field
            String facetField = facet.path(FIELD).asText();
            // Logic: Whenever a user selects the value from an "OR" aggregation,
            // you add a corresponding filter (here aggs_filter) to all aggregations as subaggregation
            // EXCEPT for the aggregation in which the selection was performed in.
            if (selectedFacets.containsKey(facetField)) {
              //Make a copy of the map
              Map<String, List<String>> selectedFacetCopy = new HashMap<>(
                  selectedFacets);
              //Remove the facet that aggregation was performed in from the map
              selectedFacetCopy.remove(facetField);

              //Build bool_filter for the copy of the selected facets.
              //We build the subaggregation filter only for "OR" facets
              if (aggsFilter.hasClauses()) {
                BoolQuery.Builder aggsFilter2 = FilterUtils.getPostFilter(selectedFacetCopy,
                    aggregations);
                logger.log(Level.FINE, "Aggregations aggsfilter added to search request: {0} ",
                    aggsFilter2);
                agg = addSubAggregationFilter(aggsFilter2);
                termsAggs.aggregations(AGGS_FILTER_KEY, agg);
              }
            }
          }
          aggregationMap.put(facet.path(FIELD).asText(), termsAggs.build());
        }
      }
    }
    logger.log(Level.FINE, "Aggregations added to search request: {0}", aggregationMap);
    return searchRequest.aggregations(aggregationMap);
  }

  /**
   * Adds the subaggregation filter with the name "aggs_filter". Sub aggregations are added only to
   * "OR" facets
   */
  private static Aggregation addSubAggregationFilter(BoolQuery.Builder aggsFilter) {

    return new Aggregation.Builder().filter(aggsFilter.build()._toQuery()).build();
  }

  /**
   * A method to build a date histogram aggregation
   *
   * @param facet a JSON object
   * @return a date histogram builder
   */
  public static DateHistogramAggregation.Builder getDateHistogramAggregation(JsonNode facet) {
    String field = facet.path(FIELD).asText();
    //Create the date histogram
    DateHistogramAggregation.Builder dateHistBuilder = new DateHistogramAggregation.Builder();

    dateHistBuilder.field(field);

    //Set date format
    if (facet.has("format")) {
      logger.fine("datehistogram has format");
      dateHistBuilder.format(facet.path("format").asText());
    }
    //Set interval
    if (facet.has("interval")) {
      logger.fine("datehistogram has interval");
      dateHistBuilder.fixedInterval(
          new Time.Builder().time(facet.path("interval").asText()).build());
    }
    //Set the number of minimum documents that should be returned
    if (facet.has(MIN_DOC_COUNT)) {
      dateHistBuilder.minDocCount(facet.path(MIN_DOC_COUNT).asInt());
    }
    //Set order
    if (facet.has(ORDER)) {
      NamedValue<SortOrder> order;

      if (facet.path(ORDER).asText().equalsIgnoreCase(ORDER_COUNT_ASC)) {
        order = new NamedValue<>(KEY_COUNT, SortOrder.Asc);
      } else if (facet.path(ORDER).asText().equalsIgnoreCase(ORDER_COUNT_DESC)) {
        order = new NamedValue<>(KEY_COUNT, SortOrder.Desc);
      } else if (facet.path(ORDER).asText().equalsIgnoreCase(ORDER_TERM_DESC)) {
        order = new NamedValue<>(KEY_TERM, SortOrder.Desc);
      } else {
        order = new NamedValue<>(KEY_TERM, SortOrder.Asc);
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
  public static ContainerBuilder constructTermsAggregation(JsonNode facet,
      boolean sortBySubAggregation) {
    String field = facet.path(FIELD).asText();

    Aggregation.Builder termsBuilder = new Aggregation.Builder();
    TermsAggregation.Builder termsAggregationBuilder = new TermsAggregation.Builder();
    logger.log(Level.FINE, "field for termsAggs2: {0}", field);

    termsAggregationBuilder.field(field);

    //Set size
    if (facet.has("size")) {
      int size = facet.path("size").asInt();
      termsAggregationBuilder.size(size);
    }
    //Set order

    NamedValue<SortOrder> subAggregationOrder = new NamedValue<>(KEY_COUNT, SortOrder.Asc);
    if (facet.has(ORDER)) {

      NamedValue<SortOrder> order = new NamedValue<>(KEY_COUNT,
          SortOrder.Desc);//default order (count descending)
      if (facet.path(ORDER).asText().equalsIgnoreCase(ORDER_COUNT_ASC)) {
        order = new NamedValue<>(KEY_COUNT, SortOrder.Asc);
      } else if (facet.path(ORDER).asText().equalsIgnoreCase(ORDER_TERM_ASC)) {
        order = new NamedValue<>(KEY_TERM, SortOrder.Asc);
      } else if (facet.path(ORDER).asText().equalsIgnoreCase(ORDER_TERM_DESC)) {
        order = new NamedValue<>(KEY_TERM, SortOrder.Desc);
      }
      if (sortBySubAggregation) {//Sort using sub aggregation
        //@this is also applied below, if order is not specified.
        termsAggregationBuilder.order(subAggregationOrder);
      } else { //sort normally using top aggregation
        termsAggregationBuilder.order(order);
      }
    } else {//if the order is not specified
      if (sortBySubAggregation) { //use sub aggregation order, otherwise use Elasticsearch default
        termsAggregationBuilder.order(subAggregationOrder);
      }
    }
    //Set the number of minimum documents that should be returned
    if (facet.has(MIN_DOC_COUNT)) {
      int minDocCount = facet.path(MIN_DOC_COUNT).asInt();
      termsAggregationBuilder.minDocCount(minDocCount);
    }

    return termsBuilder.terms(termsAggregationBuilder.build());
  }

  /**
   * A method to build terms aggregations and sort options on the parent aggregations
   *
   * @param facet a JSON object
   * @return a term builder
   */
  public static ContainerBuilder constructTermsAggregation(JsonNode facet) {
    return constructTermsAggregation(facet, false);
  }

}
