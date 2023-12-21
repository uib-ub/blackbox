package no.uib.marcus.search;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import co.elastic.clients.elasticsearch._types.SortOptions;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

/**
 * A contract for creating search builders. See also {@link AbstractSearchBuilder}
 *
 * @author Hemed Ali Al Ruwehy
 */
public interface SearchBuilder<S> {
    /**
     * Set Elasticsearch client to the service
     *
     * @param elasticsearchClient Elasticsearch client to communicate with a cluster. Cannot be <code>null</code>
     */
    S setElasticsearchClient(@NotNull ElasticsearchClient elasticsearchClient);

    /**
     * Set up indices for the service, default to all indices in the cluster
     *
     * @param indices one or more indices
     */
    S setIndices(@Nullable String... indices);

    /**
     * Set up index types for the service, default to all types in an index
     *
     * @param types one or more index types
     */
    S setTypes(@Nullable String... types);

    /**
     * Set up a query string, default to <code>null</code> which means query for everything.
     *
     * @param queryString a nullable query string
     **/
    S setQueryString(@Nullable String queryString);

    /**
     * Set from (offset), a start of a document
     *
     * @param from a result offset
     */
    S setFrom(int from);

    /**
     * Set how many documents to be returned
     *
     * @param size a size of document returned
     */
    S setSize(int size);

    /**
     * Sets index to boost
     */
    S setIndexToBoost(String indexToBoost);

    /**
     * Sets aggregations
     */
    S setAggregations(@Nullable String aggs);


    /**
     * Sets selected filer for this search builder.
     *
     * @param selectedFacets in the map, keys are "fields" and values are "terms"
     *                       e.g {"subject.exact" = ["Flyfoto" , "Birkeland"], "type" = ["Brev"]}
     */
    S setSelectedFacets(Map<String, List<String>> selectedFacets);

    /**
     * Sets sort builder
     */
    S setSortBuilder(SortOptions.Builder sortBuilder);


    /**
     * Sets post filter
     */
    S setPostFilter(BoolQuery.Builder postFilter);

    /**
     * Sets filter (filtered_query)
     */
    S setFilter(BoolQuery.Builder postFilter);


    /**
     * Construct search request based on the service settings
     */
    SearchRequest.Builder constructSearchRequest();

    /**
     * Get documents based on the service settings.
     */
    SearchResponse<ObjectNode> executeSearch();

}
