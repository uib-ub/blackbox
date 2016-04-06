package no.uib.marcus.search.servlet;


import no.uib.marcus.common.AggregationUtils;
import no.uib.marcus.common.SortUtils;
import no.uib.marcus.search.MarcusSearchService;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Booleans;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.search.sort.SortBuilder;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract: This class processes all HTTP requests coming from "/search" endpoint
 * and gives back a response in the form of JSON string.
 * <p/>
 * @author Hemed A. Al Ruwehy
 * 2016-01-24, University of Bergen Library.
 */
@WebServlet(
        name = "SearchServlet",
        urlPatterns = {"/search"}
)
public class SearchServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(SearchServlet.class);
    private static final long serialVersionUID = 1L;
    MarcusSearchService service;

    public SearchServlet() {
        //Share this object amongst all requests in this context.
        this.service = new MarcusSearchService();
    }

    /**
     * A method to process the HTTP request
     *
     * @param request  HTTP request
     * @param response HTTP response
     * <p>
     * returns a JSON string of search hits.
     **/
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        /*Get parameters from search request*/
        String queryString = request.getParameter("q");
        String[] selectedFilters = request.getParameterValues("filter");
        String aggs = request.getParameter("aggs");
        String[] indices = request.getParameterValues("index");
        String[] types = request.getParameterValues("type");
        String from = request.getParameter("from");
        String size = request.getParameter("size");
        String fromDate = request.getParameter("from_date");
        String toDate = request.getParameter("to_date");
        String sortString = request.getParameter("sort");
        String isPretty = request.getParameter("pretty");
        SearchResponse searchResponse;
        BoolFilterBuilder boolFilter;
        String searchResponseString;

        try (PrintWriter out = response.getWriter()) {
            int _from = Strings.hasText(from) ? Integer.parseInt(from) : 0;
            int _size = Strings.hasText(size) ? Integer.parseInt(size) : 10;
            SortBuilder fieldSort = Strings.hasText(sortString) ? SortUtils.getFieldSort(sortString) : null;
            //Override service properties from previous request
            service.setIndices(indices);
            service.setTypes(types);
            service.setAggregations(aggs);
            service.setFrom(_from);
            service.setSize(_size);
            service.setSort(fieldSort);

            boolFilter = buildBoolFilter(selectedFilters, aggs, fromDate, toDate);
            if (boolFilter.hasClauses()) {
                searchResponse = service.getDocuments(queryString, boolFilter);
            } else {
                searchResponse = service.getDocuments(queryString);
            }

            //Decide whether to get a pretty JSON output or not
            searchResponseString = Booleans.isExplicitTrue(isPretty)
                    ? service.toJsonString(searchResponse, true)
                    : service.toJsonString(searchResponse, false);

            //Write the response to the output stream
            out.write(searchResponseString);

            /**======Log what has been queried===**/
            Map<String, String[]> parameterMap = new HashMap<>(request.getParameterMap());
            //Remove aggregations from the parameter list, we are not interested to logging them.
            parameterMap.remove("aggs");
            XContentBuilder builder = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("status" , response.SC_OK)
                    .field("host", request.getRemoteAddr().equals("0:0:0:0:0:0:0:1") ? InetAddress.getLocalHost() : request.getRemoteAddr())
                    .field("params", parameterMap)
                    .field("hits" , searchResponse.getHits().getTotalHits())
                    .field("took" , searchResponse.getTook())
                    .endObject();
            logger.info(builder.string());

        }
    }

    /**
     * A method for building BoolFilter based on the aggregation settings.
     */
    private BoolFilterBuilder buildBoolFilter(String[] selectedFilters, String aggregations, String fromDate, String toDate) {
        //In this map, keys are "fields" and values are "terms"
        Map<String, List> filterMap = AggregationUtils.getFilterMap(selectedFilters);
        BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();
        try {
            //Build a date filter
            if (Strings.hasText(fromDate) || Strings.hasText(toDate)) {
                boolFilter
                        //Range within "available" field
                        .should(FilterBuilders.rangeFilter(DateField.AVAILABLE).gte(fromDate).lte(toDate))
                        //Range within "created" field
                        .should(FilterBuilders.rangeFilter(DateField.CREATED).gte(fromDate).lte(toDate))
                        //madeBefore >= from_date and madeBefore <= to_date
                        .should(FilterBuilders.boolFilter()
                                .must(FilterBuilders.rangeFilter(DateField.MADE_BEFORE).gte(fromDate))
                                .must(FilterBuilders.rangeFilter(DateField.MADE_BEFORE).lte(toDate)))
                        //madeAfter >= from_date and madeAfter <= to_date
                        .should(FilterBuilders.boolFilter()
                                .must(FilterBuilders.rangeFilter(DateField.MADE_AFTER).gte(fromDate))
                                .must(FilterBuilders.rangeFilter(DateField.MADE_AFTER).lte(toDate)));
            }
            //Building a filter based on the user selected facets
            for (Map.Entry<String, List> entry : filterMap.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    if (AggregationUtils.contains(aggregations, entry.getKey(), "operator", "AND")) {
                        //logger.info("Constructing AND query for facet: " + entry.getKey());
                        for (Object value : entry.getValue()) {
                            //Building "AND" filter.
                            boolFilter.must(FilterBuilders.termFilter(entry.getKey(), value));
                        }
                    } else {
                        //Building "OR" filter.
                        boolFilter.must(FilterBuilders.termsFilter(entry.getKey(), entry.getValue()));
                    }
                }
            }
        } catch (Exception ex) {
            logger.error("Exception occurred while constructing OR filter" + ex.getLocalizedMessage());
        }
        return boolFilter;
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
        return "Search servlet";
    }

    /**
     * Date fields to perform date ranges.
     **/
    public static class DateField {
        public static final String AVAILABLE = "available";
        public static final String CREATED = "created";
        public static final String MADE_BEFORE = "madeBefore";
        public static final String MADE_AFTER = "madeAfter";
    }

}
