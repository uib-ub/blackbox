package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.util.NamedValue;
import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import no.uib.marcus.common.util.StringUtils;

/**
 * Building a search service for Skeivtarkiv
 *
 * @author Hemed Al Ruwehy
 */
public class SkaSearchBuilder extends MarcusSearchBuilder {
    private final Logger logger = Logger.getLogger(getClass().getName());

    SkaSearchBuilder(ElasticsearchClient client) {
        super(client);
    }

    /**
     * Construct a specific search request for SkA dataset
     **/
    @Override
    public SearchRequest.Builder constructSearchRequest() {
        Query query;
        SearchRequest.Builder searchRequest = new SearchRequest.Builder();
        try {

            //Set indices
            if (isNeitherNullNorEmpty(getIndices())) {
                searchRequest.index(List.of(getIndices()));
            }

            // types are removed from elasticsearch 2
            //    if (isNeitherNullNorEmpty(getTypes())) {
            //        searchRequest.setTypes(getTypes());
            //    }
            //Set query
            if (StringUtils.hasText(getQueryString())) {
                query = QueryBuilders.simpleQueryString()
                        .query(getQueryString()).build()._toQuery();
            } else {
                //Boost documents of type "Manuskript" if nothing specified
                query = QueryBuilders.functionScore().functions(List.of(new FunctionScore.Builder().weight(2.0).build())).query(QueryBuilders.matchAll().build()._toQuery()).build().query();

            }
            //Set query whether with or without filter
            if (getFilter() != null) {
                searchRequest.query(QueryBuilders.bool().filter(List.of(getFilter().build())).build()._toQuery());
            } else {
                searchRequest.query(query);
            }

            //Set post filter
            if (getPostFilter() != null) {
                searchRequest.postFilter(getPostFilter().build());
            }

            //Set from and size
            searchRequest.from(getFrom());
            searchRequest.size(getSize());

            //Set sortBuilder
            if (getSortBuilder() != null) {
                searchRequest.sort(List.of(getSortBuilder().build()));
            }
            //Boost specific index
            if (getIndexToBoost() != null) {
                searchRequest.indicesBoost(NamedValue.of(getIndexToBoost(), 5.0));
            }
            //Append aggregations to the request builder
            if (StringUtils.hasText(getAggregations())) {
                AggregationUtils.addAggregations(searchRequest, getAggregations(), getSelectedFacets());
            }
            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
        } finally {

        }
        return searchRequest;
    }

    //Type to be boosted if nothing is specified
    static class BoostType {
        final static String INTERVJU = "intervju";
    }

}
