/*
 Hemed.
 */
package no.uib.marcus.search;

import java.util.Map;
import no.uib.marcus.search.client.ClientFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

public class SearchService {
     
    public static String getAllDocuments(String indexName, String typeName,  Map<String,String> facetMap){
           SearchResponse response = null;
           //TO DO: Build facets/aggregations based on the input map
        try{
            response = ClientFactory.getTransportClient()
                    .prepareSearch(indexName)
                    //.setTypes(typeName)
                    //.addAggregation(AggregationBuilders.terms("by_status").field("status"))
                    //.addFacet(FacetBuilders.termsFacet("by_status").field("status"))
                    .addAggregation(AggregationBuilders.terms("by_status").field("status"))
                    .setQuery(QueryBuilders.matchAllQuery())
                    .execute()
                    .actionGet();
        }
         catch(SearchSourceBuilderException se){se.getDetailedMessage();}
        
         return response.toString();
      }
        
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
    
         public static String getAllDocuments(String queryStr, String indexName, String typeName , Map<String,String> aggMap){
           SearchResponse response = null;
           
          try{
            response = ClientFactory.getTransportClient()
                    .prepareSearch(indexName)
                    .setTypes(typeName)
                    .setQuery(QueryBuilders.queryString(queryStr))
                    .execute()
                    .actionGet();
        }
         catch(SearchSourceBuilderException se){se.getDetailedMessage();}
        
         return response.toString();
      }
        
     //Method for easy debugging
     public static void main(String [] args){
        //System.out.println(getAll("admin", null));
        System.out.println(getAllDocuments("Marianne", "admin" , "invoice", null));
        
    }
    
}
