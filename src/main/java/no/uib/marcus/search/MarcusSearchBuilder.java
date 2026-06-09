package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScoreQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import co.elastic.clients.util.NamedValue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import no.uib.marcus.common.Params;
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
    // loaded at the front page on page load if nothing is specified
    private final String[] randomPictures = {
            "Knud Knudsen",
            "Postkort",
            "Marcus Selmer",
            "Nordnes",
            "Widerøesamlingen",
            "Sophus Tromholt",
            "Nyborg"
    };
    private static final String TYPE = "type";
    private static final int TRACK_HINT_SIZE = 500000;

  private final TrackHits trackHits = new TrackHits.Builder().count(TRACK_HINT_SIZE).build();


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
     * Construct the search request based on the service settings.
     */
    @Override
    public SearchRequest.Builder constructSearchRequest() {
            Query query;
            FunctionScoreQuery.Builder functionScoreQueryBuilder;
            SearchRequest.Builder searchRequest = new SearchRequest.Builder();

            try {
                //Set indices
                if (isNeitherNullNorEmpty(getIndices())) {
                    searchRequest.index(List.of(getIndices()));
                }

                //Set from and size
                searchRequest.from(getFrom());
                searchRequest.size(getSize());

              searchRequest.trackTotalHits(trackHits);
              //Bound query execution time and per-shard collection so a single slow/expensive
              //query can't tie up a shard (H1)
              searchRequest.timeout(Params.SEARCH_TIMEOUT);
              searchRequest.terminateAfter(Params.TERMINATE_AFTER);


              FunctionScore fotoFs = new FunctionScore.Builder().filter(QueryBuilders.term().value(BoostType.FOTOGRAFI).field(TYPE).build()._toQuery()).weight(3.0).build();

              //Set query
                if (StringUtils.hasText(getQueryString())) {
                    //Use query_string query with AND operator
                    functionScoreQueryBuilder = QueryBuilders.functionScore().query(QueryUtils.buildMarcusQueryString(getQueryString()).build()._toQuery());
                } else {
                    //Boost documents inside the "random list" of places because they beautify the front page.
                    //This is just for coolness, and it has no effect if the query yields no results
                    String randomQueryString = randomPictures[ThreadLocalRandom.current().nextInt(randomPictures.length)];
                    functionScoreQueryBuilder = QueryBuilders.functionScore().query(QueryBuilders.matchAll().build()._toQuery()).functions(
                            List.of(fotoFs,new FunctionScore.Builder().filter(QueryBuilders.simpleQueryString().query(randomQueryString).build()._toQuery()).weight(2.0).build()));
                }
                 query = functionScoreQueryBuilder.functions(List.of(fotoFs)).build()._toQuery();
                //Set the filtered query with top_filter
                if (getFilter() != null) {

                  logger.fine("setting filter");
                  BoolQuery filterQuery = getFilter();
                  logger.log(Level.FINE, "sizes: {0}", filterQuery.filter().size());
                  searchRequest
                      .query(QueryBuilders.bool().must(query)
                          .filter(List.of(filterQuery._toQuery())).build()._toQuery());
                } else {
                    searchRequest.query(query);
                }
                //Set post filter if available
                if (getPostFilter() != null) {
                  logger.log(Level.FINE, "postfilter hasClauses: {0}", getPostFilter().hasClauses());
                  searchRequest.postFilter(getPostFilter());
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



            } catch (Exception e) {
                throw new RuntimeException("Marcus search query builder exception:", e);
            }
            return searchRequest;

   // Type to be boosted if nothing is specified

}
  static class BoostType {
    static final String FOTOGRAFI = "fotografi";
    // why english, should it use "nummer" instead?
    static final String ISSUE = "issue";

    private BoostType() {
    }
  }

}

