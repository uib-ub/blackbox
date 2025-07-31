package no.uib.marcus.search;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.SignatureUtils;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import no.uib.marcus.common.util.StringUtils;

/**
 * A custom search builder for WAB
 */
public class WabSearchBuilder extends AbstractSearchBuilder<WabSearchBuilder> {
    private final Logger logger = Logger.getLogger(getClass().getName());

    WabSearchBuilder(ElasticsearchClient client) {
        super(client);
    }

    /**
     * Appends leading wildcard if query string is WAB signature
     */
    @Override
    public String getQueryString() {
        return SignatureUtils.appendLeadingWildcardIfWABSignature(super.getQueryString());
    }

    /**
     * Builds query for WAB
     */
    @Override
    public SearchRequest.Builder constructSearchRequest() {
        Query query;
        SearchRequest.Builder searchRequest = new SearchRequest.Builder();
        try {
            //Set indices
            if (isNeitherNullNorEmpty(getIndices())) {
                logger.info("Setting indices to " + Arrays.asList(getIndices()));
                searchRequest.index(Arrays.asList(getIndices()));
            }


            //Set query
            if (StringUtils.hasText(getQueryString())) {
                logger.info("query set for wab");
                query = QueryBuilders.simpleQueryString()
                        .query(getQueryString())
                        .defaultOperator(Operator.And)
                        .fields(List.of("label",
                                        "publishedIn",
                                        "publishedInPart.exact",
                                        "all"
                                        )).build()._toQuery();//whitespace analyzed

            } else {
                query = QueryBuilders.matchAll().build()._toQuery();
            }
            FunctionScoreQuery.Builder functions = new FunctionScoreQuery.Builder() ;
            // @todo find out
            // Set Query, whether with or without filter
            if (getFilter() != null) {
                logger.info("setting filter");
                BoolQuery filterQuery = getFilter().build();
                logger.info("compare if filterQuery list is the same as filter() method" + Boolean.toString(filterQuery.filter().equals(List.of(filterQuery._toQuery()))));
                logger.info("sizes: " + filterQuery.filter().size() + " " + List.of(filterQuery._toQuery()).size());
                searchRequest.query(QueryBuilders.bool().must(query).filter(List.of(filterQuery._toQuery())).build()._toQuery());
            } else {
                searchRequest.query(query);
            }
            //Set post filter
            if (getPostFilter() != null) {

                logger.info("setting post filter" + getPostFilter().hasClauses());
                searchRequest.postFilter(getPostFilter().build());
            }
            //Set sortBuilder
            if (getSortBuilder() != null) {
                logger.info("setting sort builder");
                searchRequest.sort(List.of(getSortBuilder().build()));
            }
            //Append aggregations to the request builder
            if (StringUtils.hasText(getAggregations())) {
                logger.info("adding aggreations");
                SearchRequest.Builder aggRequest = AggregationUtils.addAggregations(searchRequest, getAggregations(), getSelectedFacets());
                searchRequest = aggRequest;
            }

            //Set from and size
            searchRequest.from(getFrom());
            searchRequest.size(getSize());

            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
            logger.info("end of wab implementation searchrequest builder" + searchRequest.toString());
        }  catch (IllegalStateException e) {
            throw new RuntimeException(e);
        }
        return searchRequest;
    }
}

