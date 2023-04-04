package no.uib.marcus.search;

import static org.elasticsearch.index.query.QueryBuilders.constantScoreQuery;
import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.*;
import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;
import no.uib.marcus.common.util.SignatureUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;

import org.elasticsearch.client.internal.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.*;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryBuilder;

import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.weightFactorFunction;


/**
 * Builder for Marcus search service
 *
 * @author Hemed Al Ruwehy
 * @since 0.1
 * 2016-01-24, University of Bergen Library.
 */
public class MarcusSearchBuilder extends AbstractSearchBuilder<MarcusSearchBuilder> {

    private final Logger logger = LogManager.getLogger(getClass().getName());

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
    public MarcusSearchBuilder(@NotNull Client client) {
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
    public SearchRequestBuilder constructSearchRequest() {
        QueryBuilder query;
        FunctionScoreQueryBuilder functionScoreQueryBuilder;
        SearchRequestBuilder searchRequest = getClient().prepareSearch();

        try {
            //Set indices
            if (isNeitherNullNorEmpty(getIndices())) {
                searchRequest.setIndices(getIndices());
            }

            //Set types
         //   if (isNeitherNullNorEmpty(getTypes())) {
        //        searchRequest.setTypes(getTypes());
        //    }

            //Set from and size
            searchRequest.setFrom(getFrom());
            searchRequest.setSize(getSize());

            FunctionScoreQueryBuilder.FilterFunctionBuilder[] functions;

            List functionBuilders = new ArrayList<FunctionScoreQueryBuilder.FilterFunctionBuilder[]>();

            //Set query
            if (Strings.hasText(getQueryString())) {
                //Use query_string query with AND operator
                functionScoreQueryBuilder = functionScoreQuery(QueryUtils.buildMarcusQueryString(getQueryString()));
                functionBuilders.add(functionScoreQueryBuilder);
            } else {
                //Boost documents inside the "random list" of places because they beautify the front page.
                //This is just for coolness and it has no effect if the query yields no results
                String randomQueryString = randomPictures[new Random().nextInt(randomPictures.length)];
                FunctionScoreQueryBuilder.FilterFunctionBuilder matchAll =  new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchAllQuery(),randomFunction());
                FunctionScoreQueryBuilder randomImageFactor = functionScoreQuery(QueryBuilders.simpleQueryStringQuery(randomQueryString),
                        weightFactorFunction(2));
                functionBuilders.add(matchAll);
                functionBuilders.add(randomImageFactor);

            }

                FunctionScoreQueryBuilder boostFotografi = functionScoreQuery(QueryBuilders.termQuery("type", BoostType.FOTOGRAFI),weightFactorFunction(3));
                FunctionScoreQueryBuilder boostBilde = functionScoreQuery(QueryBuilders.termQuery("type", BoostType.BILDE),weightFactorFunction(3));
          //  constantScoreQuery()
           // functionScoreQuery();
            // query = functionsScoreQuery(functionBuilders);

                functionBuilders.add(boostFotografi);
                functionBuilders.add(boostBilde);

                functions = new FunctionScoreQueryBuilder.FilterFunctionBuilder[functionBuilders.size()];

                functions = (FunctionScoreQueryBuilder.FilterFunctionBuilder[]) functionBuilders.toArray(functions);

            //Set filtered query with top_filter
            if (getFilter() != null) {
                //Note: Filtered query is deprecated from ES v2.0
                //in favour of a new filter clause on the bool query
                //Read https://www.elastic.co/blog/better-query-execution-coming-elasticsearch-2-0
                //  Use the bool query instead with a must clause for the query and a filter clause for the filter.
                QueryBuilders.boolQuery().filter(getFilter()).must();
                searchRequest.setQuery(
                        QueryBuilders.boolQuery().filter(getFilter()).must());
            } else {
                searchRequest.setsetQuery(functions.);
            }
            //Set post filter if available
            if (getPostFilter() != null) {
                searchRequest.setPostFilter(getPostFilter());
            }
            //Set index to boost
            if (getIndexToBoost() != null) {
                searchRequest.addIndexBoost(getIndexToBoost(), 4.0f);
            }
            //Set options
            if (getSortBuilder() != null) {
                searchRequest.addSort(getSortBuilder());
            }
            //Append aggregations to the request builder
            if (Strings.hasText(getAggregations())) {
                AggregationUtils.addAggregations(searchRequest, getAggregations(), getSelectedFacets());
            }

            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
            //System.out.println(searchRequest.toString());
        } catch (SearchSourceBuilderException e) {
            logger.error("Exception occurred when building search request: " + e.getDetailedMessage());
        }
        return searchRequest;
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
            if ( response.getHits().getTotalHits().value > -1) {
                optionalResponse = Optional.of(response);
            }
        } catch (ElasticsearchException e) {
            logger.error(e.getDetailedMessage());
        }
        return optionalResponse;
    }

    //Type to be boosted if nothing is specified
    static class BoostType {
        final static String FOTOGRAFI = "fotografi";
        final static String BILDE = "bilde";
    }
}


