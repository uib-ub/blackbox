package no.uib.marcus.search;

import com.google.gson.Gson;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import no.uib.marcus.search.client.ClientFactory;
import org.apache.log4j.Logger;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder.SuggestionBuilder;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

/**
 * Hemed Ali (hemed.ruwehy@uib.no) University of Bergen Library
 */
public class Suggestion {

        private static final Logger logger = Logger.getLogger(Suggestion.class);

        public static SuggestResponse getSuggestResponse(String text, String suggestField, String... indices) {
                SuggestionBuilder suggestionsBuilder = new CompletionSuggestionBuilder("completion_suggestion");
                SuggestResponse suggestResponse = null;
                SuggestRequestBuilder suggestRequest;

                try {
                        suggestionsBuilder.text(text);
                        suggestionsBuilder.field(suggestField);

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

        public static Set<String> getSuggestions(String text, String suggestField, String... indices) {
                Set<String> items = new HashSet<>();
                try {
                        SuggestResponse suggestResponse = getSuggestResponse(text, suggestField, indices);
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
                        logger.error(e.getLocalizedMessage());
                }
                return items;
        }
        
        //Main method for easy debugging
        public static void main(String[] args) {
                Gson gson = new Gson();
                String jsonString = gson.toJson(
                        Suggestion
                        .getSuggestions("t", "suggest", "admin"));

                logger.info(jsonString);
        }

}
