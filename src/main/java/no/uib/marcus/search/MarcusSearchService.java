package no.uib.marcus.search;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Order;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

public class MarcusSearchService implements SearchService {
   static final ESLogger logger = Loggers.getLogger(MarcusSearchService.class);
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
                    .addAggregation(AggregationBuilders
                         .terms("status")
                         .field("status")
                         .minDocCount(0))
                    .addAggregation(AggregationBuilders
                         .terms("assigned_to")
                         .field("assigned_to")
                         .minDocCount(0))
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
                    .addAggregation(AggregationBuilders
                            .terms("status")
                            .field("status")
                            .minDocCount(0))
                    .addAggregation(AggregationBuilders
                            .terms("assigned_to")
                            .field("assigned_to")
                            .minDocCount(0))
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
             SearchRequestBuilder searchRequest = ClientFactory.getTransportClient()
                     .prepareSearch(indexName);
                     
                     searchRequest
                             .setTypes(typeName)
                             .setQuery(QueryBuilders.queryStringQuery(queryStr))
                             .setPostFilter(filterBuilder)
                             //Here check with aggregations as well
                             .addAggregation(AggregationBuilders
                                     .terms("status")
                                     .field("status")
                                     //.order(Order.term(true))
                                     .minDocCount(0))
                           
                             .addAggregation(AggregationBuilders
                                     .terms("assigned_to")
                                     .field("assigned_to")
                                     //.order(Order.term(true))
                                     .minDocCount(0));
                     
                     if(filterBuilder != null){
                       searchRequest.setPostFilter(filterBuilder);
                     }
 
                response = searchRequest
                    .execute()
                    .actionGet();
                
        } catch (SearchSourceBuilderException se) {
            se.getDetailedMessage();
        }

        return response;
    }
    
    
    //Testing Aggregations
    public static void testAggRes(BoolFilterBuilder fb1) throws IOException{
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
                .setQuery(QueryBuilders.queryStringQuery("*"))
                //.setQuery(QueryBuilders.filteredQuery(QueryBuilders.queryStringQuery("*"))
                .setPostFilter(fb1)
                ;
                //.addAggregation(aggregation)
                //.addAggregation(AggregationBuilders.filter("filtered").filter(fb))
                //.addAggregation(AggregationBuilders.terms("status").field("status"))
                //.addAggregation(AggregationBuilders.terms("assigned_to").field("assigned_to"));
                     
         System.out.println("Request: " + req);
         //System.out.println(aggregation);
    }

    //Main method for easy debugging
    public static void main(String[] args) throws IOException {
        Gson gson = new Gson();

        //Configure this!
        String s = "status.Sent,status.Draft,assignee.Oyvind, eddie.Diddei ,  http://www.w3.org/1999/02/22-rdf-syntax-ns#type.exact.Foto, stat.tot.to";
        String[] facets = s.split(",");
        
        String test = facets[0].substring(facets[0].lastIndexOf("."), facets[0].length());
        
        Map<String, List<String>> facetMap = new HashMap<>();
        //Set keys = new HashSet();
        logger.info(Arrays.toString(facets));
        for (String facet : facets ) 
        {   
            if(facet.lastIndexOf(".") != -1)
            {   
                int lastIndex = facet.lastIndexOf(".");
                String key = facet.substring(0, lastIndex).trim(); 
                String value =  facet.substring(lastIndex+1, facet.length()).trim();

                 logger.info("Key: "+ key + " Value: " + value);

                if (!facetMap.containsKey(key)) 
                {
                    List<String> valuesList = new ArrayList<>();
                    valuesList.add(value);
                    facetMap.put(key, valuesList);
                } 
                else 
                {
                  facetMap.get(key).add(value);
                }
            }
        }

        /**for (String s4 : s1)
        {
         keys.add(s4.replaceAll("^([^\\.]+)\\..+$", "$1").trim());
        
        }
        for(Iterator iter = keys.iterator(); iter.hasNext();){
          
            String s6 = (String)iter.next();
          String key = s6.replaceAll("^([^\\.]+)\\..+$", "$1");
          
          List<String> values = new ArrayList();
          for ( String s3 : s1) {
              if (s3.replaceAll("^([^\\.]+)\\..+$","$1").trim().equals(key)){
               values.add(s3.replaceAll("^[^\\.]+\\.(.+)$","$1"));
                map.put(key,values);       
              }
          }
        /**for (String s4 : s1)
        {
         keys.add(s4.replaceAll("^([^\\.]+)\\..+$", "$1").trim());
        
        }
        for(Iterator iter = keys.iterator(); iter.hasNext();){
          
            String s6 = (String)iter.next();
          String key = s6.replaceAll("^([^\\.]+)\\..+$", "$1");
          
          List<String> values = new ArrayList();
          for ( String s3 : s1) {
              if (s3.replaceAll("^([^\\.]+)\\..+$","$1").trim().equals(key)){
               values.add(s3.replaceAll("^[^\\.]+\\.(.+)$","$1"));
                map.put(key,values);       
              }
          }
        }**/
        
        
        
        System.out.println("Map baby: " + facetMap.toString());
         
      
        //System.out.println(getAll("admin", null));
        //System.out.println(getAllDocuments("ma", "admin" , "invoice", null));
        //System.out.println("List of suggestion :" + Suggestion.getSuggestionsFor("m", "admin").toString());
        String jsonString = gson.toJson(Suggestion.getSuggestions("m", "admin" , "suggest"));
        //System.out.println("List of suggestion :" + jsonString);
       //System.out.println("List of suggestion :" + Suggestion.getSuggestResponse("m", "admin" , "suggest"));
       //testAggRes();
    }

}
