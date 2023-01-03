package no.uib.marcus.search;

import no.uib.marcus.common.Params;
import no.uib.marcus.common.util.QueryUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

import java.time.LocalDate;

import static org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders.weightFactorFunction;

/***
 * A search builder for naturen
 * @author Hemed Al Ruwehy
 */
public class NaturenSearchBuilder extends MarcusSearchBuilder {

    private final Logger logger = LogManager.getLogger(getClass().getName());

    NaturenSearchBuilder(Client client) {
        super(client);
    }

    @Override
    public SearchRequestBuilder constructSearchRequest() {
        QueryBuilder query;
        SearchRequestBuilder searchRequest = super.constructSearchRequest();

        try {
            //Set query
            if (Strings.hasText(getQueryString())) {
                query = QueryUtils.buildMarcusQueryString(getQueryString());
            } else {
                query = QueryBuilders.functionScoreQuery(QueryBuilders.matchAllQuery())
                        .add(FilterBuilders.rangeFilter(Params.DateField.AVAILABLE)
                                .from(LocalDate.now().minusDays(1)), weightFactorFunction(2))
                        .add(FilterBuilders.existsFilter("hasThumbnail"), weightFactorFunction(2));

                // Restrict search only on type issues
                if (getTypes() == null || getTypes().length == 0) {
                    // Index types are disabled from ES v6.0, we will therefore need to find
                    // another way to do this restriction
                    searchRequest.setTypes(BoostType.ISSUE);
                }
            }
            //Set filtered query, whether with or without filter
            if (getFilter() != null) {
                searchRequest.setQuery(QueryBuilders.filteredQuery(query, getFilter()));
            } else {
                searchRequest.setQuery(query);
            }
            //Set highlighting option
            searchRequest.addHighlightedField("textContent")
                    // .setHighlighterNoMatchSize(0) // if no match, return these characters, 0 means return all
                    // .setHighlighterFragmentSize(500) //default 100
                    // .setHighlighterNumOfFragments(0) //fragments to return. 0 means return everything
                    .setHighlighterPreTags("<em class='txt-highlight'>")
                    .setHighlighterPostTags("</em>");

            //Show builder for debugging purpose
            //logger.info(searchRequest.toString());
            //System.out.println(searchRequest.toString());
        } catch (SearchSourceBuilderException e) {
            logger.error("Exception occurred when building search request: " + e.getMostSpecificCause());
        }
        return searchRequest;
    }

    //Type to be boosted if nothing is specified
    static class BoostType {
        final static String ISSUE = "issue";
    }


}
