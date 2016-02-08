package no.uib.marcus.search;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import no.uib.marcus.search.client.ClientFactory;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

/**
 * Hemed Ali (hemed.ruwehy@uib.no) 
 * University of Bergen Library
 */
public class Suggestion {

        public static SuggestResponse getSuggestResponse(String text, String indexType, String suggestField) {
                CompletionSuggestionBuilder suggestionsBuilder = new CompletionSuggestionBuilder("suggest_me");
                SuggestResponse suggestResponse = null;

                try {
                        suggestionsBuilder.text(text);
                        suggestionsBuilder.field(suggestField);

                        suggestResponse = ClientFactory.getTransportClient()
                                .prepareSuggest(indexType)
                                .addSuggestion(suggestionsBuilder)
                                .execute()
                                .actionGet();

                } catch (Exception e) {
                        e.getLocalizedMessage();
                }
                return suggestResponse;
        }

        public static Set<String> getSuggestions(String text, String indexType, String suggestField) {
                Set<String> items = new HashSet<>();
                try {
                        SuggestResponse suggestResponse = getSuggestResponse(text, indexType, suggestField);
                        Iterator<? extends Suggest.Suggestion.Entry.Option> iterator = suggestResponse
                                .getSuggest()
                                .getSuggestion("suggest_me")
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
