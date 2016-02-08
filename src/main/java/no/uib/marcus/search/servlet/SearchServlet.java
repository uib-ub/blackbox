package no.uib.marcus.search.servlet;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import no.uib.marcus.common.AggregationUtil;

import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;

import no.uib.marcus.search.MarcusSearchService;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Strings;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

/**
 * @author Hemed Al Ruwehy (hemed.ruwehy@uib.no) 2016-01-24, University of
 * Bergen Library
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
                //Share this object amongst all requests.
                this.service = new MarcusSearchService();
        }
        protected void processRequest(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
                request.setCharacterEncoding("UTF-8");
                response.setContentType("text/html;charset=UTF-8");
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
                SearchResponse searchResponse;
                BoolFilterBuilder boolFilter;

                try (PrintWriter out = response.getWriter()) {
                        int _from = Strings.hasText(from) ? Integer.parseInt(from) : 0;
                        int _size = Strings.hasText(size) ? Integer.parseInt(size) : 10;
                        SortBuilder fieldSort = Strings.hasText(sortString) ? getFieldSort(sortString) : null;
                        
                        /**Override service properties from previous request**/
                        service.setIndices(indices);
                        service.setTypes(types);
                        service.setAggregations(aggs);
                        service.setFrom(_from);
                        service.setSize(_size);
                        service.setSort(fieldSort);
 
                        boolFilter = (BoolFilterBuilder) buildBoolFilter(selectedFilters, aggs, fromDate, toDate);
                        if (boolFilter.hasClauses()) {
                                searchResponse = service.getDocuments(queryString, boolFilter);
                        } else {
                                searchResponse = service.getDocuments(queryString);
                        }
                        String searchResponseString = searchResponse.toString();
                        if(searchResponse != null){
                                //After getting the response, add extra field "total_doc_count" 
                                //to every bucket in the aggregations
                                searchResponseString = service.addExtraFieldToBucketsAggregation(searchResponse);
                        }
                        //logger.info("Marcus service: " + service.toString() + "\n" + service.toJsonString());
                        out.write(searchResponseString);
                }
        }

        /**
         * A method for building BoolFilter based on the aggregation settings.
         *
         */
        private FilterBuilder buildBoolFilter(String[] selectedFilters, String aggregations, String fromDate, String toDate) {
                //In this map, keys are "fields" and values are "terms"
                Map<String, List> filterMap = AggregationUtil.getFilterMap(selectedFilters);
                BoolFilterBuilder boolFilter = FilterBuilders.boolFilter();
                try {
                        if (fromDate != null || toDate != null) {
                                boolFilter
                                        //Range within "created" field
                                        .should(FilterBuilders.rangeFilter("created").gte(fromDate).lte(toDate))
                                        //madeafter >= from_date and madeafter <= to_date
                                        .should(FilterBuilders.boolFilter()
                                                .must(FilterBuilders.rangeFilter("madeafter").gte(fromDate))
                                                .must(FilterBuilders.rangeFilter("madeafter").lte(toDate)))
                                        //madebefore >= from_date and madebefore <= to_date
                                        .should(FilterBuilders.boolFilter()
                                                .must(FilterBuilders.rangeFilter("madebefore").gte(fromDate))
                                                .must(FilterBuilders.rangeFilter("madebefore").lte(toDate)));

                                /**
                                 * AND filter.
                                 * .should(FilterBuilders.boolFilter()
                                 * .must((FilterBuilders.rangeFilter("madeafter").gte(fromDate)))
                                 * .must(FilterBuilders.rangeFilter("madebefore").lte(fromDate)));
                                 *
                                 */
                        }
                        /**
                         * Building the BoolFilter based on user selected
                         * facets
                         */
                        for (Map.Entry<String, List> entry : filterMap.entrySet()) {
                                if (!entry.getValue().isEmpty()) {
                                        if (AggregationUtil.contains(aggregations, "operator", "AND")) {
                                                logger.info("Constructing AND query for facet: " + entry.getKey());
                                                for (Object value : entry.getValue()) {
                                                        //Building "AND" filter. A filter based on a term. 
                                                        //If it is inside a must, it acts as "AND" filter
                                                        boolFilter.must(FilterBuilders.termFilter(entry.getKey(), value));
                                                }
                                        } else {
                                                /**
                                                 *Building "OR" filter. Using "terms" filter based on several terms, matching on
                                                 *any of them This acts as OR filter, when it is inside the must clause.
                                                 */
                                                boolFilter.must(FilterBuilders.termsFilter(entry.getKey(), entry.getValue()));
                                        }
                                }
                        }
                } catch (Exception ex) {
                        logger.error("Exception occured on constructing OR filter" + ex.getLocalizedMessage());
                }
                return boolFilter;
        }

        /**
         * Building a fieldSort.
         *
         * @param sortString, a string that contains a field and sort type in
         * the form of "field_asc" or "field_desc"
         */
        private SortBuilder getFieldSort(String sortString) {
                SortBuilder sortBuilder = null;
                SortOrder sortOrder = null;
                try {
                        int lastIndex = sortString.lastIndexOf('.');
                        String field = sortString.substring(0, lastIndex).trim();
                        String order = sortString.substring(lastIndex + 1, sortString.length()).trim();

                        if (order.equalsIgnoreCase("asc")) {
                                sortOrder = SortOrder.ASC;
                        }
                        if (order.equalsIgnoreCase("desc")) {
                                sortOrder = SortOrder.DESC;
                        }
                        //Build sort
                        sortBuilder = SortBuilders
                                .fieldSort(field)
                                .missing("_last");

                        if (sortBuilder != null) {
                                sortBuilder.order(sortOrder);
                        }

                } catch (ElasticsearchException e) {
                        logger.error("Sorting cannot be constructed. " + e.getDetailedMessage());
                } catch (StringIndexOutOfBoundsException e) {
                        logger.error("The sort string does not contain a dot, hence cannot be split into field-value pair. "
                                + "The method expects to find a dot that seperate a field and it's sort type "
                                + "but found: " + sortString);
                }
                return sortBuilder;
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

}
