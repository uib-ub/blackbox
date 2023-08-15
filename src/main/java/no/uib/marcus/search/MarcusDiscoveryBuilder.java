package no.uib.marcus.search;

import java.util.logging.Logger;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

/**
 * A basic builder to explore Marcus dataset
 *
 * @author Hemed Ali
 */
public class MarcusDiscoveryBuilder extends AbstractSearchBuilder<MarcusDiscoveryBuilder> {
    private static final Logger logger = Logger.getLogger(MarcusDiscoveryBuilder.class.getName());

    MarcusDiscoveryBuilder(Client client) {
        super(client);
    }


    /**
     * Construct search request based on the service settings
     **/
    @Override
    public SearchRequestBuilder constructSearchRequest() {
        SearchRequestBuilder searchRequest = getClient().prepareSearch();
        try {
            //Set indices
            if (isNeitherNullNorEmpty(getIndices())) {
                searchRequest.setIndices(getIndices());
            }
            //Set types
            if (isNeitherNullNorEmpty(getTypes())) {
              //  searchRequest.setTypes(getTypes());
            }
            //Set query
            if (Strings.hasText(getQueryString())) {
                searchRequest.setQuery(QueryBuilders.queryStringQuery(getQueryString()));
            } else {
                searchRequest.setQuery(QueryBuilders.matchAllQuery());
            }
            //Set from and size
            searchRequest.setFrom(getFrom());
            searchRequest.setSize(getSize());

        } catch (SearchSourceBuilderException se) {
            logger.severe("Exception on preparing the request: " + se.getDetailedMessage());
        } catch (ElasticsearchException ex) {
            logger.severe(ex.getDetailedMessage());
        }
        return searchRequest;
    }
}
