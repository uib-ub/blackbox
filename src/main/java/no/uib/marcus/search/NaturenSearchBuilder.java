package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.util.NamedValue;
import java.util.logging.Level;
import no.uib.marcus.common.util.QueryUtils;

import java.util.List;
import java.util.logging.Logger;
import no.uib.marcus.common.util.StringUtils;
/***
 * A search builder for naturen
 * @author Hemed Al Ruwehy
 */
public class NaturenSearchBuilder extends MarcusSearchBuilder {

    private final Logger logger = Logger.getLogger(getClass().getName());

    NaturenSearchBuilder(ElasticsearchClient client) {
        super(client);
    }

    @Override
    public SearchRequest.Builder constructSearchRequest() {
        Query query;
        SearchRequest.Builder searchRequest = super.constructSearchRequest();

            //Set query
            if (StringUtils.hasText(getQueryString())) {
                query = QueryUtils.buildMarcusQueryString(getQueryString()).build()._toQuery();
            } else {
                query = QueryBuilders.functionScore().functions(List.of(new FunctionScore.Builder().filter(QueryBuilders.term().value(BoostType.ISSUE).field("type").build()).filter(QueryBuilders.exists().field("hasThumbnail").build()._toQuery()).build())).build()._toQuery();
                }

            //Set filtered query, whether with or without filter
            if (getFilter() != null) {

              BoolQuery filterQuery = getFilter().build();
              logger.log(Level.FINE, "sizes: {0}", filterQuery.filter().size());
              searchRequest
                  .query(QueryBuilders.bool().must(query)
                      .filter(List.of(filterQuery._toQuery())).build()._toQuery());            } else {
                searchRequest.query(query);
            HighlightField.Builder highlightFieldBuilder = new HighlightField.Builder();
            highlightFieldBuilder.postTags("</em>").preTags("<em class='txt-highlight'>");
            Highlight.Builder highlightBuilder = new Highlight.Builder();
            highlightBuilder.fields(new NamedValue<>("textContent", highlightFieldBuilder.build()));
            searchRequest.highlight(highlightBuilder.build());
        }
        return searchRequest;
    }
}

