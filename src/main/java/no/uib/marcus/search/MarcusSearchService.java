package no.uib.marcus.search;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import no.uib.marcus.search.client.ClientFactory;
import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

public class MarcusSearchService implements SearchService {

    private static final ESLogger logger = Loggers.getLogger(MarcusSearchService.class);
    private static boolean flag = true;
     
    
    @Override
    public SearchResponse getAllDocuments(){
        SearchResponse response = null;
        try {
            //Prepare search request across all indices
           response = ClientFactory
                    .getTransportClient()
                    .prepareSearch()
                    .execute()
                    .actionGet();

        } catch (SearchSourceBuilderException se) {
            logger.error("Exception on preparing the request: " + se.getDetailedMessage());
        } catch (Exception ex) {
            logger.error("Exception: " + ex.getLocalizedMessage());
        }
        return response;
        
    }
    
    /**
     * Match all documents
     */
    @Override
    public SearchResponse getDocuments(String[] indices, String[] types, String aggs) {
        SearchResponse response = null;
        SearchRequestBuilder searchRequest;
        try {
            //Prepare search request
            searchRequest = ClientFactory
                    .getTransportClient()
                    .prepareSearch();
            //Set indices
            if (indices != null && indices.length > 0) {
                searchRequest.setIndices(indices);
            }
            //Set types
            if (types != null && types.length > 0) {
                searchRequest.setTypes(types);
            }
            //Set query
            searchRequest.setQuery(QueryBuilders.matchAllQuery());
            
            //Append term aggregations to this request builder
            appendTermsAggregation(searchRequest, aggs);
            
            response = searchRequest
                    .execute()
                    .actionGet();

        } catch (SearchSourceBuilderException se) {
            logger.error("Exception while building search request: " + se.getDetailedMessage());
        } catch (Exception ex) {
            logger.error("Exception: " + ex.getLocalizedMessage());
        }
        return response;

    }

    /**
     * Get All Documents using query string. See this:
     * See http://stackoverflow.com/questions/23807314/multiple-filters-and-an-aggregate-in-elasticsearch
     * <b />
     * @param queryStr - a query string 
     * @param indices - one or more indices
     * @param types - one or more types
     * @param aggs - predefined aggregations as a JSON string 
     * @return a SearchResponse
     * 
     */
    @Override
    public SearchResponse getDocuments(String queryStr, String[] indices, String[] types, String aggs) {
        SearchResponse response = null;
        SearchRequestBuilder searchRequest;
        try {
            //Prepare search request
            searchRequest = ClientFactory
                    .getTransportClient()
                    .prepareSearch();
            //Set indices
            if (indices != null && indices.length > 0) {
                searchRequest.setIndices(indices);
            }
            //Set types
            if (types != null && types.length > 0) {
                searchRequest.setTypes(types);
            }
            //Set query
            searchRequest.setQuery(QueryBuilders
                    .queryStringQuery(queryStr));
            
            //Append term aggregations to this request builder
            appendTermsAggregation(searchRequest, aggs);

            response = searchRequest
                    .execute()
                    .actionGet();

        } catch (SearchSourceBuilderException se) {
            logger.error("Exception on preparing the request: " + se.getDetailedMessage());
        } catch (Exception ex) {
            logger.error("Exception: " + ex.getLocalizedMessage());
        }
        return response;
    }

    /**
     * Get All Documents using query string.
     */
    @Override
    public SearchResponse getDocuments(String queryStr, String[] indices, String[] types, FilterBuilder filter, String aggs) {
        SearchResponse response = null;
        SearchRequestBuilder searchRequest;
        BoolFilterBuilder boolFilter = (BoolFilterBuilder)filter;
        try {
                //Prepare search request
                searchRequest = ClientFactory
                        .getTransportClient()
                        .prepareSearch();

                //Set indices
                if (indices != null && indices.length > 0) {
                    searchRequest.setIndices(indices);
                }
                //Set types
                if (types != null && types.length > 0) {
                    searchRequest.setTypes(types);
                }
                
                searchRequest
                        .setQuery(QueryBuilders
                                .filteredQuery(QueryBuilders
                                        .queryStringQuery(queryStr), boolFilter));
                
               /** 
                //Post filter
                searchRequest.setQuery(QueryBuilders.queryStringQuery(queryStr));
                if (boolFilter.hasClauses()) {
                    searchRequest
                            .setPostFilter(boolFilter);
                }
                * **/
                //Append term aggregations to this request builder
                appendTermsAggregation(searchRequest, aggs);
                 
                //Show Search builder for debugging purpose
               logger.info(searchRequest.toString());
               
                response = searchRequest
                        .execute()
                        .actionGet();
        } catch (SearchSourceBuilderException se) {
            logger.error("Exception on preparing the request: "
                    + se.getDetailedMessage());
        } catch (Exception ex) {
            logger.error("Exception: "
                    + ex.getLocalizedMessage());
        }
        return response;
    }

    /**
     * A method to append terms aggregations to the search request builder.
     **/
    private SearchRequestBuilder appendTermsAggregation(SearchRequestBuilder searchRequest, String aggregations) 
            throws Exception {
        JsonElement jsonElement = new JsonParser().parse(aggregations);
        for (JsonElement facets : jsonElement.getAsJsonArray()) {
            JsonObject currentFacet = facets.getAsJsonObject();
            if (currentFacet.has("field")) {
                String field = currentFacet.get("field").getAsString();
                    
                //Default size
                int size = 10;
                //Default order: count descending
                Order order = Order.count(false);

                //Set size
                if (currentFacet.has("size")) {
                    size = currentFacet.get("size").getAsInt();
                }
                
                //Set order
                if(currentFacet.has("order")){
                      if(currentFacet.get("order").getAsString().equalsIgnoreCase("count_asc")){
                          order = Order.count(true);
                       }
                      else if(currentFacet.get("order").getAsString().equalsIgnoreCase("term_asc")){
                          order = Order.term(true);
                       }
                       else if(currentFacet.get("order").getAsString().equalsIgnoreCase("term_desc")){
                              order = Order.term(false);
                       }
                }
                //Add aggregations to the search request builder
                searchRequest.addAggregation(AggregationBuilders
                        .terms(field)
                        .field(field)
                        .size(size)
                        .order(order)
                        .minDocCount(0));
            }

        }

        return searchRequest;
    }

