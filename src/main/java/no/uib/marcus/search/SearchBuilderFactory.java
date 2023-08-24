package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import no.uib.marcus.client.ElasticsearchClientFactory;
import no.uib.marcus.common.ServiceName;


/**
 * A set of static factory methods for creation of {@link SearchBuilder}s.
 */
public class SearchBuilderFactory {

    /**
     * Prevents instantiation
     */
    private SearchBuilderFactory() {
    }

    /**
     * Create a new search service for Marcus data set
     */
    public static MarcusSearchBuilder marcusSearch(ElasticsearchClient elasticsearchClient) {
        return new MarcusSearchBuilder(elasticsearchClient);
    }


    /**
     * Create a Skeivtarkiv(SkA) search builder
     */
    public static SkaSearchBuilder skaSearch(ElasticsearchClient client) {
        return new SkaSearchBuilder(client);
    }

    /**
     * Create a Wittgenstein Archives search builder
     **/
    public static WabSearchBuilder wabSearch(ElasticsearchClient client) {
        return new WabSearchBuilder(client);
    }

    /**
     * Create a Skeivtarkiv(SkA) search builder
     */
    public static NaturenSearchBuilder naturenSearch(ElasticsearchClient client) {
        return new NaturenSearchBuilder(client);
    }

    /**
     * Create a new discovery service for Marcus data set.
     * It is a subset of search service with minimal capabilities.
     * For search, @see SearchBuilderFactory#marcusSearch(Client)
     **/
    public static MarcusDiscoveryBuilder marcusDiscovery(ElasticsearchClient client) {
        return new MarcusDiscoveryBuilder(client);
    }


    /**
     * Gets corresponding search builder based on the service parameter
     *
     * @param serviceString a service parameter. If it is null, it will fall to a default service.
     * @param client        a search client to be used
     * @return a corresponding search builder
     */
    public static SearchBuilder<? extends AbstractSearchBuilder<?>> getSearchBuilder(
            @Nullable String serviceString,
            ElasticsearchClient client) {
        ServiceName service = ServiceName.toEnum(serviceString);
        switch (service) {
            case SKA:
                return SearchBuilderFactory.skaSearch(client);
            case WAB:
                return SearchBuilderFactory.wabSearch(client);
            case MARCUS:
            case MARCUS_ADMIN:
                return SearchBuilderFactory.marcusSearch(client);
            case NATUREN:
                return SearchBuilderFactory.naturenSearch(client);
            default:
                throw new IllegalParameterException("Unknown service parameter [" + service + "]");
        }
    }

}
