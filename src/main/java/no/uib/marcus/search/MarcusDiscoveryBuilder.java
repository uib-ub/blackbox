package no.uib.marcus.search;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilderException;

/**
 * A basic builder to explore Marcus dataset
 *
 * @author Hemed Ali
 */
public class MarcusDiscoveryBuilder extends AbstractSearchBuilder<MarcusDiscoveryBuilder>
{
    private static final Logger logger = Logger.getLogger(MarcusDiscoveryBuilder.class);

    MarcusDiscoveryBuilder(Client client){
        super(client);
    }

    /**
     * Get documents based on the service settings
     **/
    @Override
    public SearchResponse getDocuments() {
        assert super.getClient() != null;
        SearchResponse response = null;
        SearchRequestBuilder searchRequest;
        try {
            //Prepare search request
            searchRequest = super.getClient().prepareSearch();

            //Set indices
            if (getIndices() != null && getIndices().length > 0) {
                searchRequest.setIndices(getIndices());
            }
            //Set types
            if (getTypes() != null && getTypes().length > 0) {
                searchRequest.setTypes(getTypes());
            }
            //Set query
            if(Strings.hasText(getQueryString())){
                searchRequest.setQuery(QueryBuilders.queryStringQuery(getQueryString()));
            }else {
                searchRequest.setQuery(QueryBuilders.matchAllQuery());
            }
            //Set from and size
            searchRequest.setFrom(getFrom());
            searchRequest.setSize(getSize());

            //Show SearchRequest builder for debugging purpose
            //logger.info(searchRequest.toString());
            response = searchRequest.execute().actionGet();
            //logger.info(response.toString());
        } catch (SearchSourceBuilderException se) {
            logger.error("Exception on preparing the request: "
                    + se.getDetailedMessage());
        } catch (ElasticsearchException ex) {
            logger.error(ex.getDetailedMessage());
        }
        return response;
    }
}
