package no.uib.marcus.search;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;

import org.elasticsearch.common.Nullable;
import javax.validation.constraints.NotNull;

/**
 * @author Hemed Ali Al Ruwehy
 * A contract for creating search services.
 */
public interface SearchService<S extends SearchService> {
    /**
     * Set Elasticsearch client to the service
     * @param client Elasticsearch client to communicate with a cluster. Cannot be <code>null</code>
     */
    S setClient(@NotNull Client client);

    /**
     * Set up indices for the service, default to all indices in the cluster
     * @param indices one or more indices
     */
    S setIndices(@Nullable String... indices);

    /**
     * Set up index types for the service, default to all types in an index
     * @param types one or more index types
     */
    S setTypes(@Nullable String... types);

    /**
     * Set up a query string, default to <code>null</code> which means query for everything.
     * @param queryString a nullable query string
     **/
    S setQueryString(@Nullable String queryString);

    /**
     * Set from (offset), a start of a document
     * @param from a result offset
     */
    S setFrom(int from);

    /**
     * Set how many documents to be returned
     * @param size a size of document returned
     */
    S setSize(int size);

    /**
     * Get documents based on the service settings.
     *
     * @return a search response.
     */
    SearchResponse executeSearch();

    /**
     * Construct search request based on the service settings
     **/
    SearchRequestBuilder constructSearchRequest();
}
