package no.uib.marcus.servlet;

import no.uib.marcus.client.ClientFactory;
import no.uib.marcus.common.Params;
import no.uib.marcus.common.util.*;
import no.uib.marcus.search.MarcusSearchBuilder;
import no.uib.marcus.search.SearchBuilderFactory;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Booleans;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.search.sort.SortBuilder;

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
        String queryString = request.getParameter(Params.QUERY_STRING);
        String aggs = request.getParameter(Params.AGGREGATIONS);
        String[] indices = request.getParameterValues(Params.INDICES);
        String[] types = request.getParameterValues(Params.INDEX_TYPES);
        String from = request.getParameter(Params.FROM);
        String size = request.getParameter(Params.SIZE);
        String sortString = request.getParameter(Params.SORT);
        String isPretty = request.getParameter(Params.PRETTY_PRINT);
        String[] selectedFilters = request.getParameterValues(Params.SELECTED_FILTERS);
        String fromDate = request.getParameter(Params.FROM_DATE);
        String toDate = request.getParameter(Params.TO_DATE);

        try (PrintWriter out = response.getWriter()) {
            Client client = ClientFactory.getTransportClient();
            int offset = Strings.hasText(from)
                    ? Integer.parseInt(from)
                    : 0;
            int resultSize = Strings.hasText(size)
                    ? Integer.parseInt(size)
                    : 10;
            SortBuilder fieldSort = Strings.hasText(sortString)
                    ? SortUtils.getFieldSort(sortString)
                    : null;

            //Build a filter map based on selected facets. In the result map
            //keys are "fields" and values are "terms"
            //e.g {"subject.exact" = ["Flyfoto" , "Birkeland"], "type" = ["Brev"]}
            Map<String, List<String>> selectedFacetMap = AggregationUtils.buildFilterMap(selectedFilters);

            //Build a search service
            MarcusSearchBuilder searchService = SearchBuilderFactory.createMarcusSearchService(client)
                    .setIndices(indices)
                    .setTypes(types)
                    .setQueryString(queryString)
                    .setAggregations(aggs)
                    .setFrom(offset)
                    .setSize(resultSize)
                    .setSelectedFacets(selectedFacetMap)
                    .setSortBuilder(fieldSort);

            //Build a top level filter
            Map<String, BoolFilterBuilder> boolFilterMap = FilterUtils
                    .buildBoolFilter(selectedFacetMap, aggs, fromDate, toDate);

            //Set top level filter
            if (boolFilterMap.get(Params.TOP_FILTER).hasClauses()) {
                searchService.setFilter(boolFilterMap.get(Params.TOP_FILTER));
            }

            //Set post_filter, so that aggregations should not be affected by the query.
            //post_filter only affects search results but NOT aggregations
            if (boolFilterMap.get(Params.POST_FILTER).hasClauses()) {
                searchService.setPostFilter(boolFilterMap.get(Params.POST_FILTER));
            }

            //Get all documents from the service
            SearchResponse searchResponse = searchService.getDocuments();

            //Decide whether to get a pretty JSON output or not
            String searchResponseString = Booleans.isExplicitTrue(isPretty)
                    ? QueryUtils.toJsonString(searchResponse, true)
                    : QueryUtils.toJsonString(searchResponse, false);

            //Write response to the client
            out.write(searchResponseString);

            //Log search response
            logger.info(LogUtils.logSearchResponse(request, searchResponse));
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
        return "Search servlet";
    }

}
