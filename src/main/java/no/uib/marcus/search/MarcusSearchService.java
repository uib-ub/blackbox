package no.uib.marcus.search;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import no.uib.marcus.search.client.ClientFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryFilterBuilder;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filters.FiltersAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

public class MarcusSearchService implements SearchService {

    /**
     * Match all documents given the facet values
     */
    @Override
    public SearchResponse getAllDocuments(String indexName, String typeName, Map<String, String> facetMap) {
        SearchResponse response = null;
        //TO DO: Build facets/aggregations based on the input map
        //The good architecture would be to pass query and aggregations as params?
        try {
            response = ClientFactory.getTransportClient()
                    .prepareSearch(indexName)
                    .setTypes(typeName)
                    //.addAggregation(AggregationBuilders.terms("by_status").field("status"))
                    //.addFacet(FacetBuilders.termsFacet("by_status").field("status"))
                    .addAggregation(AggregationBuilders.terms("status").field("status"))
                    .addAggregation(AggregationBuilders.terms("assigned_to").field("assigned_to"))
                    .setQuery(QueryBuilders.matchAllQuery())
                    .execute()
                    .actionGet();
        } catch (Exception se) {
            se.getLocalizedMessage();
        }

        return response;
    }

    /**
     * Match all documents
      *
     */
    @Override
    public SearchResponse getAllDocuments(String indexName, String typeName) {
        SearchResponse response = null;
        try {
            response = ClientFactory.getTransportClient()
                    .prepareSearch(indexName)
                    .setTypes(typeName)
                    .setQuery(QueryBuilders.matchAllQuery())
                    .addAggregation(AggregationBuilders.terms("status").field("status"))
                    .addAggregation(AggregationBuilders.terms("assigned_to").field("assigned_to"))
                    .execute()
                    .actionGet();
        } catch (SearchSourceBuilderException se) {
            se.getDetailedMessage();
        }

        return response;
    }

    /**
     * Get All Documents using query string. 
     */
    @Override
    public SearchResponse getAllDocuments(String queryStr, String indexName, String typeName) {
        SearchResponse response = null;
             //See this: http://stackoverflow.com/questions/23807314/multiple-filters-and-an-aggregate-in-elasticsearch
              BoolFilterBuilder fb = (BoolFilterBuilder)FilterBuilders
                .boolFilter()
                //.must(FilterBuilders.boolFilter()
               //.should(FilterBuilders.termFilter("status", "Draft"))
               //  .should(FilterBuilders.termsFilter("assigned_to", "Marianne Paasche", "Morten Heiselberg"))
                 .must(FilterBuilders.termsFilter("status", "Estimate" , "Draft"))
                 
               //.must(FilterBuilders.termFilter("assigned_to", "Marianne Paasche"))
                ;
         
         //FiltersAggregationBuilders agb =  (FiltersAggregationBuilders)AggregationBuilders.filters("filtered").filter("filter", fb );
         
        try {
                response = ClientFactory.getTransportClient()
                    .prepareSearch(indexName)
                    .setTypes(typeName)
                    .setQuery(QueryBuilders.queryStringQuery(queryStr))
                    .addAggregation(AggregationBuilders.terms("status").field("status"))
                    .addAggregation(AggregationBuilders.terms("assigned_to").field("assigned_to"))
                    .execute()
                    .actionGet();
        } catch (SearchSourceBuilderException se) {
            se.getDetailedMessage();
        }
        return response;
    }
    
    
     /**
     * Get All Documents using query string. 
     */
    public SearchResponse getAllDocuments(String queryStr, String indexName, String typeName, FilterBuilder filterBuilder) {
        SearchResponse response = null;
        try {
                response = ClientFactory.getTransportClient()
                    .prepareSearch(indexName)
                    .setTypes(typeName)
                    .setQuery(QueryBuilders.queryStringQuery(queryStr))
                    .setPostFilter(filterBuilder)
                    .addAggregation(AggregationBuilders.terms("status").field("status"))
                    .addAggregation(AggregationBuilders.terms("assigned_to").field("assigned_to"))
                    .execute()
                    .actionGet();
        } catch (SearchSourceBuilderException se) {
            se.getDetailedMessage();
        }

        return response;
    }
    
    
    //Testing Aggregations
    public static void testAggRes() throws IOException{
        //http://stackoverflow.com/questions/23807314/multiple-filters-and-an-aggregate-in-elasticsearch
        BoolFilterBuilder fb = (BoolFilterBuilder)FilterBuilders
                .boolFilter()
                .must(FilterBuilders.termFilter("status", "Sent"))
                .must(FilterBuilders.termFilter("assigned_to", "Marianne Paasche"));
        
        AggregationBuilder aggregation = AggregationBuilders
                                    .filters("agg")
                                    .filter("men", FilterBuilders.termFilter("gender", "male"))
                                    .filter("women", FilterBuilders.termFilter("gender", "female"));
                
                XContentBuilder jsonObj = XContentFactory.jsonBuilder()
                            .startObject()
                            .field("filtered", XContentFactory.jsonBuilder().field("filter",fb))
                            .endObject();
         
                 SearchRequestBuilder req = ClientFactory.getTransportClient()
                .prepareSearch("admin")
                .setTypes("eddie")
                .setQuery(QueryBuilders.filteredQuery(QueryBuilders.queryStringQuery("mari"), fb))
                //.setPostFilter()
                ;
                //.addAggregation(aggregation)
                //.addAggregation(AggregationBuilders.filter("filtered").filter(fb))
                //.addAggregation(AggregationBuilders.terms("status").field("status"))
                //.addAggregation(AggregationBuilders.terms("assigned_to").field("assigned_to"));
                     
        System.out.println(req);
         System.out.println(aggregation);
    }

    //Main method for easy debugging
    public static void main(String[] args) throws IOException {
        Gson gson = new Gson();
        //System.out.println(getAll("admin", null));
        //System.out.println(getAllDocuments("ma", "admin" , "invoice", null));
        //System.out.println("List of suggestion :" + Suggestion.getSuggestionsFor("m", "admin").toString());
        String jsonString = gson.toJson(Suggestion.getSuggestions("m", "admin" , "suggest"));
        //System.out.println("List of suggestion :" + jsonString);
       //System.out.println("List of suggestion :" + Suggestion.getSuggestResponse("m", "admin" , "suggest"));
       testAggRes();
    }

}
