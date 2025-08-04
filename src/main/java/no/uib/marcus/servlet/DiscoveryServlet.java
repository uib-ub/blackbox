package no.uib.marcus.servlet;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.Serial;
import no.uib.marcus.client.ElasticsearchClientFactory;
import no.uib.marcus.common.Params;
import no.uib.marcus.common.util.QueryUtils;
import no.uib.marcus.search.MarcusDiscoveryBuilder;
import no.uib.marcus.search.SearchBuilderFactory;

import no.uib.marcus.common.util.StringUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Discovery service which is available at "/discover" endpoint only allow
 * to explore Marcus data in a simplest fashion.
 * It does not provide advance functionality such as free text search, aggregations or sorting.
 * For complex operations, please go to "/search" endpoint.
 */
@WebServlet(
        name = "DiscoveryServlet",
        urlPatterns = {"/discover"}
)
public class DiscoveryServlet extends HttpServlet {
    @Serial
    private static final long serialVersionUID = 3L;

    /**
     * Process a request
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        String from = request.getParameter(Params.FROM);
        String size = request.getParameter(Params.SIZE);
        String[] indices = request.getParameterValues(Params.INDICES);
        String queryString = request.getParameter(Params.QUERY_STRING);

        int _size = StringUtils.hasText(size) ? Integer.parseInt(size) : Params.DEFAULT_SIZE;
        int _from = StringUtils.hasText(from) ? Integer.parseInt(from) : Params.DEFAULT_FROM;
        ElasticsearchClient client = ElasticsearchClientFactory.getElasticsearchClient();

        //Build a discovery service
        MarcusDiscoveryBuilder service = SearchBuilderFactory.marcusDiscovery(client)
                .setIndices(indices)
                .setFrom(_from)
                .setSize(_size)
                .setQueryString(queryString);

        try (PrintWriter out = response.getWriter()) {
            SearchResponse<ObjectNode> searchResponse =  service.executeSearch();
            out.write(QueryUtils.toJsonString(searchResponse, true));
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws IOException      if an I/O error occurs
     */
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
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        ObjectNode objectNode = new ObjectMapper().createObjectNode();
        objectNode.put("code", 405);
        objectNode.put("message", "Method Not Allowed");
        try (PrintWriter out = response.getWriter()) {
            out.write(objectNode.toPrettyString()
            );
        }
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Discover servlet";
    }
}
