package no.uib.marcus.search;


import no.uib.marcus.common.util.AggregationUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

public class WabSearchBuilder extends MarcusSearchBuilder {
    private final Logger logger = Logger.getLogger(getClass().getName());

    WabSearchBuilder(Client client) {
        super(client);
    }

    @Override
    public SearchRequestBuilder constructSearchRequest() {
        QueryBuilder query;
        SearchRequestBuilder searchRequest = getClient().prepareSearch();
        try {

            //Set indices
            if (getIndices() != null && getIndices().length > 0) {
                searchRequest.setIndices(getIndices());
            }
            //Set types
            if (getTypes() != null && getTypes().length > 0) {
                searchRequest.setTypes(getTypes());
            }

            //Set query
            if (Strings.hasText(getQueryString())) {
                query = QueryBuilders.simpleQueryStringQuery(getQueryString())
                        .field("label")//whitespace analyzed
                        .field("publishedIn")//whitespace analyzed
                        //.field("hasPart")
                        //.field("refersTo")
                        .field("_all")
                        .defaultOperator(SimpleQueryStringBuilder.Operator.AND);
            } else {
                query = QueryBuilders.matchAllQuery();
            }
            //Set Query, whether with or without filter
            if (getFilter() != null) {
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
            logger.error("Exception occurred when building search request: " + e.getDetailedMessage());
        }
        return searchRequest;
    }
}
