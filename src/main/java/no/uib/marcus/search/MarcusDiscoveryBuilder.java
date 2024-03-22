package no.uib.marcus.search;

import java.util.List;
import java.util.logging.Logger;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import no.uib.marcus.common.util.StringUtils;

/**
 * A basic builder to explore Marcus dataset
 *
 * @author Hemed Ali
 */
public class MarcusDiscoveryBuilder extends AbstractSearchBuilder<MarcusDiscoveryBuilder> {
    private static final Logger logger = Logger.getLogger(MarcusDiscoveryBuilder.class.getName());

    MarcusDiscoveryBuilder(ElasticsearchClient client) {
        super(client);
    }


    /**
     * Construct search request based on the service settings
     **/
    @Override
    public SearchRequest.Builder constructSearchRequest() {
        SearchRequest.Builder searchRequest = new SearchRequest.Builder();
        try {
            //Set indices
            if (isNeitherNullNorEmpty(getIndices())) {
                searchRequest.index(List.of(getIndices()));
            }
            //Set types
            // types removes
         //   if (isNeitherNullNorEmpty(getTypes())) {
         //       searchRequest.setTypes(getTypes());
         //   }
            //Set query
            if (StringUtils.hasText(getQueryString())) {
                searchRequest.q(getQueryString());
            } else {
                searchRequest.query(QueryBuilders.matchAll().build()._toQuery());
            }
            //Set from and size
            searchRequest.from(getFrom());
            searchRequest.size(getSize());

        }
        finally {

        }
        return searchRequest;
    }
}
