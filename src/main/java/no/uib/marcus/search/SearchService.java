
package no.uib.marcus.search;

import java.util.Map;
import org.elasticsearch.action.search.SearchResponse;

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
    public SearchResponse getAllDocuments(String queryStr, String indexName, String typeName, Map<String, String> aggMap);

}
