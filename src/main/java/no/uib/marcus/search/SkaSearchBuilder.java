package no.uib.marcus.search;

import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

/**
 * Bulding a search service for Skeivt archive dataset
 * @author  Hemed Al Ruwehy
 */
public class SkaSearchBuilder extends MarcusSearchBuilder {
    private final Logger logger = Logger.getLogger(getClass().getName());


    SkaSearchBuilder(Client client){
        super(client);
    }

    /**
     * Get documents based on the service settings
     **/
    @Override
    @Nullable
    public SearchResponse getDocuments() {
        QueryBuilder query;
        SearchResponse response = null;
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
                query = QueryUtils.buildQueryString(getQueryString());
            } else {
                //Boost documents of type "skeivopedia" if nothing specified
                query = QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery()).add(
                        FilterBuilders.termFilter("type", "skeivopedia"),
                        ScoreFunctionBuilders.weightFactorFunction(2));
            }
            //Set Query, whether with or without filter
            if (getFilter() != null) {
                searchRequest.setQuery(QueryBuilders.filteredQuery(query, getFilter()));
                if (getPostFilter() != null) {
                    searchRequest.setPostFilter(getPostFilter());
                }
            } else if (getPostFilter() != null && getFilter() == null) {
                searchRequest.setQuery(query).setPostFilter(getPostFilter());
            } else {
                searchRequest.setQuery(query);
            }
            //Set from and size
            searchRequest.setFrom(getFrom());
            searchRequest.setSize(getSize());

            //Set sortBuilder
            if (getSortBuilder() != null) {
                searchRequest.addSort(getSortBuilder());
            }
            //Append aggregations to the request builder
            if (Strings.hasText(getAggregations())) {
                AggregationUtils.addAggregations(searchRequest, getAggregations(), getSelectedFacets());
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
            logger.error("Could not execute search: " + e.getDetailedMessage());
        }
        return response;
    }
}
