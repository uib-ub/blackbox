package no.uib.marcus.servlet;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.jackson.JacksonJsonpGenerator;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.stream.JsonGenerator;
import java.io.OutputStream;
import java.io.Serial;
import java.io.StringWriter;
import no.uib.marcus.client.ElasticsearchClientFactory;
import no.uib.marcus.common.Params;
import no.uib.marcus.common.util.FilterUtils;
import no.uib.marcus.common.util.SortUtils;
import no.uib.marcus.range.DateRange;
import no.uib.marcus.search.SearchBuilder;
import no.uib.marcus.search.SearchBuilderFactory;

import java.util.logging.Level;
import java.util.logging.Logger;

import no.uib.marcus.common.util.StringUtils;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;


/**
 * This servlet processes all HTTP requests coming from the "/search" endpoint
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
    private static final Logger logger = Logger.getLogger(SearchServlet.class.getName());
    private static final JsonMapper jsonMapper = new JsonMapper();
    private static final JacksonJsonpMapper jacksonJsonpMapper = new JacksonJsonpMapper(jsonMapper);

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * A method to process the HTTP <code>GET</code> requests
     *
     * @param request  HTTP servlet request
     * @param response HTTP servlet response
     * @throws IOException      if an I/O error occurs
     *                          <p>
     *                          writes a JSON string of search hits.
     **/
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");

        //Get parameters from the request
        String queryString = request.getParameter(Params.QUERY_STRING);
        String aggs = request.getParameter(Params.AGGREGATIONS);
        String[] indices = request.getParameterValues(Params.INDICES);
        String from = request.getParameter(Params.FROM);
        String size = request.getParameter(Params.SIZE);
        String fromDate = request.getParameter(Params.FROM_DATE);
        String toDate = request.getParameter(Params.TO_DATE);
        String sortString = request.getParameter(Params.SORT);
        String isPretty = request.getParameter(Params.PRETTY_PRINT);
        String[] selectedFilters = request.getParameterValues(Params.SELECTED_FILTERS);
        String service = request.getParameter(Params.SERVICE);
        String indexToBoost = request.getParameter(Params.INDEX_BOOST);

        try ( OutputStream out = response.getOutputStream()) {
            ElasticsearchClient client = ElasticsearchClientFactory.getElasticsearchClient();
            RestClientTransport restClientTransport = (RestClientTransport) client._transport();
            RestClient restClient = restClientTransport.restClient();




          //Assign default values, if needs be
            int _from = StringUtils.parseIntWithDefault(from, Params.DEFAULT_FROM, 0, Integer.MAX_VALUE);
            int _size = StringUtils.parseIntWithDefault(size, Params.DEFAULT_SIZE, 1, Params.MAX_SIZE);

            //Build a facet map based on selected filters.
            //E.g. {"subject.exact" = ["Flyfoto" , "Birkeland"], "type" = ["Brev"]}
            Map<String, List<String>> selectedFacets = FilterUtils.buildFilterMap(selectedFilters);


            logger.log(Level.FINE, "service: {0}", service);
            //Get and build the corresponding search builder based on the "service" parameter
            SearchBuilder<? extends SearchBuilder<?>> builder = SearchBuilderFactory
                    .getSearchBuilder(service, client)
                    .setIndices(indices)
                    .setQueryString(queryString)
                    .setAggregations(aggs)
                    .setFrom(_from)
                    .setSize(_size)
                    .setSelectedFacets(selectedFacets)
                    .setSortBuilder(SortUtils.getSort(sortString))
                    .setIndexToBoost(indexToBoost);

            //Add top level filter, for "AND" aggregations
            BoolQuery.Builder topFilter = FilterUtils.getTopFilter(
                    selectedFacets, aggs, DateRange.of(fromDate, toDate)
            );
            if (topFilter.hasClauses()) {
                builder.setFilter(topFilter.build());
            }
            //Add post-filter for "OR" aggregations if any
            BoolQuery.Builder postFilter = FilterUtils.getPostFilter(selectedFacets, aggs);
            if (postFilter.hasClauses()) {
              logger.log(Level.FINE, "postfilter hasClauses in SearchServlet:  {0}", postFilter.hasClauses());
              builder.setPostFilter(QueryBuilders.bool().must(postFilter.build()._toQuery()).build());
            }
            //Serialize SearchBuilder request to JSON to skip serialization and deserialization
            // and properly serialize aggregations without type names in
            // the key e.g., not "sterms#related.exact": but "related:exact"
            try (StringWriter writer = new StringWriter()) {
                JsonFactory jsonFactory = new JsonFactory();
                com.fasterxml.jackson.core.JsonGenerator jacksonGenerator = jsonFactory.createGenerator(writer);
                if (Boolean.parseBoolean(isPretty)) {
                    jacksonGenerator.useDefaultPrettyPrinter();
                }
                JsonGenerator generator = new JacksonJsonpGenerator(jacksonGenerator);
                builder.constructSearchRequest().build().serialize(generator, jacksonJsonpMapper );

                generator.close();

                // Are there things added other than indicies lost between the translation of low level
                // and the new rest client?
                String requestAsJson = writer.toString();
                Request request1 = new Request("POST", buildEndpoint(indices));
                request1.setJsonEntity(requestAsJson);
                request1.setOptions(RequestOptions.DEFAULT.toBuilder());
                Response response1 = restClient.performRequest(request1);
                response1.getEntity().writeTo(out);

            } catch (ResponseException e) {
                int status = e.getResponse().getStatusLine().getStatusCode();
                logger.warning("ES returned " + status + " for query [" + queryString + "]: " + e.getMessage());
                response.setStatus(status);
                e.getResponse().getEntity().writeTo(out);
            } catch (IllegalArgumentException e) {
                logger.warning("Bad request: " + e.getMessage());
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                ObjectNode error = jsonMapper.createObjectNode();
                error.put("error", e.getMessage());
                out.write(jsonMapper.writeValueAsBytes(error));
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("Broken pipe")
                        || e.getCause() instanceof IOException c && c.getMessage() != null && c.getMessage().contains("Broken pipe")) {
                    logger.log(Level.INFO, "Client disconnected before response was complete");
                } else {
                    logger.log(Level.SEVERE, "Unexpected error processing search request", e);
                    throw e;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unexpected error processing search request", e);
                throw e;
            }
            }
        }

    private static String buildEndpoint(String[] indices) {
        if (indices != null && indices.length > 0) {
          for (String index : indices) {
            if (index == null || index.contains("..") || index.contains("/")
                || index.startsWith("_") || index.contains("*")) {
              throw new IllegalArgumentException("Invalid index name: " + index);
            }
          }
            return "/" + String.join(",", indices) + "/_search";
        }
        return "/_search";
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

        ObjectNode objectNode = jsonMapper.createObjectNode();
        objectNode.put("field", 405);
        objectNode.put("message","Method Not Allowed");


        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);

        try (PrintWriter out = response.getWriter()) {
            out.write(objectNode.toPrettyString());
        }
    }

    @Override
    public String getServletInfo() {
        return "Search servlet";
    }

}
