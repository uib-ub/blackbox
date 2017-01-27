package no.uib.marcus.search.suggestion;

import com.google.gson.Gson;
import no.uib.marcus.client.ClientFactory;
import org.apache.log4j.Logger;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

import java.util.*;

/**
 * A class for handling Completion suggestion (auto-suggestion).
 * @author Hemed Ali Al Ruwehy (hemed.ruwehy@uib.no)
 * <p>
 * University of Bergen Library
 */
public class CompletionSuggestion {

    private static final Logger logger = Logger.getLogger(CompletionSuggestion.class);
    private static final String SUGGEST_FIELD = "suggest";

    /**A method to get a list of suggestions.
     * @param text input text
     * @param size Sets the maximum suggestions to be returned per suggest text term.
     * @param indices array of one or more setIndices, can be <code>null</code>
     * @return a set of suggestion texts.
     **/
    public static Set<String> getSuggestions(String text, int size, @Nullable String... indices) {
        //We want suggestion values to be sorted.
        SortedSet<String> items = new TreeSet<>();
        try {
            SuggestResponse suggestResponse = getSuggestionResponse(text, size, indices);
            Iterator<? extends Suggest.Suggestion.Entry.Option> iter = suggestResponse
                    .getSuggest()
                    .getSuggestion("completion_suggestion")
                    .iterator()
                    .next()
                    .getOptions()
                    .iterator();

            while (iter.hasNext()) {
                Suggest.Suggestion.Entry.Option next = iter.next();
                items.add(next.getText().string());
            }
        } catch (Exception e) {
            logger.error("Unable to perform suggestion for text: [" + text + "]") ;
        }
        return items;
    }

    /**A method to get a list of suggestions.
     * @param text input text
     * @param size Sets the maximum suggestions to be returned per suggest text term.
     * @param indices array of one or more setIndices, can be <code>null</code>
     * @return a suggestion response.
     **/
    public static SuggestResponse getSuggestionResponse(String text, int size, @Nullable String... indices) {
        SuggestionBuilder suggestionsBuilder = new CompletionSuggestionBuilder("completion_suggestion");
        SuggestResponse suggestResponse = null;
        SuggestRequestBuilder suggestRequest;

        try {
            suggestionsBuilder.field(SUGGEST_FIELD);
            suggestionsBuilder.text(text);
            suggestionsBuilder.size(size);

            suggestRequest = ClientFactory.getTransportClient().prepareSuggest();
            suggestRequest.addSuggestion(suggestionsBuilder);

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
