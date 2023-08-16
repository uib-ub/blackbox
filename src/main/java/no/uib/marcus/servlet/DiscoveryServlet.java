package no.uib.marcus.servlet;

import no.uib.marcus.client.RestClientFactory;
import no.uib.marcus.common.Params;
import no.uib.marcus.common.util.QueryUtils;
import no.uib.marcus.search.MarcusDiscoveryBuilder;
import no.uib.marcus.search.SearchBuilderFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Discovery service which is available at "/discover" endpoint only allow
 * to explore Marcus data in the simplest fashion.
 * It does not provide advance functionality such as free text search, aggregations or sorting.
 * For complex operations, please go to "/search" endpoint.
 */
@WebServlet(
        name = "DiscoveryServlet",
        urlPatterns = {"/discover"}
)
public class DiscoveryServlet extends HttpServlet {
    private static final long serialVersionUID = 3L;

    /**
     * Process a request
     */
    private void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws  IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        String from = request.getParameter(Params.FROM);
        String size = request.getParameter(Params.SIZE);
        String[] indices = request.getParameterValues(Params.INDICES);
        String[] types = request.getParameterValues(Params.INDEX_TYPES);
        String queryString = request.getParameter(Params.QUERY_STRING);

        int _size = Strings.hasText(size) ? Integer.parseInt(size) : Params.DEFAULT_SIZE;
        int _from = Strings.hasText(from) ? Integer.parseInt(from) : Params.DEFAULT_FROM;
        RestHighLevelClient client = RestClientFactory.getHighLevelRestClient();

        //Build a discovery service
        MarcusDiscoveryBuilder service = SearchBuilderFactory.marcusDiscovery(client)
                .setIndices(indices)
                .setTypes(types)
                .setFrom(_from)
                .setSize(_size)
                .setQueryString(queryString);

        try (PrintWriter out = response.getWriter()) {
            SearchResponse searchResponse = service.executeSearch();
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

        try (PrintWriter out = response.getWriter();
             XContentBuilder xcontentBuilder =  XContentFactory.jsonBuilder()) {
            out.write(xcontentBuilder
                    .startObject()
                    .field("code", 405)
                    .field("message", "Method Not Allowed")
                    .endObject()
                    .toString()
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
