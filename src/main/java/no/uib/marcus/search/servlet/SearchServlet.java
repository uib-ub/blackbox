package no.uib.marcus.search.servlet;

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
import org.elasticsearch.index.query.FilterBuilders;

@WebServlet(name = "SearchServlet", urlPatterns = {"/search"})
public class SearchServlet extends HttpServlet {
private static final ESLogger logger = Loggers.getLogger(SearchServlet.class);

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        String queryString = request.getParameter("q");
        String[] selectedFilters = request.getParameterValues("filter");
        SearchService service = new MarcusSearchService();
        SearchResponse searchResponse;
        BoolFilterBuilder postFilter;
        Map<String,List> filterMap;
        
        try (PrintWriter out = response.getWriter()) 
        {
            if (queryString == null || queryString.isEmpty()) 
            {
                logger.info("Sending match_all query");
                searchResponse = service.getAllDocuments("admin", "invoice");
            }
            else if (selectedFilters == null || selectedFilters.length == 0) 
            {  
               logger.info("No selected filters, sending only query_string");
               searchResponse = service.getAllDocuments(queryString, "admin", "invoice"); 
            } 
            else 
            {   
                filterMap = getFilterMap(selectedFilters);
                postFilter = FilterBuilders.boolFilter();

                for(Map.Entry<String,List> entry : filterMap.entrySet())
                {
                    if(!entry.getValue().isEmpty())
                    {
                       postFilter.must(FilterBuilders.termsFilter(entry.getKey() , entry.getValue()));
                    }
                } 
                //Print what has been sent, only for testing.
                MarcusSearchService.testAggRes(postFilter);
                searchResponse = service.getAllDocuments(queryString, "admin", "invoice", postFilter);
            }
            out.write(searchResponse.toString());
        }
    }
    
    private Map getFilterMap(String[] aggregations) {
        Map<String, List<String>> filters = new HashMap<>();
        try{
            for (String agg : aggregations) 
            {
                if (agg.lastIndexOf(".") != -1) 
                {
                    //Get the index for the last occurence of a dot
                    int lastIndex = agg.lastIndexOf(".");
                    String key = agg.substring(0, lastIndex).trim();
                    String value = agg.substring(lastIndex + 1, agg.length()).trim();
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
