package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.util.NamedValue;
import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;
import no.uib.marcus.common.util.SignatureUtils;

import java.util.*;
import java.util.logging.Logger;

import no.uib.marcus.common.util.StringUtils;

import jakarta.validation.constraints.NotNull;


/**
 * Builder for Marcus search service
 *
 * @author Hemed Al Ruwehy
 * @since 0.1
 * 2016-01-24, University of Bergen Library.
 */
public class MarcusSearchBuilder extends AbstractSearchBuilder<MarcusSearchBuilder> {

    private final Logger logger = Logger.getLogger(getClass().getName());

    //A list of images that will be randomly
    // loaded at the front page on page load, if nothing is specified
    private final String[] randomPictures = {
            "Knud Knudsen",
            "Postkort",
            "Marcus Selmer",
            "Nordnes",
            "Widerøesamlingen",
            "Sophus Tromholt",
            "Nyborg"
    };

    /**
     * Build Marcus search service
     *
     * @param client Elasticsearch client to communicate with a cluster.
     */
    public MarcusSearchBuilder(@NotNull ElasticsearchClient client) {
        super(client);
    }

    /**
     * Appends wildcard to the query string if it is a valid UBB signature
     */
    @Override
    public String getQueryString() {
        return SignatureUtils.appendWildcardIfUBBSignature(super.getQueryString());
    }

    /**
     * Construct search request based on the service settings.
     */
    @Override
    public SearchRequest.Builder constructSearchRequest() {
        {
            Query query;
            FunctionScoreQuery.Builder functionScoreQueryBuilder;
            SearchRequest.Builder searchRequest = new SearchRequest.Builder();

            try {
                //Set indices
                if (isNeitherNullNorEmpty(getIndices())) {
                    searchRequest.index(List.of(getIndices()));
                }

                //Set types
                // removed in ES 2.X
                //    if (isNeitherNullNorEmpty(getTypes())) {
                //         searchRequest.setTypes(getTypes());
                //     }

                //Set from and size
                searchRequest.from(getFrom());
                searchRequest.size(getSize());

                //Set query
                if (StringUtils.hasText(getQueryString())) {
                    //Use query_string query with AND operator
                    functionScoreQueryBuilder = QueryBuilders.functionScore().query(QueryUtils.buildMarcusQueryString(getQueryString()).build()._toQuery());


                } else {
                    //Boost documents inside the "random list" of places because they beautify the front page.
                    //This is just for coolness and it has no effect if the query yields no results
                    String randomQueryString = randomPictures[new Random().nextInt(randomPictures.length)];
                    functionScoreQueryBuilder = QueryBuilders.functionScore().query(QueryBuilders.matchAll().build()._toQuery()).functions(
                            List.of(new FunctionScore.Builder().filter(QueryBuilders.simpleQueryString().query(randomQueryString).build()._toQuery()).weight(2.0).build()));
                }
              query = functionScoreQueryBuilder.build()._toQuery();
              //Boost documents of type "Fotografi" for every query performed.
                // @todo ? reimplement boost
                //  query = functionScoreQueryBuilder.functions(List.of(new FunctionScore.Builder().filter(
                //          QueryBuilders.terms().field("type").terms(FOTOGRAFI)
                //          )))
                //          .add(FilterBuilders.termFilter("type", BoostType.FOTOGRAFI), weightFactorFunction(3))
                //          .add(FilterBuilders.termFilter("type", BoostType.BILDE), weightFactorFunction(3));

                //Set filtered query with top_filter
                if (getFilter() != null) {

                  logger.fine("setting filter");
                  BoolQuery filterQuery = getFilter().build();
                  logger.fine("compare if filterQuery list is the same as filter() method" + Boolean.toString(filterQuery.filter().equals(List.of(filterQuery._toQuery()))));
                  logger.fine("sizes: " + filterQuery.filter().size() + " " + List.of(filterQuery._toQuery()).size());
                  searchRequest
                      .query(QueryBuilders.bool().must(query)
                          .filter(List.of(filterQuery._toQuery())).build()._toQuery());
                    //Note: Filtered query is deprecated from ES v2.0
                    //in favour of a new filter clause on the bool query
                    //Read https://www.elastic.co/blog/better-query-execution-coming-elasticsearch-2-0
                //@todo    searchRequest.query(QueryBuilders.bool().filter(List.of(getFilter().build())).build()._toQuery());
                } else {
                    searchRequest.query(query);
                }
                //Set post filter if available
                if (getPostFilter() != null) {
                    searchRequest.postFilter(getPostFilter().build());
                }
                //Set index to boost
                if (getIndexToBoost() != null) {
                    searchRequest.indicesBoost(NamedValue.of(getIndexToBoost(), 5.0));
                }


                //Set options
                if (getSortBuilder() != null) {
                    searchRequest.sort(List.of(getSortBuilder().build()));
                }
                //Append aggregations to the request builder
                if (StringUtils.hasText(getAggregations())) {
                    AggregationUtils.addAggregations(searchRequest, getAggregations(), getSelectedFacets());
                }

                //Show builder for debugging purpose
                //logger.info(searchRequest.toString());
                //System.out.println(searchRequest.toString());

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return searchRequest;
        }
    //Type to be boosted if nothing is specified
 //   static class BoostType {
  //      final static String FOTOGRAFI = "fotografi";
  //      final static String BILDE = "bilde";
  //  }
}
}

