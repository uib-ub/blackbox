package no.uib.marcus.search;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.TrackHits;
import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.QueryUtils;
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
    private final TrackHits trackHits = new TrackHits.Builder().count(Integer.parseInt("100000")).build();

    WabSearchBuilder(ElasticsearchClient client) {
        super(client);
    }

    @Override
    public String getQueryString() {
        String query = super.getQueryString();
        if (!StringUtils.containsWhitespace(query)) {
            return SignatureUtils.appendWildcardIfWABSignature(query);
        }
        // Multi-word: apply per token so signatures get wildcards in the same
        // way as single-word queries (e.g. "Ms-132 Modell" → "Ms-132* Modell").
        StringBuilder result = new StringBuilder();
        String[] tokens = query.trim().split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            if (i > 0) result.append(' ');
            result.append(SignatureUtils.appendWildcardIfWABSignature(tokens[i]));
        }
        return result.toString();
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
                logger.fine("Setting indices to " + Arrays.asList(getIndices()));
                searchRequest.index(Arrays.asList(getIndices()));
            }


            //Set query
            if (StringUtils.hasText(getQueryString())) {
                logger.fine("query set for wab");
                query = QueryUtils.buildWabQueryString(getQueryString()).build()._toQuery();
            } else {
                query = QueryBuilders.matchAll().build()._toQuery();
            }
            // Set Query, whether with or without filter
            if (getFilter() != null) {
                logger.fine("setting filter");
                BoolQuery filterQuery = getFilter().build();
                logger.fine("compare if filterQuery list is the same as filter() method" + Boolean.toString(filterQuery.filter().equals(List.of(filterQuery._toQuery()))));
                logger.fine("sizes: " + filterQuery.filter().size() + " " + List.of(filterQuery._toQuery()).size());
                searchRequest
                    .query(QueryBuilders.bool().must(query)
                        .filter(List.of(filterQuery._toQuery())).build()._toQuery());
            } else {
                searchRequest.query(query);
            }
            //Set post-filter
            if (getPostFilter() != null) {

                logger.fine("setting post filter" + getPostFilter().hasClauses());
                searchRequest.postFilter(getPostFilter().build());
            }
            //Set sortBuilder
            if (getSortBuilder() != null) {
                logger.fine("setting sort builder");
                searchRequest.sort(List.of(getSortBuilder().build()));
            }
            //Append aggregations to the request builder
            if (StringUtils.hasText(getAggregations())) {
                logger.fine("adding aggregations");
                searchRequest = AggregationUtils.addAggregations(searchRequest, getAggregations(), getSelectedFacets());
            }

            //Set from and size
            searchRequest.from(getFrom());
            searchRequest.size(getSize());

            searchRequest.trackTotalHits(trackHits);

            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
            logger.fine("end of wab implementation searchrequest builder" + searchRequest);
        }  catch (IllegalStateException e) {
            throw new RuntimeException(e);
        }
        return searchRequest;
    }
}

