package no.uib.marcus.servlet;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.uib.marcus.common.Params;
import no.uib.marcus.common.util.StringUtils;
import no.uib.marcus.search.suggestion.CompletionSuggestion;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This servlet processes all HTTP requests coming from the "/suggest" endpoint
 *
 * @author Hemed Ali
 */
@WebServlet(
        name = "SuggestionServlet",
        urlPatterns = {"/suggest"}
)
public class SuggestionServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(SuggestionServlet.class.getName());
    private static final long serialVersionUID = 2L;
    private static final int DEFAULT_SIZE = 5;
    private static final int SUGGESTION_MAX_SIZE = 15;
    private static final JsonMapper jsonMapper = new JsonMapper(); // Reusable, thread-safe


  private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        String suggestText = request.getParameter(Params.QUERY_STRING);
        String[] indices = request.getParameterValues(Params.INDICES);
        int suggestSize = StringUtils.parseIntWithDefault(request.getParameter(Params.SIZE),DEFAULT_SIZE,1,SUGGESTION_MAX_SIZE);
        String jsonString;

        try (PrintWriter out = response.getWriter()) {
          try {
            jsonString = jsonMapper.writeValueAsString(
                CompletionSuggestion.getSuggestions(suggestText, suggestSize, indices));
            out.write(jsonString);
          } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ObjectNode errorNode = jsonMapper.createObjectNode();
            errorNode.put("code", 500);
            errorNode.put("message", "An error occurred while fetching suggestions");
            out.write(errorNode.toString());
           logger.log(Level.SEVERE, "Suggestion error", e);
           throw e;
          }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ObjectNode objectNode = jsonMapper.createObjectNode();
        objectNode.put("code", 405);
        objectNode.put("message", "Method Not Allowed");

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        request.setCharacterEncoding("UTF-8");

        try (PrintWriter out = response.getWriter()) {
            out.write(objectNode.toPrettyString()
            );
        }
    }

    @Override
    public String getServletInfo() {
        return "CompletionSuggestion servlet";
    }

}
