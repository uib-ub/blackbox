package no.uib.marcus.servlet;

import no.uib.marcus.client.ClientFactory;
import no.uib.marcus.common.RequestParams;
import no.uib.marcus.common.util.FilterUtils;
import no.uib.marcus.common.util.QueryUtils;
import no.uib.marcus.common.util.SortUtils;
import no.uib.marcus.search.MarcusSearchBuilder;
import no.uib.marcus.search.ServiceFactory;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Booleans;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * This servlet processes all HTTP requests coming from "/search" endpoint
 * and gives back a response in the form of JSON string.
 * <p/>
 *
 * @author Hemed Al Ruwehy
 * 2016-01-24, University of Bergen Library.
 */
@WebServlet(
        name = "SearchServlet",
        urlPatterns = {"/search"},
        description = "Servlet for handling search requests"
)
public class SearchServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(SearchServlet.class);
    private static final long serialVersionUID = 1L;

    /**
     * A method to process the HTTP <code>GET</code> requests
     *
     * @param request  HTTP servlet request
     * @param response HTTP servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     * @returns a JSON string of search hits.
     **/
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        //Get parameters from the request
        String queryString = request.getParameter(RequestParams.QUERY_STRING);
        String aggs = request.getParameter(RequestParams.AGGREGATIONS);
        String[] indices = request.getParameterValues(RequestParams.INDICES);
        String[] types = request.getParameterValues(RequestParams.INDEX_TYPES);
        String from = request.getParameter(RequestParams.FROM);
        String size = request.getParameter(RequestParams.SIZE);
        String sortString = request.getParameter(RequestParams.SORT);
        String isPretty = request.getParameter(RequestParams.PRETTY_PRINT);

        try (PrintWriter out = response.getWriter()) {
            Client client = ClientFactory.getTransportClient();
            int _from = Strings.hasText(from)
                    ? Integer.parseInt(from)
                    : 0;
            int _size = Strings.hasText(size)
                    ? Integer.parseInt(size)
                    : 10;
            SortBuilder fieldSort = Strings.hasText(sortString)
                    ? SortUtils.getFieldSort(sortString)
                    : null;

            //Build a search service
            MarcusSearchBuilder searchService = ServiceFactory.createMarcusSearchService(client)
                    .setIndices(indices)
                    .setTypes(types)
                    .setQueryString(queryString)
                    .setAggregations(aggs)
                    .setFrom(_from)
                    .setSize(_size)
                    .setSortBuilder(fieldSort);

            //Build a bool filter
            BoolFilterBuilder boolFilter = FilterUtils.buildBoolFilter(request);
            if (boolFilter.hasClauses()) {
                searchService.setFilter(boolFilter);
            }
            //Get all documents from the service
            SearchResponse searchResponse = searchService.getDocuments();

            //Decide whether to get a pretty JSON output or not
            String searchResponseString = Booleans.isExplicitTrue(isPretty)
                    ? QueryUtils.toJsonString(searchResponse, true)
                    : QueryUtils.toJsonString(searchResponse, false);

            //Write response to the client
            out.write(searchResponseString);

            //Log what has been queried
            Map<String, String[]> parameterMapCopy = new HashMap<>(request.getParameterMap());
            //Remove those that we are not interested in logging.
            parameterMapCopy.remove("aggs");
            parameterMapCopy.remove("index");
            XContentBuilder builder = XContentFactory.jsonBuilder()
                    .startObject()
                    //.field("host", request.getRemoteAddr().equals("0:0:0:0:0:0:0:1") ? InetAddress.getLocalHost() : request.getRemoteAddr())
                    .field("params", parameterMapCopy)
                    .field("hits", searchResponse.getHits().getTotalHits())
                    .field("took", searchResponse.getTook())
                    .endObject();
            logger.info(builder.string());
        }
        catch (org.elasticsearch.action.search.SearchPhaseExecutionException ex){

        }
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
        doGet(request, response);
    }

    @Override
    public String getServletInfo() {
        return "SearchServlet servlet";
    }

}
