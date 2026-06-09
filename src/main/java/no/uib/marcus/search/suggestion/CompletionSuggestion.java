package no.uib.marcus.search.suggestion;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.logging.Level;
import no.uib.marcus.client.ElasticsearchClientFactory;
import co.elastic.clients.elasticsearch.core.SearchRequest.Builder;

import jakarta.annotation.Nullable;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.*;

/**
 * A class for handling Completion suggestion (auto-suggestion).
 * @author Hemed Ali Al Ruwehy (hemed.ruwehy@uib.no)
 * <p>
 * University of Bergen Library
 */
public class CompletionSuggestion {

    private static final Logger logger = Logger.getLogger(CompletionSuggestion.class.getName());
    private static final String SUGGEST_FIELD = "suggest";

  // Private constructor to prevent instantiation
   private CompletionSuggestion() {
    throw new IllegalStateException("Utility class");
  }

    /**A method to get a list of suggestions.
     * @param text input text
     * @param size Sets the maximum suggestions to be returned per suggest text term.
     * @param indices array of one or more setIndices, can be <code>null</code>
     * @return a set of suggestion texts.
     **/
    public static Set<String> getSuggestions(String text, int size, @Nullable String... indices) {
        Set<String> suggestValues = new HashSet<>();
        try {
            SearchResponse<ObjectNode> suggestResponse = getSuggestionResponse(text, size, indices);
            List<Suggestion<ObjectNode>> suggestions = suggestResponse.suggest().get(SUGGEST_FIELD);

            //Add each option(value) to a set to ensure no repetition
            for ( Suggestion<ObjectNode> suggestion : suggestions) {
              List<CompletionSuggestOption<ObjectNode>> options = suggestion.completion().options();
              for (CompletionSuggestOption<ObjectNode> option : options) {
                logger.log(Level.FINE,"completion: {0}", option.text());
                suggestValues.add(option.text());
              }
            }
        } catch (Exception e) {
            logger.severe("Unable to perform suggestion for text: [" + text + "]. Message: " + e.getLocalizedMessage()) ;
        }
        //We want suggestion values to be sorted, hence we put them in a tree set
        return new TreeSet<>(suggestValues);
    }

    /**A method to get a list of suggestions.
     * @param text input text
     * @param size Sets the maximum suggestions to be returned per suggest text term.
     * @param indices array of one or more setIndices, can be <code>null</code>
     * @return a suggestion response.
     **/
    public static SearchResponse<ObjectNode> getSuggestionResponse(String text, int size, @Nullable String... indices) throws IOException {
        Map<String, FieldSuggester> map = new HashMap<>();
        map.put(SUGGEST_FIELD,FieldSuggester.of(fs -> fs
            .completion(cs -> cs.skipDuplicates(true)
                .size(size)
                .field(SUGGEST_FIELD)
                .analyzer("keyword"))));
        logger.log(Level.FINE, "getSuggestionResponse: {0}", map);

        Suggester suggester = Suggester.of(sf -> sf.suggesters(map).text(text));
        Builder builder = new Builder();
        builder.suggest(suggester);
        ElasticsearchClient client = ElasticsearchClientFactory.getElasticsearchClient();
        if (indices != null && indices.length > 0) {
            builder.index(List.of(indices));
        }
        builder.size(0);
        SearchResponse<ObjectNode> response = client.search(builder.build(), ObjectNode.class);
        logger.log(Level.FINE, "getSuggestionResponse: {0}", response.suggest().get(SUGGEST_FIELD));
        return response;
    }

}
