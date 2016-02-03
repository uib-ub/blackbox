package no.uib.marcus.search;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.index.query.FilterBuilder;

/**
 *
 * @author Hemed Ali
 */
public interface SearchService {

    /**
     * Get all documents through all indices in the cluster
     */
    public SearchResponse getAllDocuments();

    /**
     * Match all documents
     */
    //public SearchResponse getDocuments(String[] indices, String[] types, String aggs, int from, int size);

    /**
     * Get all documents based on the query string. If the queryString is <code> null </code>, then all documents will be matched.
     */
    public SearchResponse getDocuments(@Nullable String queryString, String[] indices, String[] types, String aggs, int from, int size);

    /**
     *Get all documents based on the given parameters and then add post filters.
     */
    public SearchResponse getDocuments(String queryString, String[] indices, String[] types, FilterBuilder filterBuilder, String json, int from, int size);


}
