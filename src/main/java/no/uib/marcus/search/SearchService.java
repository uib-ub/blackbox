package no.uib.marcus.search;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.search.sort.SortBuilder;

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
         * Get all documents based on the query string. If the queryString is <code> null
         * </code>, then all documents will be matched. return a search
         * response.
         */
        public SearchResponse getDocuments(@Nullable String queryString);

        /**
         * Get all documents based on the given parameters and then add post
         * filters.
         */
        public SearchResponse getDocuments(String queryString, FilterBuilder filterBuilder);

}
