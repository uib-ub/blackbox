package no.uib.marcus.search;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.*;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;
import no.uib.marcus.common.util.SignatureUtils;

import java.util.*;
import java.util.logging.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryBuilder;

import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

import javax.validation.constraints.NotNull;

import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.weightFactorFunction;


/**
 * Builder for Marcus search service
 *
 * @author Hemed Al Ruwehy
 * @since 0.1
 * 2016-01-24, University of Bergen Library.
 */
public class MarcusSearchBuilder extends AbstractSearchBuilder<MarcusSearchBuilder> {

    private final Logger logger = Logger.getLogger(MarcusSearchBuilder.class.getName());

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
     * @param restHighLevelClient Elasticsearch client to communicate with a cluster.
     */
    public MarcusSearchBuilder(@NotNull RestHighLevelClient restHighLevelClient) {
        super(restHighLevelClient);
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
    public co.elastic.clients.elasticsearch.core.SearchRequest.Builder constructSearchRequest() {
        QueryBuilder query;
        FunctionScoreQueryBuilder functionScoreQueryBuilder;
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        SearchRequest.Builder searchRequest = new SearchRequest.Builder();

        try {
            //Set indices
            if (isNeitherNullNorEmpty(getIndices())) {
                searchRequest.index(Arrays.asList(getIndices()));
            }

            //Set types
         //   if (isNeitherNullNorEmpty(getTypes())) {
        //        searchRequest.setTypes(getTypes());
        //    }

            //Set from and size
            sourceBuilder.from(getFrom());
            sourceBuilder.size(getSize());

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

                sourceBuilder.query(
                        QueryBuilders.boolQuery().filter(getFilter()).must(QueryBuilders.functionScoreQuery(functions)));
            } else {
                sourceBuilder.query(QueryBuilders.functionScoreQuery(functions));
            }
            //Set post filter if available
            if (getPostFilter() != null) {
                sourceBuilder.query(getPostFilter());
            }
            //Set index to boost
            if (getIndexToBoost() != null) {
                sourceBuilder.indexBoost(getIndexToBoost(), 4.0f);
            }
            //Set options
            if (getSortBuilder() != null) {
                sourceBuilder.sort(getSortBuilder());
            }
            //Append aggregations to the request builder
            if (Strings.hasText(getAggregations())) {
                AggregationUtils.addAggregations(sourceBuilder, getAggregations(), getSelectedFacets());
            }

            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
            //System.out.println(searchRequest.toString());
        } catch (SearchSourceBuilderException e) {
            logger.severe("Exception occurred when building search request: " + e.getDetailedMessage());
        }
        SearchRequest r = searchRequest.source();

        return r;
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
            logger.severe(e.getDetailedMessage());
        }
        return optionalResponse;
    }

    //Type to be boosted if nothing is specified
    static class BoostType {
        final static String FOTOGRAFI = "fotografi";
        final static String BILDE = "bilde";
    }
}


