
package no.uib.marcus.search.servlet;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.uib.marcus.search.Suggestion;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

/**
 *
 * @author Hemed Ali
 */
@WebServlet(name = "SuggestionServlet", urlPatterns = {"/suggest"})
public class SuggestServlet extends HttpServlet {
      private static final long serialVersionUID = 2L;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8"); 
        response.setContentType("application/json;charset=UTF-8");
        String queryString = request.getParameter("q");   
        
        try (PrintWriter out = response.getWriter()) {
            Gson gson = new Gson();
            
            /**XContentBuilder jsonObj = XContentFactory
                    .jsonBuilder()
                    .startObject()
                    .field("suggest_list", Suggestion.getSuggestions(queryString, "admin" , "suggest"))
                    .endObject();
            **/
           
              String jsonString = gson.toJson(Suggestion.getSuggestions(queryString, "admin" , "suggest"));
              out.write(jsonString);
        }
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
        return "Suggestion servlet";
    }

}
