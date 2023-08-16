package no.uib.marcus.search;


import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import no.uib.marcus.common.util.AggregationUtils;
import no.uib.marcus.common.util.SignatureUtils;

import java.util.Arrays;
import java.util.logging.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;

/**
 * A custom search builder for WAB
 */
public class WabSearchBuilder extends AbstractSearchBuilder<WabSearchBuilder> {
    private final Logger logger = Logger.getLogger(WabSearchBuilder.class.getName());

    WabSearchBuilder(Client client) {
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
        Query.Builder query = new Query.Builder();

        SearchRequest.Builder searchRequest = new SearchRequest.Builder();
        try {
            //Set indices
            if (isNeitherNullNorEmpty(getIndices())) {
                searchRequest.index(Arrays.asList(getIndices())).;
            }
            //Set types
            if (isNeitherNullNorEmpty(getTypes())) {
      //          searchRequest.setTypes(getTypes());
            }

            //Set query
            if (Strings.hasText(getQueryString())) {
                query = query.match(t -> t
                        .field("label")//whitespace analyzed
                        .field("publishedIn")//whitespace analyzed
                        .field("publishedInPart.exact")//not_analyzed
                        //.field("hasPart")
                        //.field("refersTo")
                        .field("_all");

                searchRequest.query(query.build()).defaultOperator(Operator.And);


            } else {
                Query.Builder qb =  new Query.Builder();

                searchRequest.query(qb.matchAll()QueryBuilders.matchAllQuery().toQuery() )
            }
            //Set Query, whether with or without filter
            if (getFilter() != null) { // @todo
            //    searchRequest.setQuery(Queries.filtered(query.)toQuery(), getFilter()));
            } else {
                searchRequest.query(query);
            }
            //Set post filter
            if (getPostFilter() != null) {
                searchRequest.setPostFilter(getPostFilter());
            }
            //Set sortBuilder
            if (getSortBuilder() != null) {
                searchRequest.addSort(getSortBuilder());
            }
            //Append aggregations to the request builder
            if (Strings.hasText(getAggregations())) {
                AggregationUtils.addAggregations(searchRequest, getAggregations(), getSelectedFacets());
            }

            //Set from and size
            searchRequest.setFrom(getFrom());
            searchRequest.setSize(getSize());

            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
        } catch (SearchSourceBuilderException e) {
            logger.severe("Exception occurred when building search request: " + e.getDetailedMessage());
        }
        return searchRequest;
    }
}

