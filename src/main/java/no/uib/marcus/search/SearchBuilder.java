package no.uib.marcus.search;

import no.uib.marcus.common.util.AggregationUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.sort.SortBuilder;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Abstract builder for search services. The idea here is that, all search builders
 * should inherit this class.
 *
 * @author Hemed Ali Al Ruwehy
 * <p>
 * University of Bergen Library.
 * 2016-04-15
 */
public abstract class SearchBuilder<T extends SearchBuilder<T>> implements SearchService<T> {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private Client client;
    @Nullable
    private String[] indices;
    @Nullable
    private String[] types;
    @Nullable
    private String queryString;
    private FilterBuilder filter, postFilter;
    private Map<String, List<String>> selectedFacets;
    private String aggregations;
    private SortBuilder sortBuilder;
    private String indexToBoost;
    private int from = 0;
    private int size = 10;

    /**
     * Constructor
     *
     * @param client a non-null Elasticsearch client to communicate with a cluster.
     */
    public SearchBuilder(@NotNull Client client) {
        if (client == null) {
            throw new IllegalParameterException("Unable to initialize service. Client cannot be null");
        }
        this.client = client;
    }

    /**
     * A "getThis" trick is a way to recover the type of the this object in the class hierarchies.
     * It avoids the "unchecked warnings" from the compiler.
     */
    //protected abstract T getThis();

    /**
     * Ensure this string or array of string is neither null nor empty
     */
    public static boolean isNeitherNullNorEmpty(String... s) {
        return s != null && s.length > 0;
    }

    /**
     * Get aggregations or <tt>null</tt> if not set
     */
    public String getAggregations() {
        return aggregations;
    }

    /**
     * Set aggregations to be applied
     *
     * @param aggregations a JSON string of aggregations
     * @return this object where aggregations have been set
     */
    @SuppressWarnings("unchecked")
    public T setAggregations(String aggregations) {
        if (aggregations != null) {
            AggregationUtils.validateAggregations(aggregations);
            this.aggregations = aggregations;
        }
        return (T) this;
    }

    /**
     * Get sort builder or <tt>null</tt> if not set
     */
    public SortBuilder getSortBuilder() {
        return sortBuilder;
    }

    /**
     * Set a sortBuilder order
     *
     * @param sortBuilder
     * @return this object where sort has been set
     */
    @SuppressWarnings("unchecked")
    public T setSortBuilder(SortBuilder sortBuilder) {
        this.sortBuilder = sortBuilder;
        return (T) this;
    }

    /**
     * Get sort builder or <tt>null</tt> if not set
     **/
    public FilterBuilder getFilter() {
        return filter;
    }

    /**
     * Set a top-level filter that would filter both search results and aggregations
     *
     * @param filter
     * @return this object where filter has been set
     */
    @SuppressWarnings("unchecked")
    public T setFilter(FilterBuilder filter) {
        this.filter = filter;
        return (T) this;
    }

    /**
     * Get post filter or <tt>null</tt> if not set
     */
    public FilterBuilder getPostFilter() {
        return postFilter;
    }

    /**
     * Set a post_filter that would affect only search results but NOT aggregations.
     * You would use this on "OR" terms aggregations
     *
     * @param filter
     * @return this object where post_filter has been set
     */
    @SuppressWarnings("unchecked")
    public T setPostFilter(FilterBuilder filter) {
        this.postFilter = filter;
        return (T) this;
    }

    public String getIndexToBoost() {
        return indexToBoost;
    }

    /**
     * Set index that need to be boosted
     *
     * @param indexToBoost an index to boost
     * @return this builder where index to boost has been set
     */
    @SuppressWarnings("unchecked")
    public T setIndexToBoost(String indexToBoost) {
        this.indexToBoost = indexToBoost;
        return (T) this;
    }

    /**
     * Get selected filters or <tt>null</tt> if not set
     *
     * @return a map containing selected filters
     */
    public Map<String, List<String>> getSelectedFacets() {
        return selectedFacets;
    }

    /**
     * Set a selected filters map so that we can build filters out of them
     *
     * @param selectedFacets selected filters
     * @return this object where a date range filter has been set
     */
    @SuppressWarnings("unchecked")
    public T setSelectedFacets(Map<String, List<String>> selectedFacets) {
        if (selectedFacets != null && !selectedFacets.isEmpty()) {
            this.selectedFacets = selectedFacets;
        }
        return (T) this;
    }

    /**
     * Get Elasticsearch client for this service
     */
    public Client getClient() {
        return client;
    }

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
        return (T) this;
    }

    /**
     * Get indices for this service
     */
    public String[] getIndices() {
        return indices;
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
        return (T) this;
    }

    /**
     * Get index types for this service
     */
    public String[] getTypes() {
        return types;
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
        return (T) this;
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
        return (T) this;
    }

    /**
     * Get size of the returned documents, default to 10.
     **/
    public int getSize() {
        return size;
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
        return (T) this;
    }

    /**
     * Get query string for this service
     */
    public String getQueryString() {
        return queryString;
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
        return (T) this;
    }

    /**
     * Construct search request based on the service settings. Should be implemented by subclasses
     */
    @Override
    public abstract SearchRequestBuilder constructSearchRequest();

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
        } catch (SearchPhaseExecutionException e) {
            //I've not found a direct way to validate a query string. Therefore, the idea here is to catch any
            //exception that is related to search execution.
            logger.error("Could not execute search: " + e.getDetailedMessage());
        }
        return response;
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
                    .field("indices", getIndices() == null ? Strings.EMPTY_ARRAY : getIndices())
                    .field("type", getTypes() == null ? Strings.EMPTY_ARRAY : getTypes())
                    .field("from", getFrom())
                    .field("size", getSize())
                    .field("aggregations", getAggregations() == null ? Strings.EMPTY_ARRAY : getAggregations())
                    .endObject();
            return jsonObj.string();
        } catch (IOException e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }
}