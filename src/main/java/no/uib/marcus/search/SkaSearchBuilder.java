package no.uib.marcus.search;

import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

/**
 * Building a search service for Skeivt archive dataset
 *
 * @author Hemed Al Ruwehy
 */
public class SkaSearchBuilder extends MarcusSearchBuilder {
    private final Logger logger = Logger.getLogger(getClass().getName());

    SkaSearchBuilder(Client client) {
        super(client);
    }

    /**
     * Construct a specific search request for SkA dataset
     **/
    @Override
    public SearchRequestBuilder constructSearchRequest() {
        QueryBuilder query;
        SearchRequestBuilder searchRequest = getClient().prepareSearch();
        try {

            //Set indices
            if (isNeitherNullNorEmpty(getIndices())) {
                searchRequest.setIndices(getIndices());
            }

            //Set types
            if (isNeitherNullNorEmpty(getTypes())) {
                searchRequest.setTypes(getTypes());
            }
            //Set query
            if (Strings.hasText(getQueryString())) {
                query = QueryUtils.buildMarcusQueryString(getQueryString());
            } else {
                //Boost documents of type "skeivopedia" if nothing specified
                query = QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery()).add(
                        FilterBuilders.termFilter("type", "skeivopedia"),
                        ScoreFunctionBuilders.weightFactorFunction(2));
            }
            //Set query whether with or without filter
            if (getFilter() != null) {
                searchRequest.setQuery(QueryBuilders.filteredQuery(query, getFilter()));
            } else {
                searchRequest.setQuery(query);
            }

            //Set post filter
            if (getPostFilter() != null) {
                searchRequest.setPostFilter(getPostFilter());
            }

            //Set from and size
            searchRequest.setFrom(getFrom());
            searchRequest.setSize(getSize());

            //Set sortBuilder
            if (getSortBuilder() != null) {
                searchRequest.addSort(getSortBuilder());
            }
            //Boost specific index
            if (getIndexToBoost() != null) {
                searchRequest.addIndexBoost(getIndexToBoost(), 5.0f);
            }
            //Append aggregations to the request builder
            if (Strings.hasText(getAggregations())) {
                AggregationUtils.addAggregations(searchRequest, getAggregations(), getSelectedFacets());
            }
            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
        } catch (SearchSourceBuilderException e) {
            logger.error("Exception occurred when building search request: " + e.getMostSpecificCause());
        }
        return searchRequest;
    }

}
