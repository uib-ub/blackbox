package no.uib.marcus.search.servlet;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.elasticsearch.index.mapper.object.ObjectMapper;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

@WebServlet(name = "SearchServlet", urlPatterns = {"/search"})
public class SearchServlet extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        String queryString = request.getParameter("q");
        String statusAgg = request.getParameter("status");
        String selectedAggregations = request.getParameter("selected_aggs");
        SearchService service = new MarcusSearchService();
        SearchResponse searchResponse;
        BoolFilterBuilder fb = null;
        
        System.out.println("Parameter Aggs: " + Arrays.toString(request.getParameterValues("id")));
        String aggs = request.getParameter("aggs");
        Gson gson = new Gson();
        Map<String,String[]> aggMap = null;
        
        //Only for testing.
        if(aggs.length() > 4){
        //Convert JSON String to Map
            aggMap= gson.fromJson(aggs, new TypeToken<HashMap<String,String[]>>(){}.getType());
          }
        
        
        //System.out.print("Map from JSON: " + Arrays.toString(aggMap.get("status")));
                
        try (PrintWriter out = response.getWriter()) {
            
            

            if (queryString == null || queryString.isEmpty() || aggs.length() > 4) {
                //Match all
                searchResponse = service.getAllDocuments("admin", "invoice");
            }
            else {
                    if (aggs== null || aggs.isEmpty() || aggMap.get("assignee").length == 0) 
                    {  
                       System.out.println("NUll and Empty: " + statusAgg );
                       searchResponse = service.getAllDocuments(queryString, "admin", "invoice"); 
                    } 
                 else {   
                           if (!aggMap.isEmpty()){
                               
                               //FilterBuilder fb = buildBoolORFilter(aggMap);
                               fb = FilterBuilders.boolFilter();
                               //OR Query
                               for(Map.Entry<String,String[]> entry : aggMap.entrySet()){
                                    if(entry.getValue().length > 0){
                                      fb.must(FilterBuilders.termsFilter(entry.getKey() , entry.getValue()));
                                    //System.out.println("Key: " + entry.getKey() +"Value: " + Arrays.toString(entry.getValue()));
                                    }
                               }
                               
                               MarcusSearchService.testAggRes(fb);
                           }
                           
                            /**if (statusAgg.contains(",")) 
                            {
                                String[] values = statusAgg.split(",");
                                fb = FilterBuilders
                                        .boolFilter()
                                        .must(FilterBuilders.termsFilter("status", values));
                            } else 
                            {
                                fb = FilterBuilders
                                        .boolFilter()
                                        .must(FilterBuilders.termsFilter("status", statusAgg));
                            }**/
                            searchResponse = service.getAllDocuments(queryString, "admin", "invoice", fb);
                           
                      }

            }
            out.write(searchResponse.toString());
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Search servlet";
    }// </editor-fold>

}
