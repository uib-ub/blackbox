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
    public static MarcusSearchBuilder marcusSearch(Client client) {
        return new MarcusSearchBuilder(client);
    }


    /**
     * Create a Skeivtarkiv(SkA) search builder
     **/
    public static SkaSearchBuilder skaSearch(Client client) {
        return new SkaSearchBuilder(client);
    }

    /**
     * Create a Wittgenstein Archives search builder
     **/
    public static WabSearchBuilder wabSearch(Client client) {
        return new WabSearchBuilder(client);
    }

    /**
     * Create a new discovery service for Marcus data set.
     * It is a subset of search service with minimal capabilities.
     * For search, @see SearchBuilderFactory#marcusSearch(Client)
     **/
    public static MarcusDiscoveryBuilder marcusDiscovery(Client client) {
        return new MarcusDiscoveryBuilder(client);
    }

    /**
     * Create a dummy service. It may use different client and different settings.
     * (Not yet implemented)
     **/
    public static <T extends AbstractSearchBuilder> T dummySearch() {
        return null;
    }

}
