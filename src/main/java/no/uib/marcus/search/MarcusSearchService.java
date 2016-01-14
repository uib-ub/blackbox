package no.uib.marcus.search;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import no.uib.marcus.search.client.ClientFactory;
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
import org.elasticsearch.search.builder.SearchSourceBuilderException;

public class MarcusSearchService implements SearchService {

    static final ESLogger logger = Loggers.getLogger(MarcusSearchService.class);
     
    
    @Override
    public SearchResponse getAllDocuments(){
        SearchResponse response = null;
        try {
            //Prepare search request across all indices
           response = ClientFactory
                    .getTransportClient()
                    .prepareSearch("_all")
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
                    .prepareSearch(indices);
            //Set types
            if (types.length > 0) {
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
            logger.error("Exception on preparing the request: " + se.getDetailedMessage());
        } catch (Exception ex) {
            logger.error("Exception: " + ex.getLocalizedMessage());
        }
        return response;

    }

    /**
     * Get All Documents using query string. See this:
     * See http://stackoverflow.com/questions/23807314/multiple-filters-and-an-aggregate-in-elasticsearch
     */
    @Override
    public SearchResponse getDocuments(String queryStr, String[] indices, String[] types, String aggs) {
        SearchResponse response = null;
        SearchRequestBuilder searchRequest;
        try {
            //Prepare search request
            searchRequest = ClientFactory
                    .getTransportClient()
                    .prepareSearch(indices);
            //Set types
            if (types.length > 0) {
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
    public SearchResponse getDocuments(String queryStr, String[] indices, String[] types, FilterBuilder postFilter, String aggs) {
        SearchResponse response = null;
        SearchRequestBuilder searchRequest;
        BoolFilterBuilder boolPostFilter = (BoolFilterBuilder)postFilter;
        try {
                //Prepare search request
                searchRequest = ClientFactory
                        .getTransportClient()
                        .prepareSearch(indices);

                if (types.length > 0) {
                    searchRequest.setTypes(types);
                }

                searchRequest
                        .setQuery(QueryBuilders
                                .queryStringQuery(queryStr));
                
                if (boolPostFilter.hasClauses()) {
                    searchRequest
                            .setPostFilter(boolPostFilter);
                }
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

    private SearchRequestBuilder appendTermsAggregation(SearchRequestBuilder req, String json) throws Exception {
        JsonElement el = new JsonParser().parse(json);
        for (JsonElement je : el.getAsJsonArray()) {
            JsonObject o = je.getAsJsonObject();

            if (o.has("field")) {
                int size = 10;
                String field = o.get("field").getAsString();

                if (o.has("size")) {
                    size = o.get("size").getAsInt();
                }
                req.addAggregation(AggregationBuilders
                        .terms(field)
                        .field(field)
                        .size(size)
                        //.order(Order.term(true))
                        .minDocCount(0));
            }

        }

        return req;
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
                .must(FilterBuilders.termFilter("status", "Sent"))
                .must(FilterBuilders.termFilter("assigned_to", "Marianne Paasche"));

        AggregationBuilder aggregation = AggregationBuilders
                .filters("agg")
                .filter("men", FilterBuilders.termFilter("gender", "male"))
                .filter("women", FilterBuilders.termFilter("gender", "female"));

        XContentBuilder jsonObj = XContentFactory.jsonBuilder()
                .startObject()
                .field("filtered", XContentFactory.jsonBuilder().field("filter", fb))
                .endObject();

        SearchRequestBuilder req = ClientFactory.getTransportClient()
                .prepareSearch("_all")
                //.setTypes("eddie")
                .setQuery(QueryBuilders.matchAllQuery()) //.setQuery(QueryBuilders.filteredQuery(QueryBuilders.queryStringQuery("*"))
                //.setPostFilter(fb1)
                ;

        // appendTermsAggregation(req, json);
        //.addAggregation(aggregation)
        //.addAggregation(AggregationBuilders.filter("filtered").filter(fb))
        //.addAggregation(AggregationBuilders.terms("status").field("status"))
        //.addAggregation(AggregationBuilders.terms("assigned_to").field("assigned_to"));
        System.out.println("Request: " + req.execute().actionGet());
        //System.out.println(aggregation);
    }

    //Main method for easy debugging
    public static void main(String[] args) throws IOException, Exception {

      
        // System.out.println("Map baby: " + facetMap.toString());
        //System.out.println(getAll("admin", null));
        //System.out.println(getDocuments("ma", "admin" , "invoice", null));
        //System.out.println("List of suggestion :" + Suggestion.getSuggestionsFor("m", "admin").toString());
        // String jsonString = gson.toJson(Suggestion.getSuggestions("m", "admin" , "suggest"));
        //System.out.println("List of suggestion :" + jsonString);
        //System.out.println("List of suggestion :" + Suggestion.getSuggestResponse("m", "admin" , "suggest"));
        testAggRes(null);
    }

}
