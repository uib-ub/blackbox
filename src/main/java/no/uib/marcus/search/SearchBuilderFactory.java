package no.uib.marcus.search;

import org.elasticsearch.client.Client;

/**
 * Creation factory for search services.
 */
public class SearchBuilderFactory {
    private SearchBuilderFactory() {}

    /**
     * Create a new search service for Marcus data set
     **/
    public static MarcusSearchBuilder createMarcusSearchService(Client client) {
        return new MarcusSearchBuilder(client);
    }

    /**
     * Create a new discovery service for Marcus data set.
     * It is a subset of search service with minimal capabilities.
     * For search, @see SearchBuilderFactory#createMarcusSearchService(Client)
     **/
    public static MarcusDiscoveryBuilder createMarcusDiscoveryService(Client client) {
        return new MarcusDiscoveryBuilder(client);
    }

    /**
     * Create a dummy service. It may use different client and different settings.
     * (Not yet implemented)
     **/
    public static <T extends AbstractSearchBuilder> T createDummySearchService() {
        return null;
    }

}
