package no.uib.marcus.search.servlet;

import no.uib.marcus.common.SortUtils;
import no.uib.marcus.search.MarcusSearchService;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.search.sort.SortBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet(
        name = "DiscoverServlet",
        urlPatterns = {"/discover"}
)
public class DiscoverServlet extends HttpServlet {
    private static final long serialVersionUID = 3L;

    /**
     * Process a request
     **/
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        String[] indices = request.getParameterValues("index");
        String[] types = request.getParameterValues("type");
        String from = request.getParameter("from");
        String size = request.getParameter("size");
        String sortString = request.getParameter("sort");

        int _from = Strings.hasText(from) ? Integer.parseInt(from) : 0;
        int _size = Strings.hasText(size) ? Integer.parseInt(size) : 10;
        SortBuilder fieldSort = Strings.hasText(sortString) ? SortUtils.getFieldSort(sortString) : null;

        MarcusSearchService service = new MarcusSearchService();
        service.setIndices(indices);
        service.setTypes(types);
        service.setFrom(_from);
        service.setSize(_size);
        service.setSort(fieldSort);

        try (PrintWriter out = response.getWriter()) {
            SearchResponse searchResponse = service.getAllDocuments();
            out.write(searchResponse.toString());
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

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
    }// </editor-fold>
}