    //Testing Aggregations
    public static void testAggRes(BoolFilterBuilder fb1) throws IOException, Exception {
        //http://stackoverflow.com/questions/23807314/multiple-filters-and-an-aggregate-in-elasticsearch
        Map map = new HashMap();
        String json = "[{\"field\": \"status\", \"size\": 25},{\"field\" : \"assigned_to\"}]";
        JsonElement el = new JsonParser().parse(json);

        map.put("status", "go_to_gate");
        BoolFilterBuilder fb = (BoolFilterBuilder) FilterBuilders
                .boolFilter()
                //.must(FilterBuilders.termFilter("status", "Draft"))
                .should(FilterBuilders
                        .rangeFilter("created")
                        .from("1920")
                        .to(null)
                        .includeLower(true)
                        .includeUpper(true))
                
                 .should(FilterBuilders
                        .rangeFilter("madeafter")
                        .from("1920-01-01")
                        .to(null))
                
                 .should(FilterBuilders
                        .rangeFilter("madebefore")
                        .from(null)
                        .to("1920-01-01"));
        
                //.must(FilterBuilders.termFilter("assigned_to", "Marianne Paasche"));

        AggregationBuilder aggregation = AggregationBuilders
                .filters("agg")
                .filter("men", FilterBuilders.termFilter("gender", "male"))
                .filter("women", FilterBuilders.termFilter("gender", "female"));

        XContentBuilder jsonObj = XContentFactory.jsonBuilder()
                .startObject()
                .field("filtered", XContentFactory.jsonBuilder().field("filter", fb))
                .endObject();

        SearchRequestBuilder req = ClientFactory.getTransportClient()
                .prepareSearch()
                .setIndices("admin")
                //.setTypes("invoice")
                //.setQuery(QueryBuilders.matchAllQuery()) 
                .setQuery(QueryBuilders.filteredQuery(QueryBuilders.queryStringQuery("*"), fb))
                //.setPostFilter(fb1)
               // .addAggregation(AggregationBuilders.global("_aggs")
                .addAggregation((AggregationBuilders
                   .terms("status")
                   .field("status")))
                   //.subAggregation(AggregationBuilders.terms("by_assignee").field("assigned_to"))))               
                 .addAggregation(AggregationBuilders
                         .dateRange("created")
                         .field("created")
                         .addRange("created" , "2010", null)   
                 );
                 /**.addAggregation(AggregationBuilders
                   .terms("assigned_to")
                   .field("assigned_to") 
                   .subAggregation(AggregationBuilders.terms("by_status").field("status"))
                 );**/
        
        CountRequestBuilder cr = ClientFactory
                .getTransportClient()
                .prepareCount()
                .setQuery(QueryBuilders.termQuery("status", "Sent"));
        
        SearchResponse res = req.execute().actionGet();
        
        JsonElement responseJson = new JsonParser().parse(res.toString());
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonObject aggs = responseJson.getAsJsonObject().get("aggregations").getAsJsonObject();
        //Iterate throgh all the terms
        for (Map.Entry<String, JsonElement> entry : aggs.entrySet()) {
            //Get the term's key
            String field = entry.getKey();
            //Get the value
            JsonObject terms = entry.getValue().getAsJsonObject();
            //Iterate throgh the array of bucket of this term.
            for (JsonElement element : terms.getAsJsonArray("buckets")) {
                JsonObject bucket = element.getAsJsonObject();
                String value = bucket.get("key").getAsString();

                //Query Elasticsearch independently
                CountResponse countResponse = ClientFactory
                        .getTransportClient()
                        .prepareCount()
                        .setQuery(QueryBuilders.termQuery(field, value))
                        .execute()
                        .actionGet();

                bucket.addProperty("total_doc_count", countResponse.getCount());
            }

        }
 
        // appendTermsAggregation(req, json);
        //.addAggregation(aggregation)
        //.addAggregation(AggregationBuilders.filter("filtered").filter(fb))
        //.addAggregation(AggregationBuilders.terms("status").field("status"))
        //.addAggregation(AggregationBuilders.terms("assigned_to").field("assigned_to"));
        //System.out.println("JSON Response: " +  gson.toJson(responseJson));
     
        System.out.println("Request: " + req.toString());
        System.out.println("Response: " + gson.toJson(responseJson));
    }


    //Main method for easy debugging
    public static void main(String[] args) throws IOException, Exception {
         
        String s = "[{\"field\": \"status\", \"size\": 15, \"operator\" : \"AND\", \"order\": \"term_asc\"}]";
        // System.out.println("Map baby: " + facetMap.toString());
        //System.out.println(getAll("admin", null));
        //System.out.println(getDocuments("ma", "admin" , "invoice", null));
        //System.out.println("List of suggestion :" + Suggestion.getSuggestionsFor("m", "admin").toString());
        // String jsonString = gson.toJson(Suggestion.getSuggestions("m", "admin" , "suggest"));
        //System.out.println("List of suggestion :" + jsonString);
        //System.out.println("List of suggestion :" + Suggestion.getSuggestResponse("m", "admin" , "suggest"));
        testAggRes(null);
        //System.out.println(hasANDOperator("status", s));
    }

}
