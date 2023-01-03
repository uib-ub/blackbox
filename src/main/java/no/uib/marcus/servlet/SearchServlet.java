package no.uib.marcus.servlet;

import no.uib.marcus.client.ClientFactory;
import no.uib.marcus.common.Params;
import no.uib.marcus.common.util.FilterUtils;
import no.uib.marcus.common.util.LogUtils;
import no.uib.marcus.common.util.QueryUtils;
import no.uib.marcus.common.util.SortUtils;
import no.uib.marcus.range.DateRange;
import no.uib.marcus.search.SearchBuilder;
import no.uib.marcus.search.SearchBuilderFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.internal.Client;
import org.elasticsearch.core.Booleans;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
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
        description = "Servlet for handling search requests")

public class SearchServlet extends HttpServlet {
    private static final Logger logger = LogManager.getLogger(SearchServlet.class);
    private static final long serialVersionUID = 1L;

    /**
     * A method to process the HTTP <code>GET</code> requests
     *
     * @param request  HTTP servlet request
     * @param response HTTP servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     *                          <p>
     *                          writes a JSON string of search hits.
     **/
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        //Get parameters from the request
        String queryString = request.getParameter(Params.QUERY_STRING);
        String aggs = request.getParameter(Params.AGGREGATIONS);
        String[] indices = request.getParameterValues(Params.INDICES);
        String[] types = request.getParameterValues(Params.INDEX_TYPES);
        String from = request.getParameter(Params.FROM);
        String size = request.getParameter(Params.SIZE);
        String fromDate = request.getParameter(Params.FROM_DATE);
        String toDate = request.getParameter(Params.TO_DATE);
        String sortString = request.getParameter(Params.SORT);
        String isPretty = request.getParameter(Params.PRETTY_PRINT);
        String[] selectedFilters = request.getParameterValues(Params.SELECTED_FILTERS);
        String service = request.getParameter(Params.SERVICE);
        String indexToBoost = request.getParameter(Params.INDEX_BOOST);

        try (PrintWriter out = response.getWriter()) {
            Client client = ClientFactory.getTransportClient();

            //Assign default values, if needs be
            int _from = Strings.hasText(from) ? Integer.parseInt(from) : Params.DEFAULT_FROM;
            int _size = Strings.hasText(size) ? Integer.parseInt(size) : Params.DEFAULT_SIZE;

            // Build a facet map based on selected filters.
            // e.g {"subject.exact" = ["Flyfoto" , "Birkeland"], "type" = ["Brev"]}
            Map<String, List<String>> selectedFacets = FilterUtils.buildFilterMap(selectedFilters);

            //Get and build corresponding search builder based on the "service" parameter
            SearchBuilder<? extends SearchBuilder<?>> builder = SearchBuilderFactory
                    .getSearchBuilder(service, client)
                    .setIndices(indices)
                    .setTypes(types)
                    .setQueryString(queryString)
                    .setAggregations(aggs)
                    .setFrom(_from)
                    .setSize(_size)
                    .setSelectedFacets(selectedFacets)
                    .setSortBuilder(SortUtils.getSort(sortString))
                    .setIndexToBoost(indexToBoost);

            //Add top level filter, for "AND" aggregations
            BoolQueryBuilder topFilter = FilterUtils.getTopFilter(
                    selectedFacets, aggs, DateRange.of(fromDate, toDate)
            );
            if (topFilter.hasClauses()) {
                builder.setFilter(topFilter);
            }
            //Add post filter for "OR" aggregations if any
            BoolQueryBuilder postFilter = FilterUtils.getPostFilter(selectedFacets, aggs);
            if (postFilter.hasClauses()) {
                builder.setPostFilter(postFilter);
            }
            //Send search request to Elasticsearch and execute
            SearchResponse searchResponse = builder.executeSearch();

            //Decide whether to get a pretty JSON output or not
            String searchResponseString = Booleans.isTrue(isPretty)
                    ? QueryUtils.toJsonString(searchResponse, true)
                    : QueryUtils.toJsonString(searchResponse, false);

            //Write response to the client
            out.write(searchResponseString);

            // Log search response
            logger.info(LogUtils.createLogMessage(request, searchResponse));
            // System.out.println("Builder class: " + builder.getClass().getName() + " " +
            //                 "\nBuilder toString: " + builder  );
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

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        try (PrintWriter out = response.getWriter();
             XContentBuilder xContentBuilder = XContentFactory.jsonBuilder()) {
            out.write(xContentBuilder
                    .startObject()
                    .field("code", 405)
                    .field("message", "Method Not Allowed")
                    .endObject()
                    .toString()
             );
        }
    }

    @Override
    public String getServletInfo() {
        return "Search servlet";
    }

}
