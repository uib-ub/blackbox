package no.uib.marcus.search;

import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.queryparser.xml.QueryBuilderFactory;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;
import org.elasticsearch.search.highlight.HighlightBuilder;

public class NaturenSearchBuilder extends MarcusSearchBuilder {

    private final Logger logger = Logger.getLogger(getClass().getName());

    NaturenSearchBuilder(Client client) {
        super(client);
    }


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
                if (isNeitherNullNorEmpty(getTypes())) {
                    searchRequest.setTypes(getTypes());
                }

                //Set query
                if (Strings.hasText(getQueryString())) {
                    //Use query_string query with AND operator
                    functionScoreQueryBuilder = QueryBuilders
                            .functionScoreQuery(QueryUtils.buildMarcusQueryString(getQueryString()));
                } else {
                    // Search only if type is not specified
                    if(getTypes() == null || getTypes().length == 0) {
                        searchRequest.setTypes("issue"); // show only issues
                    }
                    functionScoreQueryBuilder = QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery())
                            //.add(FilterBuilders.)
                            .add(FilterBuilders.existsFilter("hasThumbnail"), ScoreFunctionBuilders.weightFactorFunction(2));
                }

                //Boost documents of type "Fotografi" for every query performed.
                query = functionScoreQueryBuilder.add(
                        FilterBuilders.termFilter("type", "issue"),
                        ScoreFunctionBuilders.weightFactorFunction(3)
                );

                //Set filtered query, whether with or without filter
                if (getFilter() != null) {
                    //Note: Filtered query is deprecated from ES v2.0
                    //in favour of a new filter clause on the bool query
                    //Read https://www.elastic.co/blog/better-query-execution-coming-elasticsearch-2-0
                    searchRequest.setQuery(QueryBuilders.filteredQuery(query, getFilter()));
                } else {
                    searchRequest.setQuery(query);
                }

                //Set post filter
                if (getPostFilter() != null) {
                    searchRequest.setPostFilter(getPostFilter());
                }

                //Set sortBuilder
                if (getSortBuilder() != null) {
                    searchRequest.addSort(getSortBuilder());
                }

                if (getIndexToBoost() != null) {
                    searchRequest.addIndexBoost(getIndexToBoost(), 4.0f);
                }

                //Append aggregations to the request builder
                if (Strings.hasText(getAggregations())) {
                    AggregationUtils.addAggregations(searchRequest, getAggregations(), getSelectedFacets());
                }
                //Set from and size
                searchRequest.setFrom(getFrom());
                searchRequest.setSize(getSize());

                //Set highlighting
                searchRequest.addHighlightedField("hasTextContent")
                        // .setHighlighterNoMatchSize(0) // if no match, return these characters, 0 means return all
                        // .setHighlighterFragmentSize(500)
                        // .setHighlighterNumOfFragments(0) //fragments to return. 0 means return everything
                        .setHighlighterPreTags("<em class='highlight'>")
                        .setHighlighterPostTags("</em>");

                //Show builder for debugging purpose
                //logger.info(searchRequest.toString());
                System.out.println(searchRequest.toString());
            } catch (SearchSourceBuilderException e) {
                logger.error("Exception occurred when building search request: " + e.getMostSpecificCause());
            }
            return searchRequest;
        }

}
