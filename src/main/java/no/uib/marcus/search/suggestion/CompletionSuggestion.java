package no.uib.marcus.search.suggestion;

import com.google.gson.Gson;
import no.uib.marcus.client.ClientFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;

import org.elasticsearch.search.suggest.SuggestionBuilder;

import org.elasticsearch.core.Nullable;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import java.util.*;

/**
 * A class for handling Completion suggestion (auto-suggestion).
 * @author Hemed Ali Al Ruwehy (hemed.ruwehy@uib.no)
 * <p>
 * University of Bergen Library
 */
public class CompletionSuggestion {

    private static final Logger logger = LogManager.getLogger(CompletionSuggestion.class);
    private static final String SUGGEST_FIELD = "suggest";

    /**A method to get a list of suggestions.
     * @param text input text
     * @param size Sets the maximum suggestions to be returned per suggest text term.
     * @param indices array of one or more setIndices, can be <code>null</code>
     * @return a set of suggestion texts.
     **/
    public static Set<String> getSuggestions(String text, int size, @Nullable String... indices) {
        Set<String> suggestValues = new HashSet<>();
        try {

            CompletionSuggestion suggestion = CompletionSuggestionBuilder.Reader.;

            Suggest suggestResponse = SearchReponse.getSuggest();

            SearchResponse suggestRespone
            SuggestBuilders.
            SuggestResponse suggestResponse = getSuggestionResponse(text, size, indices);
            suggestBuilder.

            //Add each option(value) to a set to ensure no repetition
            for (Suggest.Suggestion.Entry.Option option : suggestResponse

                    .getSuggestion("completion_suggestion")
                    .iterator()
                    .next()
                    .getOptions()) {
                // Lowecasing is done via ubb-rdf-river
                // suggestValues.add(option.getText().string().toLowerCase());
                suggestValues.add(option.getText().string());
            }
        } catch (Exception e) {
            logger.error("Unable to perform suggestion for text: [" + text + "]. Message: " + e.getLocalizedMessage()) ;
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
    public static Suggest getSuggestionResponse(String text, int size, @Nullable String... indices) {
        SuggestionBuilder suggestionsBuilder = new CompletionSuggestionBuilder("completion_suggestion");

        Suggest suggestResponse = null;
        SuggestBuilder suggestRequest;


        try {
            suggestionsBuilder.field();
            suggestionsBuilder.text(text);
            suggestionsBuilder.size(size);

             SearchRequestBuilder searchRequest = ClientFactory.getTransportClient().prepareSearch().suggest( new SuggestBuilder().addSuggestion());
            suggestRequest.;

            if (indices != null && indices.length > 0) {
                suggestRequest.setIndices(indices);
            }

            //Execute suggestions
            suggestResponse = suggestRequest.execute().actionGet();

        } catch (Exception e) {
           logger.error("Exception " +  e.getLocalizedMessage());
        }
        return suggestResponse;
    }

    //Main method for easy debugging
    public static void main(String[] args) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(
                CompletionSuggestion
                        .getSuggestions("Ms-114,120v[7]", 10, "wab"));

        logger.info(jsonString);
    }

}
