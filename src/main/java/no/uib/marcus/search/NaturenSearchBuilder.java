package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import no.uib.marcus.common.util.QueryUtils;
import java.util.logging.Logger;

import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
//import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

/***
 * A search builder for naturen
 * @author Hemed Al Ruwehy
 */
public class NaturenSearchBuilder extends MarcusSearchBuilder {

    private final Logger logger = Logger.getLogger(NaturenSearchBuilder.class.getName());

    NaturenSearchBuilder(RestHighLevelClient client) {
        super(client);
    }

    @Override
    public SearchRequest.Builder constructSearchRequest() {
        QueryBuilder query;
        SearchRequest.Builder searchRequest = super.constructSearchRequest();

        try {
            //Set query
            if (Strings.hasText(getQueryString())) {
                query = QueryUtils.buildMarcusQueryString(getQueryString());
            } else {
             //   query = QueryBuilders.matchAllQuery().
                //        .add(QueryBuilders.rangeQuery(Params.DateField.AVAILABLE)
                //                .from(LocalDate.now().minusDays(1)), weightFactorFunction(2))
           //             .add(QueryBuilders.fil.existsFilter("hasThumbnail"), weightFactorFunction(2));

                // Restrict search only on type issues
                if (getTypes() == null || getTypes().length == 0) {
                    // Index types are disabled from ES v6.0, we will therefore need to find
                    // another way to do this restriction
                    // ES suggests using a keyword field "type"
                 //   searchRequest.setTypes(BoostType.ISSUE);
                }
            }
            //Set filtered query, whether with or without filter
            if (getFilter() != null) {
                searchRequest.setQuery(QueryBuilders.boolQuery().filter(getFilter()));
            } else {
              //  searchRequest.setQuery(query);
            }
            //Set highlighting option
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            HighlightBuilder.Field field = new HighlightBuilder.Field("textContent");
            highlightBuilder.field(field).preTags("<em class='txt-highlight'>").postTags("</em>");

            searchRequest.highlighter(highlightBuilder);
                    // .setHighlighterNoMatchSize(0) // if no match, return these characters, 0 means return all
                    // .setHighlighterFragmentSize(500) //default 100
                    // .setHighlighterNumOfFragments(0) //fragments to return. 0 means return everything


            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
            //System.out.println(searchRequest.toString());
        } catch (SearchSourceBuilderException e) {
            logger.severe("Exception occurred when building search request: " + e.getRootCause().getMessage());
        }
        return searchRequest;
    }

    //Type to be boosted if nothing is specified
    static class BoostType {
        final static String ISSUE = "issue";
    }


}
