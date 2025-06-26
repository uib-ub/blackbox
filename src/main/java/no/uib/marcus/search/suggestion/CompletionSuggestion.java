package no.uib.marcus.search.suggestion;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.*;
import co.elastic.clients.json.jackson.JacksonJsonpGenerator;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.stream.JsonGenerator;
import java.io.StringWriter;
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
    private static Suggester.Builder suggesterBuilder;

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
            List<Suggestion<ObjectNode>> suggestions = suggestResponse.suggest().get("suggest-test");

            JsonFactory jsonFactory = new JsonFactory();

            //Add each option(value) to a set to ensure no repetition
            for ( Suggestion<ObjectNode> suggestion : suggestions)

                     {
                         List<CompletionSuggestOption<ObjectNode>> options = suggestion.completion().options();
                         for (CompletionSuggestOption<ObjectNode> option : options) {
                             logger.info("completion: " + option.text());
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
        map.put("suggest-test",FieldSuggester.of(fs -> fs
            .completion(cs -> cs.skipDuplicates(true)
                .size(size)
                        .field(SUGGEST_FIELD))));
        logger.info("getSuggestionResponse: " + map);

        Suggester suggester = Suggester.of(sf -> sf.suggesters(map).text(text));
       // Suggester.Builder suggesterBuilder = new Suggester.Builder().completion(new CompletionSuggester.Builder().field(SUGGEST_FIELD).size(size).build()._toFieldSuggester());
        Builder builder = new Builder();
        builder.suggest(suggester);
       // builder.suggest(suggesterBuilder.suggesters("test", FieldSuggester.of(new FieldSuggester.Builder().)).text(text).build());
        ElasticsearchClient client = ElasticsearchClientFactory.getElasticsearchClient();
        if (indices != null && indices.length > 0) {
            builder.index(List.of(indices));
        }
        builder.size(size);
        SearchResponse<ObjectNode> response = client.search(builder.build(), ObjectNode.class);
        logger.info("getSuggestionResponse: " + response.suggest().get("suggest-test"));
        return response;
    }

    //Main method for easy debugging
    public static void main(String[] args) throws JsonProcessingException {
        String jsonString = new JsonMapper().writeValueAsString(
                CompletionSuggestion
                        .getSuggestions("Ms-114,120v[7]", 10, "wab"));

        logger.info(jsonString);
    }

}
