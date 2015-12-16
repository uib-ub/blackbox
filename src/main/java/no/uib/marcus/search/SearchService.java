/*
 Hemed.
 */
package no.uib.marcus.search;

import java.util.Map;
import no.uib.marcus.search.client.ClientFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.mapper.object.ObjectMapper;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;
import org.elasticsearch.search.sort.SortOrder;

public class SearchService {
    
    
    /**
     * Match all documents given the facet values
     **/ 
    public static String getAllDocuments(String indexName, String typeName,  Map<String,String> facetMap){
           SearchResponse response = null;
           //TO DO: Build facets/aggregations based on the input map
        try{
            response = ClientFactory.getTransportClient()
                    .prepareSearch(indexName)
                    .setTypes(typeName)
                    //.addAggregation(AggregationBuilders.terms("by_status").field("status"))
                    //.addFacet(FacetBuilders.termsFacet("by_status").field("status"))
                    .addAggregation(AggregationBuilders.terms("by_status").field("status"))
                    .addHighlightedField("status")
                    .setQuery(QueryBuilders.matchAllQuery())
                    .execute()
                    .actionGet();
        }
         catch(SearchSourceBuilderException se){se.getDetailedMessage();}
        
         return response.toString();
      }
        
     /**
      * Match all documents
      **/
      public static String getAllDocuments(String indexName, String typeName){
           SearchResponse response = null;
            try{
                response = ClientFactory.getTransportClient()
                        .prepareSearch(indexName)
                        .setTypes(typeName)
                        .setQuery(QueryBuilders.matchAllQuery())
                        .execute()
                        .actionGet();
            }
         catch(SearchSourceBuilderException se){se.getDetailedMessage();}
        
         return response.toString();
      }
    
      
         /**
          * Get All Documents using query string. 
          **/
         public static String getAllDocuments(String queryStr, String indexName, String typeName , Map<String,String> aggMap){
           SearchResponse response = null;
           
          try{
            response = ClientFactory.getTransportClient()
                    .prepareSearch(indexName)
                    .setTypes(typeName)
                    .setQuery(QueryBuilders.queryStringQuery(queryStr))
                    .execute()
                    .actionGet();
        }
         catch(SearchSourceBuilderException se){se.getDetailedMessage();}
        
         return response.toString();
      }
         
         
        public static String getSuggestion(String indexName, String typeName){
           SearchResponse response = null;
           //TO DO: Build facets/aggregations based on the input map
        try{
            response = ClientFactory.getTransportClient()
                    .prepareSearch(indexName)
                    .setQuery(QueryBuilders.matchAllQuery())
                    .execute()
                    .actionGet();
        }
         catch(SearchSourceBuilderException se){se.getDetailedMessage();}
        
         return response.toString();
      }
        
     //Method for easy debugging
     public static void main(String [] args){
        //System.out.println(getAll("admin", null));
        System.out.println(getAllDocuments("admin" , "invoice", null));
        //System.out.println("List of suggestion :" + Suggestion.getSuggestionsFor("m", "admin").toString());
        
    }
    
}
