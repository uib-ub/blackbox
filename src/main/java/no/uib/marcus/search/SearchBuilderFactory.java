package no.uib.marcus.search;

import no.uib.marcus.common.ServiceName;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;

/**
 * Creation factory for search builders.
 */
public class SearchBuilderFactory {
    private SearchBuilderFactory() {
    }

    /**
     * Create a new search service for Marcus data set
     */
    public static MarcusSearchBuilder marcusSearch(Client client) {
        return new MarcusSearchBuilder(client);
    }


    /**
     * Create a Skeivtarkiv(SkA) search builder
     */
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
     * Gets default search builder
     */
    public static SearchBuilder get(Client client) {
        return get(null, client);
    }


    /**
     * Gets corresponding search builder based on the service parameter
     *
     * @param serviceString a service parameter. If it is null, it will fall to a default service.
     * @param client        a search client to be used
     * @return a corresponding search builder
     */
    public static SearchBuilder get(@Nullable String serviceString, Client client) {
        ServiceName service = ServiceName.toEnum(serviceString);
        switch (service) {
            case SKA:
                return SearchBuilderFactory.skaSearch(client);
            case WAB:
                return SearchBuilderFactory.wabSearch(client);
            case MARCUS:
                return SearchBuilderFactory.marcusSearch(client);
            case MARCUS_ADMIN:
                return SearchBuilderFactory.marcusSearch(client);
            default:
                throw new IllegalParameterException("Unknown service parameter [" + service + "]");
        }
    }

}
