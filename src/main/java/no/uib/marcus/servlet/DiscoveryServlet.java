package no.uib.marcus.servlet;

import no.uib.marcus.client.ClientFactory;
import no.uib.marcus.common.RequestParams;
import no.uib.marcus.search.MarcusDiscoveryBuilder;
import no.uib.marcus.search.ServiceFactory;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
    private static final long serialVersionUID = 3L;

    /**
     * Process a request
     **/
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        String[] indices = request.getParameterValues(RequestParams.INDICES);
        String[] types = request.getParameterValues(RequestParams.INDEX_TYPES);
        String from = request.getParameter(RequestParams.FROM);
        String size = request.getParameter(RequestParams.SIZE);

        int _from = Strings.hasText(from) ? Integer.parseInt(from) : 0;
        int _size = Strings.hasText(size) ? Integer.parseInt(size) : 10;
        Client client = ClientFactory.getTransportClient();

        //Build a discovery service
        MarcusDiscoveryBuilder service = ServiceFactory.createMarcusDiscoveryService(client)
                .setIndices(indices)
                .setTypes(types)
                .setFrom(_from)
                .setSize(_size);

        try (PrintWriter out = response.getWriter()) {
            SearchResponse searchResponse = service.getDocuments();
            out.write(searchResponse.toString());
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
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
        processRequest(request, response);
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
