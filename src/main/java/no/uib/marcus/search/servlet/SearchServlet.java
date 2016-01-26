package no.uib.marcus.search.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;

import no.uib.marcus.search.SearchService;
import no.uib.marcus.search.MarcusSearchService;
import no.uib.marcus.search.client.ClientFactory;

/**
 * @author Hemed Al Ruwehy (hemed.ruwehy@uib.no)
 * 2016-01-24, University of Bergen Library
 **/

@WebServlet(
        name = "SearchServlet", 
        urlPatterns = {"/search"}
)
public class SearchServlet extends HttpServlet {
private static final Logger logger = Logger.getLogger(SearchServlet.class);

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("text/html;charset=UTF-8");
        String queryString = request.getParameter("q");
        String[] selectedFilters = request.getParameterValues("filter");
        String aggs = request.getParameter("aggs");
        String[] indices = request.getParameterValues("index");
        String[] types = request.getParameterValues("type");
        SearchService service = new MarcusSearchService();
        SearchResponse searchResponse;
        BoolFilterBuilder boolFilter;
        Map<String,List> filterMap;
        
        try (PrintWriter out = response.getWriter()) 
        {   
            if (queryString == null || queryString.isEmpty()) 
            {
                logger.info("Sending match_all query");
                searchResponse = service.getDocuments(indices, types, aggs);
            }
            else if (selectedFilters == null || selectedFilters.length == 0) 
            {  
               logger.info("No selected filters, sending only query_string");
               logger.info("Index: " + indices + " types " + types);
               searchResponse = service.getDocuments(queryString, indices, types, aggs); 
            } 
            else 
            {   
                boolFilter = (BoolFilterBuilder)getBoolFilter(selectedFilters, aggs);
                
                if(boolFilter.hasClauses())
                {
                  searchResponse = service.getDocuments(queryString, indices, types, boolFilter, aggs);
                }
                else
                {
                  searchResponse = service.getDocuments(queryString, indices, types, aggs);   
                }
            }
            //After getting the response, add extra field to every bucket in the aggregations
            String  responseJson = processAggregations(searchResponse, indices, types);
            out.write(responseJson);
        }
    }
    
    /**
     * A method to get a map based on the selected filters.
     **/
    private Map getFilterMap(String[] selectedFilters) {
        Map<String, List<String>> filters = new HashMap<>();
        try{
            for (String entry : selectedFilters) 
            {
                if (entry.lastIndexOf(".") != -1) 
                {
                    //Get the index for the last occurence of a dot
                    int lastIndex = entry.lastIndexOf(".");
                    String key = entry.substring(0, lastIndex).trim();
                    String value = entry.substring(lastIndex + 1, entry.length()).trim();
                    //Should we allow empty values? maybe :) 
                    if (!filters.containsKey(key)) 
                    {
                        List<String> valuesList = new ArrayList<>();
                        valuesList.add(value);
                        filters.put(key, valuesList);
                    }  
                   else 
                    {
                        filters.get(key).add(value);
                    }
                }
            }
        }
        catch(Exception ex)
        {
            logger.error("Exception occured while constructing a map from selected filters: " + ex.getMessage());
        }
        return filters;
    }
        
        /**
         * A method for building BoolFilter based on the aggregation settings.
        **/
        private FilterBuilder getBoolFilter (String[] selectedFilters , String aggregations) {
        Map<String,List> filterMap = getFilterMap(selectedFilters);
        BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();
        try{
            for(Map.Entry<String,List> entry : filterMap.entrySet())
            {
                if(!entry.getValue().isEmpty())
                {    
                   if(hasAND(entry.getKey(), aggregations))
                   {
                      logger.info("Constructing AND query for facet: "  + entry.getKey());
                      for(Object value : entry.getValue())//Perform AND
                      {  
                          //A filter based on a term. If it is inside a must, it acts as "AND" filter
                          boolFilter.must(FilterBuilders.termFilter(entry.getKey(), value));
                      }
                   }
                   else//Perform OR filter
                   {
                    //Using "terms" filter based on several terms, matching on any of them
                    //This acts as OR filter, when it is inside the must clause.
                    boolFilter.must(FilterBuilders.termsFilter(entry.getKey(), entry.getValue()));
                   }
                }
            } 
        }
        catch(Exception ex)
        {
            logger.error("Exception occured on constructing OR filter" + ex.getLocalizedMessage());
        }
        return boolFilter;
    }
       
        /**
         *  The method checks if the facets/aggregations contain AND operator.
         *  If not specified, OR operator is used as a default.
         * <b>
         *  Note that the facets must be valid JSON array. For example 
         *  [ {"field": "status", "size": 15, "operator" : "AND", "order": "term_asc"}, 
         *  {"field" : "assigned_to" , "order" : "term_asc"}]
         **/       
        private boolean hasAND(String field, String aggregations){
           try{
                JsonElement facets = new JsonParser().parse(aggregations);
                for (JsonElement e : facets.getAsJsonArray()) 
                {
                    JsonObject facet = e.getAsJsonObject();
                    if(facet.has("field") && facet.has("operator"))
                    {   
                        String currentField = facet.get("field").getAsString();
                        String operator = facet.get("operator").getAsString();
                        if(currentField.equals(field) && operator.equalsIgnoreCase("AND"))
                        {
                          return true;
                        }
                    }
                }
           }
            catch(Exception e){
                logger.error("Facets could not be processed. "
                             + "Please check the syntax. "
                             + "It should be a valid JSON array: " + aggregations);
                return false;
           }
           return false;
        }
        
        /**
         * A method to add extra field to every bucket in the terms aggregations.
         * The extra field is queried independently from the Elasticsearch.
         * <b>
         * This method is in experimentation phase and maybe removed in the future releases.
         **/
        private String processAggregations(SearchResponse response, String [] indices, String... types){
            JsonElement responseJson = new JsonParser().parse(response.toString());
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            
            JsonObject aggs = responseJson.getAsJsonObject().get("aggregations").getAsJsonObject();
            //Iterate throgh all the terms
            for (Map.Entry<String, JsonElement> entry : aggs.entrySet()) 
            {
                //Get term 
                String term = entry.getKey();
                //Get value
                JsonObject terms = entry.getValue().getAsJsonObject();
                //Iterate throgh array of buckets of this term.
                for (JsonElement element : terms.getAsJsonArray("buckets")) 
                {
                    JsonObject bucket = element.getAsJsonObject();
                    String value = bucket.get("key").getAsString();

                    //Query Elasticsearch independently for count of the specified term.
                    CountResponse countResponse = ClientFactory
                            .getTransportClient()
                            .prepareCount()
                            .setIndices(indices)
                            .setTypes(types)
                            .setQuery(QueryBuilders.termQuery(term, value))
                            .execute()
                            .actionGet();
                    bucket.addProperty("total_doc_count", countResponse.getCount());
                }

            }
           return  gson.toJson(responseJson);
        }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    public String getServletInfo() {
        return "Search servlet";
    }

}
