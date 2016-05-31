package no.uib.marcus.search;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;

import javax.validation.constraints.NotNull;

/**
 * Abstract search service builder. The idea is all search service builders to inherit from this class.
 * @author Hemed Ali Al Ruwehy
 *
 * University of Bergen Library.
 * 2016-04-15
 */
public abstract class AbstractSearchBuilder<T extends AbstractSearchBuilder<T>> implements SearchService<T> {
    private Client client;
    @Nullable
    private String[] indices;
    @Nullable
    private String[] types;
    @Nullable
    private String queryString;

    /**
     * Constructor
     *
     * @param client Elasticsearch client to communicate with a cluster.
     */
    public AbstractSearchBuilder(@NotNull Client client) {
        if (client == null) {
            throw new IllegalParameterException("[Unable to initialize service. Client cannot be null]");
        }
        this.client = client;
    }

    /**
     * A "getThis" trick is a way to recover the type of the this object in the class hierarchies.
     * It avoids the "unchecked warnings" from the compiler.
     */
    //protected abstract T getThis();

    /**
     * Set Elasticsearch client
     *
     * @param client Elasticsearch client to communicate with a cluster. Cannot be <code>null</code>
     * @return this object where client has been set
     */
    @SuppressWarnings("unchecked")
    public T setClient(@NotNull Client client) {
        if (client == null) {
            throw new IllegalParameterException("[Unable to initialize service. Client cannot be null]");
        }
        this.client = client;
        return (T)this;
    }

    /**
     * Get Elasticsearch client for this service
     */
    public Client getClient() {
        return client;
    }

    /**
     * Set indices the query to be executed upon, default is the entire cluster.
     *
     * @param indices
     * @return this object where indices have been set
     */
    @SuppressWarnings("unchecked")
    public T setIndices(String... indices) {
        this.indices = indices;
        return (T)this;
    }

    /**
     * Get indices for this service
     */
    public String[] getIndices() {
        return indices;
    }

    /**
     * Set index types to be executed upon, default to all types in the index.
     *
     * @param types index types
     * @return this object where index types are set
     */
    @SuppressWarnings("unchecked")
    public T setTypes(String... types) {
        this.types = types;
        return (T)this;
    }

    /**
     * Get index types for this service
     */
    public String[] getTypes() {
        return types;
    }

    /**
     * Set a query string
     *
     * @param queryString a query string
     * @return this object where query string has been set.
     */
    @SuppressWarnings("unchecked")
    public T setQueryString(@Nullable String queryString) {
        this.queryString = queryString;
        return (T)this;
    }

    /**
     * Get query string for this service
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * Get documents based on the service settings.
     *
     * @return a search response.
     */
    @Override
    public abstract SearchResponse getDocuments();
}
