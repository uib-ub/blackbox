package no.uib.marcus.search;

import no.uib.marcus.common.ServiceName;
import org.apache.log4j.Logger;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;

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


    /**
     * Decide which search service to use based on the service parameter
     * @param serviceString a service parameter. If it is null, it will fall to a default service.
     * @param client
     * @return a corresponding search builder
     */
    public static MarcusSearchBuilder getSearchBuilder(@Nullable String serviceString, Client client) {
        ServiceName service = ServiceName.toEnum(serviceString);
        switch (service){
            case SKA :
                return SearchBuilderFactory.skaSearch(client);
            case WAB :
                return SearchBuilderFactory.wabSearch(client);
            case MARCUS :
                return SearchBuilderFactory.marcusSearch(client);
            default:
                throw new AssertionError("Unknown service for " + service);
        }
    }


}
