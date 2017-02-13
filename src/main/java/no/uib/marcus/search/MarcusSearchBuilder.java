package no.uib.marcus.search;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Builder for Marcus search service
 *
 * @author Hemed A. Al Ruwehy
 * @since 0.1
 * 2016-01-24, University of Bergen Library.
 */
public class MarcusSearchBuilder extends AbstractSearchBuilder<MarcusSearchBuilder> {

    private final Logger logger = Logger.getLogger(getClass().getName());
    private FilterBuilder filter;
    private FilterBuilder postFilter;
    private Map<String, List<String>> selectedFacets;
    private String aggregations;
    private SortBuilder sortBuilder;
    private String indexToBoost;

    //A list of images that will be
    //randomly loaded at the front page.
    private final String[] randomList =
            {
                    "Knud Knudsen",
                    "Postkort",
                    "Sophus Tromholt",
                    "Marcus Selmer",
                    "Widerøesamlingen",
                    "Nyborg"
            };

    /**
     * Build Marcus search service
     * @param client Elasticsearch client to communicate with a cluster.
     */
    public MarcusSearchBuilder(@NotNull Client client) {
        super(client);
    }

    /**
     * Experimental search response
     *
     * @param request
     * @return optional search response
     */
    private Optional<SearchResponse> getSearchResponse(SearchRequestBuilder request) {
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
     * Set aggregations to be applied
     *
     * @param aggregations a JSON string of aggregations
     * @return this object where aggregations have been set
     */
    public MarcusSearchBuilder setAggregations(String aggregations) {
        if (aggregations != null && AggregationUtils.isValidJSONArray(aggregations)) {
            this.aggregations = aggregations;
        }
        return this;
    }

    /**
     * Get aggregations or <tt>null</tt> if not set
     **/
    public String getAggregations() {
        return aggregations;
    }

    /**
     * Set a sortBuilder order
     *
     * @param sortBuilder
     * @return this object where sort has been set
     */
    public MarcusSearchBuilder setSortBuilder(SortBuilder sortBuilder) {
        this.sortBuilder = sortBuilder;
        return this;
    }

    /**
     * Get sort builder or <tt>null</tt> if not set
     */
    public SortBuilder getSortBuilder() {
        return sortBuilder;
    }

    /**
     * Set a top-level filter that would filter both search results and aggregations
     *
     * @param filter
     * @return this object where filter has been set
     */
    public MarcusSearchBuilder setFilter(FilterBuilder filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Get sort builder or <tt>null</tt> if not set
     **/
    public FilterBuilder getFilter() {
        return filter;
    }


    /**
     * Set a post_filter that would affect only search results but NOT aggregations.
     * You would use this on "OR" terms aggregations
     *
     * @param filter
     * @return this object where post_filter has been set
     */
    public MarcusSearchBuilder setPostFilter(FilterBuilder filter) {
        this.postFilter = filter;
        return this;
    }

    /**
     * Get post filter or <tt>null</tt> if not set
     */
    public FilterBuilder getPostFilter() {
        return postFilter;
    }

    /**
     * Set a selected filters map so that we can build filters out of them
     *
     * @param selectedFacets selected filters
     * @return this object where a date range filter has been set
     */
    public MarcusSearchBuilder setSelectedFacets(Map<String, List<String>> selectedFacets) {
        if (selectedFacets != null && !selectedFacets.isEmpty()) {
            this.selectedFacets = selectedFacets;
        }
        return this;
    }

    /**
     * Set index that need to be boosted
     * @param indexToBoost
     * @return
     */
    public MarcusSearchBuilder setIndexToBoost(String indexToBoost) {
        this.indexToBoost = indexToBoost;
        return this;
    }

    public String getIndexToBoost(){
        return indexToBoost;
    }

    /**
     * Get selected filters or <tt>null</tt> if not set
     * @return a map containing selected filters
     */
    public Map<String, List<String>> getSelectedFacets() {
        return selectedFacets;
    }

    /**
     * Get all documents based on the service settings.
     *
     * @return a SearchResponse, can be <code>null</code>, which means search was not successfully executed.
     */
    @Override
    @Nullable
    public SearchResponse executeSearch() {
        SearchResponse response = null;
        try {
            response = constructSearchRequest().execute().actionGet();
            //Show response for debugging purpose
            //logger.info(response.toString());
        }
           catch (SearchPhaseExecutionException e) {
            //I've not found a direct way to validate a query string. Therefore, the idea here is to catch any
            //exception that is related to search execution.
            logger.error("Could not execute search: " + e.getDetailedMessage());
        }
        return response;
    }


    /**
     * Construct search request based on the service settings.
     **/
    @Override
    public SearchRequestBuilder constructSearchRequest() {
        QueryBuilder query;
        FunctionScoreQueryBuilder functionScoreQueryBuilder;
        SearchRequestBuilder searchRequest = getClient().prepareSearch();

        try {
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
                functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(
                        QueryUtils.buildQueryString(getQueryString())
                );
            } else {
                //Boost documents inside the "random list" of places because these places have colorful images
                //and hence they beautify the front page.
                //This is just for coolness and it has no effect if the query yields no results
                String randomQueryString = randomList[new Random().nextInt(randomList.length)];
                functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery())
                        .add(FilterBuilders.queryFilter(
                                QueryBuilders.simpleQueryStringQuery(randomQueryString)),
                                ScoreFunctionBuilders.weightFactorFunction(2)
                        );
            }
            //Boost documents of type "Fotografi" for every query performed.
            query = functionScoreQueryBuilder.add(
                    FilterBuilders.termFilter("type", "fotografi"),
                    ScoreFunctionBuilders.weightFactorFunction(3)
            );

            //Set filtered query, whether with or without filter
            if (filter != null) {
                //Note: Filtered query is deprecated from ES v2.0
                //in favour of a new filter clause on the bool query
                //Read https://www.elastic.co/blog/better-query-execution-coming-elasticsearch-2-0
                searchRequest.setQuery(QueryBuilders.filteredQuery(query, filter));
            } else {
                searchRequest.setQuery(query);
            }

            //Set post filter
            if (postFilter != null) {
                searchRequest.setPostFilter(postFilter);
            }

            //Set sortBuilder
            if (sortBuilder != null) {
                searchRequest.addSort(sortBuilder);
            }

            if(indexToBoost != null){
                searchRequest.addIndexBoost(indexToBoost, 4.0f);
            }

            //Append aggregations to the request builder
            if (Strings.hasText(aggregations)) {
                AggregationUtils.addAggregations(
                        searchRequest, aggregations, selectedFacets
                );
            }
            //Set from and size
            searchRequest.setFrom(getFrom());
            searchRequest.setSize(getSize());

            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
        } catch (SearchSourceBuilderException e) {
            logger.error("Exception occurred when building search request: " + e.getMostSpecificCause());
        }
        return searchRequest;
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
                    .field("from", getFrom())
                    .field("size", getSize())
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
        MarcusSearchBuilder service = SearchBuilderFactory.marcusSearch(c);
        service.setAggregations("koba"); //Invalid aggs, it should fail.
        service.setClient(null);
        service.setQueryString("~ana");
        System.out.println(QueryUtils.toJsonString(service.executeSearch(), true));
    }
}

