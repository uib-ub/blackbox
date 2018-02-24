package no.uib.marcus.search;

import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;

/**
 * Abstract builder for search services.
 * The idea here is that, all search service builders should inherit this class.
 * @author Hemed Ali Al Ruwehy
 *
 * University of Bergen Library.
 * 2016-04-15
 */
public abstract class AbstractSearchBuilder<T extends AbstractSearchBuilder<T>> implements SearchService<T> {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private Client client;
    @Nullable
    private String[] indices;
    @Nullable
    private String[] types;
    @Nullable
    private String queryString;
    private int from = 0;
    private int size = 10;

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
     * Get documents offset, default to 0.
     */
    public int getFrom() {
        return from;
    }

    /**
     * Set from (offset), a start of a document, default to 0
     *
     * @param from an offset
     * @return this object where from is set
     */
    @SuppressWarnings("unchecked")
    public T setFrom(int from) {
        if (from >= 0) {
            this.from = from;
        }
        return (T)this;
    }

    /**
     * Set how many documents to be returned, default to 10.
     *
     * @param size a size of document returned
     * @return this object where size has been set
     */
    @SuppressWarnings("unchecked")
    public T setSize(int size) {
        if (size >= 0) {
            this.size = size;
        }
        return (T)this;
    }

    /**
     * Get size of the returned documents, default to 10.
     **/
    public int getSize() {
        return size;
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
     * Get all documents based on the service settings.
     *
     * @return a SearchResponse, can be <code>null</code>, which means search was not successfully executed.
     */
    @Override
    public abstract SearchResponse executeSearch();


    /**
     * Construct search request based on the service settings.
     **/
    @Override
    public abstract SearchRequestBuilder constructSearchRequest();


    /**
     * Ensure this array of string is neither null nor empty
     */
    public static boolean isNeitherNullNorEmpty(String[] s) {
        return s != null && s.length > 0;
    }

    /**
     * Print out properties of this instance as a JSON string
     *
     * @return a JSON string of service properties
     */
    @Override
    public String toString() {
        try {
            XContentBuilder jsonObj = XContentFactory.jsonBuilder().prettyPrint()
                    .startObject()
                    .field("indices", indices == null ? Strings.EMPTY_ARRAY : indices)
                    .field("type", types == null ? Strings.EMPTY_ARRAY : types)
                    .field("from", from)
                    .field("size", size)
                    .endObject();
            return jsonObj.string();
        } catch (IOException e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }
}
