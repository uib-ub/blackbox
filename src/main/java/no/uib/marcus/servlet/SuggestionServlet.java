package no.uib.marcus.servlet;

import com.google.gson.Gson;
import no.uib.marcus.common.Params;
import no.uib.marcus.search.suggestion.CompletionSuggestion;
import org.elasticsearch.common.Strings;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Hemed Ali
 */
@WebServlet(
        name = "SuggestionServlet",
        urlPatterns = {"/suggest"}
)
public class SuggestionServlet extends HttpServlet {

    private static final long serialVersionUID = 2L;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String suggestText = request.getParameter(Params.QUERY_STRING);
        String[] indices = request.getParameterValues(Params.INDICES);
        String size = request.getParameter(Params.SIZE);
        String jsonString;

        try (PrintWriter out = response.getWriter()) {
            int suggestSize = Strings.hasText(size)
                    ? Integer.parseInt(size)
                    : 5;
            Gson gson = new Gson();

            jsonString = gson.toJson(CompletionSuggestion.getSuggestions(suggestText, suggestSize, indices));
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
        return "CompletionSuggestion servlet";
    }

}
