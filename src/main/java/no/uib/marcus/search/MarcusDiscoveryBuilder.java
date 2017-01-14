package no.uib.marcus.search;

import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;

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
     * Get all documents based on the service settings.
     *
     * @return a SearchResponse, can be <code>null</code>, which means search was not successfully executed.
     */
    @Override
    @Nullable
    public SearchResponse executeSearch() {
        SearchResponse response = null;
        try {
            response = constructSearchRequest().execute().actionGet();
            //Show response for debugging purpose
            //logger.info(response.toString());
        }
        catch (SearchPhaseExecutionException e) {
            logger.error("Could not execute search: " + e.getDetailedMessage());
        }
        return response;
    }
}
