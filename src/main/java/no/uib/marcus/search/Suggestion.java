package no.uib.marcus.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import no.uib.marcus.search.client.ClientFactory;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

/**
 * Hemed
 */
public class Suggestion {

    public static Set<String> getSuggestionsFor(String text, String indexType) {
        Set<String> items = new HashSet<>();
        CompletionSuggestionBuilder suggestionsBuilder = new CompletionSuggestionBuilder("completeMe");

        try {
            suggestionsBuilder.text(text);
            suggestionsBuilder.field("suggest");

            SuggestRequestBuilder suggestRequestBuilder = ClientFactory.getTransportClient()
                    .prepareSuggest(indexType)
                    .addSuggestion(suggestionsBuilder);

            SuggestResponse suggestResponse = suggestRequestBuilder.execute().actionGet();

            Iterator<? extends Suggest.Suggestion.Entry.Option> iterator = suggestResponse
                    .getSuggest()
                    .getSuggestion("completeMe")
                    .iterator()
                    .next()
                    .getOptions()
                    .iterator();

            while (iterator.hasNext()) {
                Suggest.Suggestion.Entry.Option next = iterator.next();
                items.add(next.getText().string());
            }
        } catch (Exception e) {
            e.getLocalizedMessage();
        }
        return items;
    }
    
}
