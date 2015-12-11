/*
 Hemed.
 */
package no.uib.marcus.search;

import java.util.Map;
import no.uib.marcus.search.connection.ConnectionFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

public class SearchService {
    
    
    public static String getAll(String indexName, Map<String,String> facetMap){
           SearchResponse response = null;
           //TO DO: Build facets/aggregations based on the input map
        try{

            response =  ConnectionFactory.getTransportClient().prepareSearch(indexName)
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
    
    
        public static String getAll(String indexName){
           SearchResponse response = null;
            try{

                response = ConnectionFactory.getTransportClient().prepareSearch(indexName)
                        .setQuery(QueryBuilders.matchAllQuery())
                        .execute()
                        .actionGet();
            }
         catch(SearchSourceBuilderException se){se.getDetailedMessage();}
        
         return response.toString();
      }
    
    
         public static String getAllFromSearchString(String queryStr, String indexName, Map<String,String> aggMap){
           SearchResponse response = null;
           //I'm thinking a facet map is a key-value pair of the facets/aggregations
          try{

            response =  ConnectionFactory.getTransportClient().prepareSearch(indexName)
                    
                    .setQuery(QueryBuilders.queryStringQuery(queryStr))
                    .execute()
                    .actionGet();
        }
         catch(SearchSourceBuilderException se){se.getDetailedMessage();}
        
         return response.toString();
      }
        
    
    //Method for easy debugging
    public static void main(String [] args){
        //System.out.println(getAll("admin", null));
        System.out.println(getAllFromSearchString("Marianne", "admin" , null));
        
    }
    
}
