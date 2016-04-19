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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A class for handling auto-suggestion.
 * @author Hemed Ali Al Ruwehy (hemed.ruwehy@uib.no)
 * <p>
 * University of Bergen Library
 */
public class CompletionSuggestion {

    private static final Logger logger = Logger.getLogger(CompletionSuggestion.class);

    /**A method to get a list of suggestions.
     * @param text input text
     * @param suggestField a suggestion field
     * @param indices array of one or more setIndices, can be <code>null</code>
     * @return a set of suggestion texts.
     **/
    public static Set<String> getSuggestions(String text, String suggestField, @Nullable String... indices) {
        Set<String> items = new HashSet<>();
        try {
            SuggestResponse suggestResponse = getSuggestionResponse(text, suggestField, indices);
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
     * @param suggestField a suggestion field
     * @param indices array of one or more setIndices, can be <code>null</code>
     * @return a suggestion response.
     **/
    public static SuggestResponse getSuggestionResponse(String text, String suggestField, @Nullable String... indices) {
        SuggestionBuilder suggestionsBuilder = new CompletionSuggestionBuilder("completion_suggestion");
        SuggestResponse suggestResponse = null;
        SuggestRequestBuilder suggestRequest;

        try {
            suggestionsBuilder.text(text);
            suggestionsBuilder.field(suggestField);
            //suggestionsBuilder.size(50);

            suggestRequest = ClientFactory
                    .getTransportClient()
                    .prepareSuggest();

            suggestRequest.addSuggestion(suggestionsBuilder);

            if (indices != null && indices.length > 0) {
                suggestRequest.setIndices(indices);
            }
            suggestResponse = suggestRequest
                    .execute()
                    .actionGet();

        } catch (Exception e) {
            e.getLocalizedMessage();
        }
        return suggestResponse;
    }

    //Main method for easy debugging
    public static void main(String[] args) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(
                CompletionSuggestion
                        .getSuggestions("t", "suggest", "admin"));

        logger.info(jsonString);
    }

}
