package no.uib.marcus.search;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import no.uib.marcus.client.ClientFactory;
import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;
import org.elasticsearch.search.sort.SortBuilder;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Optional;

/**
 * Builder for Marcus search service
 * @author Hemed A. Al Ruwehy
 * @since 0.1
 * 2016-01-24, University of Bergen Library.
 */
public class MarcusSearchBuilder extends AbstractSearchBuilder<MarcusSearchBuilder> {

    private static final Logger logger = Logger.getLogger(MarcusSearchBuilder.class);
    private FilterBuilder filterBuilder;
    private String aggregations;
    private SortBuilder sortBuilder;
    private int from = -1;
    private int size = -1;

    /**
     * Constructor
     *
     * @param client Elasticsearch client to communicate with a cluster.
     */
    public MarcusSearchBuilder(@NotNull Client client){
       super(client);
    }

    /**
     * Set aggregations to be applied
     * @param aggregations a JSON string of aggregations
     *
     * @return this object where aggregations have been set
     */
    public MarcusSearchBuilder setAggregations(String aggregations) {
        if(aggregations != null && isValidJSONArray(aggregations)) {
            this.aggregations = aggregations;
        }
        return this;
    }

    /**
     * Set from, a start of a document, default to 0
     * @param from
     *
     * @return this object where from is set
     */
    public MarcusSearchBuilder setFrom(int from) {
        if(from >= 0) {
            this.from = from;
        }
        return this;
    }

    /**
     * Set how many documents to be returned.
     * @param size
     *
     * @return this object where size has been set
     */
    public MarcusSearchBuilder setSize(int size) {
        if(size >= 0) {
            this.size = size;
        }
        return this;
    }

    /**
     * Set a sortBuilder order
     * @param sortBuilder
     *
     * @return this object where sort has been set
     */
    public MarcusSearchBuilder setSortBuilder(SortBuilder sortBuilder) {
        this.sortBuilder = sortBuilder;
        return this;
    }

    /**
     * Set a filterBuilder
     * @param filter
     *
     * @return this object where filter has been set
     */
    public MarcusSearchBuilder setFilter(FilterBuilder filter) {
        this.filterBuilder = filter;
        return this;
    }

    /**
     * Validate aggregations
     **/
     private boolean isValidJSONArray(String jsonString){
         JsonElement element =  new JsonParser().parse(jsonString);
             if(!element.isJsonArray()){
                 throw new IllegalParameterException(
                         "Aggregations must be valid JSON. Expected JSON Array of objects but found : ["+ jsonString + "]");
             }
         return true;
     }

    /**
     * Experimental search response
     *
     * @param request
     * @return optional search response
     */
    private static Optional<SearchResponse> getSearchResponse(SearchRequestBuilder request) {
        Optional<SearchResponse> optionalResponse = Optional.empty();
        try {
            SearchResponse response = request.execute().actionGet();
            if (response.getHits().getTotalHits() > -1) {
                optionalResponse = Optional.of(response);
            }
        } catch (ElasticsearchException e) {
            logger.error(e.getDetailedMessage());
        }
        return optionalResponse;
    }

    /**
     * Get all documents based on the service settings.
     *
     * @return a SearchResponse, can be <code>null</code>, which means search was not successfully executed.
     */
    @Override
    @Nullable
    public SearchResponse getDocuments() {
        assert super.getClient() != null;
        SearchResponse response = null;
        SearchRequestBuilder searchRequest;
        QueryBuilder query;
        FunctionScoreQueryBuilder functionScoreQueryBuilder;
        try {
            //Prepare search request
            searchRequest = getClient().prepareSearch();

            //Set indices
            if (getIndices() != null && getIndices().length > 0) {
                searchRequest.setIndices(getIndices());
            }
            //Set types
            if (getTypes() != null && getTypes().length > 0) {
                searchRequest.setTypes(getTypes());
            }

            //Set query
            if (Strings.hasText(getQueryString())) {
                //Use query_string query with AND operator
                functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(QueryUtils.buildQueryString(getQueryString()));
            } else {
                //Match all documents and boost the documents with label "fana" because they
                //are colored photo and they beautify the page.
                // This is just for coolness and it has no harm if they don't exist
                functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery())
                        .add(FilterBuilders.queryFilter(QueryBuilders.simpleQueryStringQuery("fana")), ScoreFunctionBuilders.weightFactorFunction(2));
            }

            //Boost documents of type "fotografi"
            query = functionScoreQueryBuilder
                    .add(FilterBuilders.termFilter("type", "fotografi"), ScoreFunctionBuilders.weightFactorFunction(2));

            //Set Query, whether with or without filter
            if(filterBuilder != null){
                searchRequest.setQuery(QueryBuilders.filteredQuery(query, filterBuilder));
            }
            else {
                searchRequest.setQuery(query);
            }

            //Set from and size
            searchRequest.setFrom(from);
            searchRequest.setSize(size);

            //Set sortBuilder
            if (sortBuilder != null) {
                searchRequest.addSort(sortBuilder);
            }
            //Append aggregations to the request builder
            if (Strings.hasText(aggregations)) {
                AggregationUtils.addAggregations(searchRequest, aggregations);
            }

            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());

            //Execute the response
            response = searchRequest.execute().actionGet();

            //logger.info(response.toString());
        } catch (SearchSourceBuilderException e) {
            logger.error("Exception occurred when building search request: " + e.getDetailedMessage());
        } catch (SearchPhaseExecutionException e) {
            //I've not found a direct way to validate a query string. Therefore, the idea here is to catch any
            //exception that is related to search execution.
            logger.error("Could not execute search: " + e.getDetailedMessage());
        }
        return response;
    }

    /**
     * Print out properties of this instance as a JSON string
     *
     * @return a JSON string of service properties
     */
    @Override
    public String toString() {
        try {
            XContentBuilder jsonObj = XContentFactory.jsonBuilder().prettyPrint()
                    .startObject()
                    .field("indices", getIndices() == null ? Strings.EMPTY_ARRAY : getIndices())
                    .field("type", getTypes() == null ? Strings.EMPTY_ARRAY : getTypes())
                    .field("from", from)
                    .field("size", size)
                    .field("aggregations", aggregations == null ? Strings.EMPTY_ARRAY : aggregations)
                    .endObject();
            return jsonObj.string();
        } catch (IOException e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }


    //Main method for easy debugging
    public static void main(String[] args) throws IOException {
        Client c = ClientFactory.getTransportClient();
        MarcusSearchBuilder service = ServiceFactory.createMarcusSearchService(c);
        service.setAggregations("koba"); //Invalid aggs, it should fail.
        service.setClient(null);
        service.setQueryString("~ana");
        System.out.println(QueryUtils.toJsonString(service.getDocuments(), true));
        /**try {
         //System.out.println(QueryUtils.toJsonString(service.getDocuments(), true));
         SearchRequestBuilder sr = c.prepareSearch("admin-test").setQuery(QueryBuilders.queryStringQuery("~ana"));
         //System.out.println(sr.execute().actionGet().toString());
         System.out.println(getSearchResponse(sr));
         }
         catch(org.elasticsearch.index.query.QueryParsingException ex){
         logger.error("Your query was fucked up: " + service.getQueryString());
         }
         /**catch (org.elasticsearch.action.search.SearchPhaseExecutionException ex){
         logger.error("Unable to execute search : " + ex.getMessage());
         }**/
    }
}

