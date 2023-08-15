package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;
import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;

import java.util.Arrays;
import java.util.logging.Logger;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

/**
 * Building a search service for Skeivtarkiv
 *
 * @author Hemed Al Ruwehy
 */
public class SkaSearchBuilder extends MarcusSearchBuilder {
    private final Logger logger = Logger.getLogger(SkaSearchBuilder.class.getName());

    SkaSearchBuilder(RestClient client) {
        super(client);
    }

    /**
     * Construct a specific search request for SkA dataset
     **/
    @Override
    public Builder constructSearchRequest() {
        QueryBuilder query;
        SearchRequest.Builder searchRequest = new SearchRequest.Builder();
        try {

            //Set indices
            if (isNeitherNullNorEmpty(getIndices())) {
                searchRequest.index(Arrays.asList(getIndices()));
            }

            //Set types
            if (isNeitherNullNorEmpty(getTypes())) {
            //    searchRequest.setTypes(getTypes());
            }
            //Set query
            if (Strings.hasText(getQueryString())) {
                query = QueryUtils.buildMarcusQueryString(getQueryString());
            } else {
                //Boost documents of type "Manuskript" if nothing specified
          //      query = QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery(),).
            //            .add(QueryBuilders.termQuery("type", BoostType.INTERVJU),
              //                  ScoreFunctionBuilders.weightFactorFunction(2));
            }
            //Set query whether with or without filter
            if (getFilter() != null) {
                searchRequest.query(QueryBuilders.boolQuery().filter(getFilter()));
            } else {
          //      searchRequest.setQuery(query);
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
            logger.severe("Exception occurred when building search request: " + e.getRootCause().getMessage());
        }
        return searchRequest;
    }

    //Type to be boosted if nothing is specified
    static class BoostType {
        final static String INTERVJU = "intervju";
    }

}
