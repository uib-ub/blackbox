package no.uib.marcus.search;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import no.uib.marcus.client.ClientFactory;
import no.uib.marcus.common.Settings;
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

    private static final Logger logger = Logger.getLogger(MarcusSearchBuilder.class);
    private FilterBuilder filter;
    private FilterBuilder postFilter;
    private Map<String, List<String>> selectedFacets;
    private String aggregations;
    private SortBuilder sortBuilder;
    private int from = -1;
    private int size = -1;

    /**
     * Constructor
     *
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
     * Set aggregations to be applied
     *
     * @param aggregations a JSON string of aggregations
     * @return this object where aggregations have been set
     */
    public MarcusSearchBuilder setAggregations(String aggregations) {
        if (aggregations != null && isValidJSONArray(aggregations)) {
            this.aggregations = aggregations;
        }
        return this;
    }

    /**
     * Set from, a start of a document, default to 0
     *
     * @param from
     * @return this object where from is set
     */
    public MarcusSearchBuilder setFrom(int from) {
        if (from >= 0) {
            this.from = from;
        }
        return this;
    }

    /**
     * Set how many documents to be returned.
     *
     * @param size
     * @return this object where size has been set
     */
    public MarcusSearchBuilder setSize(int size) {
        if (size >= 0) {
            this.size = size;
        }
        return this;
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
     * Set a selected filters map so that we can build filters out of them
     *
     * @param selectedFacets selected filters
     * @return this object where a date range filter has been set
     */
    public MarcusSearchBuilder setSelectedFacets(Map<String, List<String>> selectedFacets) {
        if (!selectedFacets.isEmpty() && selectedFacets != null) {
            this.selectedFacets = selectedFacets;
        }
        return this;
    }

    /**
     * Validate aggregations
     **/
    private boolean isValidJSONArray(String jsonString) {
        JsonElement element = new JsonParser().parse(jsonString);
        if (!element.isJsonArray()) {
            throw new IllegalParameterException(
                    "Aggregations must be valid JSON. Expected JSON Array of objects but found : [" + jsonString + "]");
        }
        return true;
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
                functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(
                        QueryUtils.buildQueryString(getQueryString())
                );
            } else {
                //Boost documents inside the "random list" of places because these places have colorful images
                // and hence they beautify the front page.
                //This is just for coolness and it has no effect if the query yields no results
                String randomQueryString = Settings.randomList[new Random().nextInt(Settings.randomList.length)];
                functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery())
                        .add(FilterBuilders.queryFilter(
                                QueryBuilders.simpleQueryStringQuery(randomQueryString)), ScoreFunctionBuilders.weightFactorFunction(2)
                        );
            }
            //Boost documents of type "Fotografi" for every query performed.
            query = functionScoreQueryBuilder.add(
                    FilterBuilders.termFilter("type", "fotografi"), ScoreFunctionBuilders.weightFactorFunction(3)
            );

            //Set Query, whether with or without filter
            if (filter != null) {
                //Note: Filtered query is deprecated from ES v2.0
                //in favour of a new filter clause on the bool query
                //Read https://www.elastic.co/blog/better-query-execution-coming-elasticsearch-2-0
                searchRequest.setQuery(QueryBuilders.filteredQuery(query, filter));
                if (postFilter != null) {
                    searchRequest.setPostFilter(postFilter);
                }
            } else if (filter == null && postFilter != null) {
                searchRequest.setQuery(query);
                searchRequest.setPostFilter(postFilter);
            } else {
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
                AggregationUtils.addAggregations(searchRequest, aggregations, selectedFacets);
            }

            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());

            //Execute the response
            response = searchRequest.execute().actionGet();

            //Show response for debugging purpose
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
        MarcusSearchBuilder service = SearchBuilderFactory.createMarcusSearchService(c);
        service.setAggregations("koba"); //Invalid aggs, it should fail.
        service.setClient(null);
        service.setQueryString("~ana");
        System.out.println(QueryUtils.toJsonString(service.getDocuments(), true));
    }
}

