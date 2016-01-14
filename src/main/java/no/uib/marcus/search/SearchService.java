package no.uib.marcus.search;

import org.elasticsearch.action.search.SearchResponse;
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
    public SearchResponse getDocuments(String[] indices, String[] types, String aggs);

    /**
     * Get all Documents based on the query string.
     */
    public SearchResponse getDocuments(String queryStr, String[] indices, String[] types, String aggs);

    /**
     *Get all documents based on the given parameters and then add post filters.
     */
    public SearchResponse getDocuments(String queryStr, String[] indices, String[] types, FilterBuilder filterBuilder, String json);

}
