package no.uib.marcus.search;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.validation.constraints.NotNull;
import no.uib.marcus.search.client.ClientFactory;
import org.apache.log4j.Logger;
import org.elasticsearch.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder;

/**
 * Hemed Ali (hemed.ruwehy@uib.no) 
 * University of Bergen Library
 */
public class Suggestion {
        
        private static final Logger logger = Logger.getLogger(Suggestion.class);
        
        public static SuggestResponse getSuggestResponse(String text, @Nullable String[] indices, @NotNull String suggestField) {
                CompletionSuggestionBuilder suggestionsBuilder = new CompletionSuggestionBuilder("completionSuggestion");
                SuggestResponse suggestResponse = null;
                SuggestRequestBuilder suggestRequest;

                try {
                        suggestionsBuilder.text(text);
                        suggestionsBuilder.field(suggestField);
                        
                        suggestRequest = ClientFactory
                                .getTransportClient()
                                .prepareSuggest();
                        
                        if(indices != null && indices.length > 0){
                                suggestRequest.setIndices(indices);
                        }
                        
                        suggestRequest
                                .addSuggestion(suggestionsBuilder);

                        suggestResponse = suggestRequest
                                .execute()
                                .actionGet();

                } catch (Exception e) {
                        e.getLocalizedMessage();
                }
                return suggestResponse;
        }

        public static Set<String> getSuggestions(String text, @Nullable String[] indices, String suggestField) {
                Set<String> items = new HashSet<>();
                try {
                        SuggestResponse suggestResponse = getSuggestResponse(text, indices, suggestField);
                        Iterator<? extends Suggest.Suggestion.Entry.Option> iterator = suggestResponse
                                .getSuggest()
                                .getSuggestion("completionSuggestion")
                                .iterator()
                                .next()
                                .getOptions()
                                .iterator();
                        while (iterator.hasNext()) {
                                Suggest.Suggestion.Entry.Option next = iterator.next();
                                items.add(next.getText().string());
                        }
                } catch (Exception e) {
                        logger.error(e.getLocalizedMessage());
                }
                return items;
        }

}
