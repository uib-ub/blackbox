package no.uib.marcus.search;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import no.uib.marcus.client.ClientFactory;
import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
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
                         "Aggregations must be valid JSON string. Expected JSON Array of objects but found : ["+ jsonString + "]");
             }
         return true;
     }

    /**
     * Get all documents based on the service settings.
     *
     * @return a SearchResponse
     */
    @Override
    public SearchResponse getDocuments() {
        assert getClient() != null;
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
                //Use simple_query_string query with AND operator
                functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(
                        QueryUtils.getSimpleQueryString(getQueryString()));
            } else {
                //Match all documents
                functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(
                        QueryBuilders.matchAllQuery());
            }
            //Boost documents of type "fotographi" and those that have showWeb=true, if exist
            query = functionScoreQueryBuilder
                    .add(FilterBuilders.termFilter("type", "fotografi"),
                            ScoreFunctionBuilders.weightFactorFunction(2))
                    .add(FilterBuilders.termFilter("showWeb", "true"),
                            ScoreFunctionBuilders.weightFactorFunction(2));


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
            response = searchRequest
                    .execute()
                    .actionGet();
            //logger.info(response.toString());
        } catch (SearchSourceBuilderException se) {
            logger.error("Exception on preparing the request: "
                    + se.getDetailedMessage());
        } catch (ElasticsearchException ex) {
            logger.error(ex.getDetailedMessage());
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
        System.out.println(QueryUtils.toJsonString(service.getDocuments(), true));
    }
}

