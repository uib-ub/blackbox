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

/**
 * @author Hemed Ali
 */
@WebServlet(
        name = "SuggestionServlet",
        urlPatterns = {"/suggest"}
)
public class SuggestionServlet extends HttpServlet {

        private static final long serialVersionUID = 2L;
        private static final String SUGGEST_FIELD = "suggest";

        protected void processRequest(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {

                request.setCharacterEncoding("UTF-8");
                response.setContentType("application/json;charset=UTF-8");
                String suggestText = request.getParameter("q");
                String[] indices = request.getParameterValues("index");
                String jsonString;

                try (PrintWriter out = response.getWriter()) {
                        Gson gson = new Gson();
                        jsonString = gson.toJson(
                                Suggestion
                                        .getSuggestions(suggestText, SUGGEST_FIELD, indices));
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