package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import co.elastic.clients.util.NamedValue;
import no.uib.marcus.common.util.QueryUtils;

import java.util.List;
import no.uib.marcus.common.util.StringUtils;
/***
 * A search builder for naturen
 * @author Hemed Al Ruwehy
 */
public class NaturenSearchBuilder extends MarcusSearchBuilder {
    private static final Highlight HIGHLIGHT =
        new Highlight.Builder()
        .fields(new NamedValue<>("textContent",
            new HighlightField.Builder()
                .postTags("</em>")
                .preTags("<em class='txt-highlight'>")
                .build()
            ))
        .build();
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
                query = QueryBuilders.functionScore().functions(List.of(
                    new FunctionScore.Builder()
                        .filter( new BoolQuery.Builder()
                            .filter(List.of(QueryBuilders.term()
                                .value(BoostType.ISSUE).field("type")
                                .build()._toQuery(),QueryBuilders.exists()
                                .field("hasThumbnail")
                                .build()
                                ._toQuery()))
                            .build())
                        .weight(2.0)
                        .build()))
                    .build()
                    ._toQuery();
                }
            //Set filtered query, whether with or without filter
            if (getFilter() != null) {

              BoolQuery filterQuery = getFilter().build();
              searchRequest
                  .query(QueryBuilders.bool().must(query)
                      .filter(List.of(filterQuery._toQuery())).build()._toQuery());            } else {
              searchRequest.query(query);
            }
            searchRequest.highlight(HIGHLIGHT);

        return searchRequest;
    }
}

