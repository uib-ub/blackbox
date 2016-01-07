
package no.uib.marcus.search;

import java.util.Map;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.FilterBuilder;

/**
 *
 * @author  Hemed Ali
 */
public interface SearchService {

    /**
     * Match all documents given the facet values
     */
    public SearchResponse getAllDocuments(String indexName, String typeName, Map<String, String> facetMap);

    /**
     * Match all documents
     */
    public SearchResponse getAllDocuments(String indexName, String typeName);

    /**
     * Get All Documents using query string. 
     */
     public SearchResponse getAllDocuments(String queryStr, String indexName, String typeName);
     
    /**
     * Add Post Filters
     **/
     public SearchResponse getAllDocuments(String queryStr, String indexName, String typeName, FilterBuilder filterBuilder);

}
