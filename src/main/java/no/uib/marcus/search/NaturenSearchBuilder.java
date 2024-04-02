package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
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
    public SearchRequest.Builder constructSearchRequest() { {
        Query query;
        SearchRequest.Builder searchRequest = super.constructSearchRequest();

        try {
            //Set query
            if (StringUtils.hasText(getQueryString())) {
                query = QueryUtils.buildMarcusQueryString(getQueryString()).build()._toQuery();
            } else {
                query = QueryBuilders.functionScore().functions(List.of(new FunctionScore.Builder().filter(QueryBuilders.exists().field("hasThumbnail").build()._toQuery()).build())).build()._toQuery();
                // @todo?  skip reimplementing scoring except thumbnails
                //      .query(QueryBuilders.matchAll().build()._toQuery());functionScoreQuery(QueryBuilders.matchAllQuery())
                //     .add(FilterBuilders.rangeFilter(Params.DateField.AVAILABLE)
                //              .from(LocalDate.now().minusDays(1)), weightFactorFunction(2))
                //      .add(FilterBuilders.existsFilter("hasThumbnail"), weightFactorFunction(2));

                // Restrict search only on type issues
                if (getTypes() == null || getTypes().length == 0) {
                    // Index types are disabled from ES v6.0, we will therefore need to find
                    // another way to do this restriction
                    //@todo index old type as a field
                    //   searchRequest.setTypes(BoostType.ISSUE);
                }
            }
            //Set filtered query, whether with or without filter
            if (getFilter() != null) {
                searchRequest.query(QueryBuilders.bool().filter(List.of(getFilter().build())).build()._toQuery());
            } else {
                searchRequest.query(query);
            }
            //Set highlighting option
            searchRequest.highlight(new Highlight.Builder().fields("textContent", new HighlightField.Builder().preTags("<em class='txt-highlight'>")
                    .postTags("</em>").build()).build());


            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
            //System.out.println(searchRequest.toString());
        } finally {

        }
        return searchRequest;
    }

    //Type to be boosted if nothing is specified
 ////   static class BoostType {
 //       final static String ISSUE = "issue";
 //   }

    }

}

