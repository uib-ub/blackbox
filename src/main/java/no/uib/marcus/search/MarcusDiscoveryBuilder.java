package no.uib.marcus.search;

import java.util.logging.Logger;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import org.elasticsearch.ElasticsearchException;

import org.elasticsearch.client.RestHighLevelClient;
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

    MarcusDiscoveryBuilder(RestHighLevelClient restHighLevelClient) {
        super(restHighLevelClient);
    }


    /**
     * Construct search request based on the service settings
     **/
    @Override
    public SearchRequest.Builder constructSearchRequest() {
        //SearchRequestBuilder searchRequest = getRestHighLevelClient().prepareSearch();
        SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
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
            if (Strings.hasText(getQueryString().)) {
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
