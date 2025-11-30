package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;

import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.util.ObjectBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.logging.Level;
import no.uib.marcus.common.util.AggregationUtils;

import java.util.Arrays;
import java.util.logging.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;


import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;
import no.uib.marcus.common.util.StringUtils;

/**
 * This class provides a skeletal implementation of {@link SearchBuilder} interface to minimize
 * the effort required to implement the interface when building your own search builder. The idea here is that
 * all custom search builders should inherit from this class.
 *
 * @author Hemed Ali Al Ruwehy
 * <p>
 * University of Bergen Library.
 * 2016-04-15
 */
public abstract class AbstractSearchBuilder<T extends AbstractSearchBuilder<T>> implements SearchBuilder<T> {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private ElasticsearchClient client;
    @Nullable
    private String[] indices;
    @Nullable
    private String queryString;
    private BoolQuery.Builder filter;
  private BoolQuery.Builder postFilter;
    private Map<String, List<String>> selectedFacets;
    private String aggregations;
    private ObjectBuilder<SortOptions> sortBuilder;
    private String indexToBoost;
    private int from = 0;
    private int size = 10;

    private static final ObjectMapper objectMapper =  new ObjectMapper();

    /**
     * Sole constructor. (For invocation by subclass constructors, typically implicit.)
     *
     * @param client a non-null Elasticsearch client to communicate with a cluster.
     */
    protected AbstractSearchBuilder(ElasticsearchClient client) {
        if (client == null) {
            throw new IllegalArgumentException("Unable to initialize service. Client cannot be null");
        }
        logger.log(Level.FINE, "super initialized {0}", client);
        this.client = client;
    }


    /**
     * Ensure this string or array of string is neither null nor empty
     */
    public static boolean isNeitherNullNorEmpty(String... s) {
      if (s == null || s.length == 0) return false;
      for (String str : s) {
        if (str == null || str.isEmpty()) return false;
      }
      return true;
    }

    /**
     * Get aggregations or {@code null} if not set
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
     * Get sort builder or {@code null} if not set
     */
    public ObjectBuilder<SortOptions> getSortBuilder() {
        return sortBuilder;
    }

    /**
     * Set a sortBuilder order
     *
     * @param sortBuilder
     * @return this object where sort has been set
     */
    @SuppressWarnings("unchecked")
    public T setSortBuilder(ObjectBuilder<SortOptions> sortBuilder) {
        this.sortBuilder = sortBuilder;
        return (T) this;
    }

    /**
     * Get sort builder or {@code null} if not set
     **/
    public BoolQuery.Builder getFilter() {
        return filter;
    }

    /**
     * Set a top-level filter that would filter both search results and aggregations
     *
     * @param filter
     * @return this object where filter has been set
     */
    @SuppressWarnings("unchecked")
    public T setFilter(BoolQuery.Builder filter) {
        this.filter = filter;
        return (T) this;
    }

    /**
     * Get POST filter or {@code null} if not set
     */
    public BoolQuery.Builder getPostFilter() {
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
    public T setPostFilter(BoolQuery.Builder filter) {
        if(filter != null) {
            this.postFilter = filter;
        }
        return (T) this;
    }

    public String getIndexToBoost() {
        return indexToBoost;
    }

    /**
     * Set the index that needs to be boosted
     *
     * @param indexToBoost an index to boost
     * @return this builder where the index to boost has been set
     */
    @SuppressWarnings("unchecked")
    public T setIndexToBoost(String indexToBoost) {
        if(StringUtils.hasText(indexToBoost)) {
            this.indexToBoost = indexToBoost;
        }
        return (T) this;
    }

    /**
     * Get selected filters or {@code null} if not set
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
        if (selectedFacets != null) {
            this.selectedFacets = selectedFacets;
        }
        return (T) this;
    }

    /**
     * Get the Elasticsearch client for this service
     */
    public ElasticsearchClient getClient() {
        return client;
    }

    /**
     * Set Elasticsearch client
     *
     * @param client Elasticsearch client to communicate with a cluster. Cannot be <code>null</code>
     * @return this object where the client has been set
     */
    @SuppressWarnings("unchecked")
    public T setClient(ElasticsearchClient client) {
        if (client == null) {
            throw new IllegalParameterException("Unable to initialize service. Client cannot be null");
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
     * Get the size of the returned documents, default to 10.
     **/
    public int getSize() {
        return size;
    }

    /**
     * Set how many documents to be returned, default to 10.
     *
     * @param size a size of the document returned
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
     * Get the query string for this service
     */
    public String getQueryString() {
        return queryString;
    }

    /**
     * Set a query string
     *
     * @param queryString a query string
     * @return this object where the query string has been set.
     */
    @SuppressWarnings("unchecked")
    public T setQueryString(@Nullable String queryString) {
        this.queryString = queryString;
        return (T) this;
    }

    /**
     * Get all documents based on the service settings.
     *
     * @return a SearchResponse, can be <code>null</code>, which means search was not successfully executed.
     */
    @Override
    @Nullable
    public SearchResponse<ObjectNode> executeSearch() {

        SearchResponse<ObjectNode> response = null;
        try {
            response = client.search(constructSearchRequest().build(), ObjectNode.class);
            //Show response for debugging purpose
            logger.log(Level.FINE, "response: {0}", response);
        } catch (java.io.IOException e) {
          //I've not found a direct way to validate a query string. Therefore, the idea here is to catch any
          //exception related to search execution.
          throw new RuntimeException("Could not execute search", e);
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
        ObjectNode jsonObj = objectMapper.createObjectNode();

        jsonObj = jsonObj.put("indices",  getIndices().length == 0   ?  "" : Arrays.toString(getIndices()))
                .put("from", getFrom())
                .put("size", getSize())
                .put("aggregations", getAggregations() == null ? "" : getAggregations());

        return jsonObj.toString();
    }
}
