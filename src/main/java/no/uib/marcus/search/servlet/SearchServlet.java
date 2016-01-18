package no.uib.marcus.search.servlet;

import com.google.gson.Gson;
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
import no.uib.marcus.search.SearchService;
import no.uib.marcus.search.MarcusSearchService;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

@WebServlet(
        name = "SearchServlet", 
        urlPatterns = {"/search"}
)
public class SearchServlet extends HttpServlet {
private static final ESLogger logger = Loggers.getLogger(SearchServlet.class);

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
        Gson gson = new Gson();
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
               searchResponse = service.getDocuments(queryString, indices, types, aggs); 
            } 
            else 
            {   
                filterMap = getFilterMap(selectedFilters);
                boolFilter = FilterBuilders.boolFilter();
                FilterBuilder b = null;

                for(Map.Entry<String,List> entry : filterMap.entrySet())
                {
                    if(!entry.getValue().isEmpty())
                    {
                       boolFilter.must(FilterBuilders.termsFilter(entry.getKey(),entry.getValue()));
                       //b = FilterBuilders.boolFilter().should(FilterBuilders.termsFilter(entry.getKey() , entry.getValue()));
                    }
                } 
                searchResponse = service.getDocuments(queryString, indices, types, boolFilter, aggs);
            }
            out.write(searchResponse.toString());
        }
    }
    
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
            logger.error("Exception occured on processing selected aggregations: " + ex.getLocalizedMessage());
        }
        return filters;
    }
        
        //Build AND filter
        private BoolFilterBuilder getAND (String[] selectedFilters) {
        BoolFilterBuilder andFilter = FilterBuilders.boolFilter();
        try{
            for (String entry : selectedFilters) 
            {
                if (entry.lastIndexOf(".") != -1) 
                {
                    //Get the index for the last occurence of a dot
                    int lastIndex = entry.lastIndexOf(".");
                    String key = entry.substring(0, lastIndex).trim();
                    String value = entry.substring(lastIndex + 1, entry.length()).trim();
                    andFilter.must(FilterBuilders.termFilter(key, value));
                    
                }
            }
        }
        catch(Exception ex)
        {
            logger.error("Exception occured on processing selected aggregations: " + ex.getLocalizedMessage());
        }
        return andFilter;
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
