package no.uib.marcus.search;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.util.ObjectBuilder;
import com.google.gson.JsonParseException;
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
                searchRequest.index(Arrays.asList(getIndices()));
            }


            //Set query
            if (StringUtils.hasText(getQueryString())) {
                query = QueryBuilders.simpleQueryString()
                        .query(getQueryString())
                        .defaultOperator(Operator.And)
                        .fields(List.of("label",
                                        "publishedIn",
                                        "publishedInPart.exact",
                                        "_all")).build()._toQuery();//whitespace analyzed

            } else {
                query = QueryBuilders.matchAll().build()._toQuery();
            }
            FunctionScoreQuery.Builder functions = new FunctionScoreQuery.Builder() ;
            // @todo find out
            // Set Query, whether with or without filter
            if (getFilter() != null) {
                searchRequest.query(QueryBuilders.bool().filter(List.of(getFilter().build())).must( query).build()._toQuery());
            } else {
                searchRequest.query(query);
            }
            //Set post filter
            if (getPostFilter() != null) {
                searchRequest.postFilter(getPostFilter().build());
            }
            //Set sortBuilder
            if (getSortBuilder() != null) {
                searchRequest.sort(List.of(getSortBuilder().build()));
            }
            //Append aggregations to the request builder
            if (StringUtils.hasText(getAggregations())) {
                AggregationUtils.addAggregations(searchRequest, getAggregations(), getSelectedFacets());
            }

            //Set from and size
            searchRequest.from(getFrom());
            searchRequest.size(getSize());

            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
        } catch (JsonParseException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            throw new RuntimeException(e);
        }
        return searchRequest;
    }
}

