package no.uib.marcus.search;

import no.uib.marcus.client.ClientFactory;
import no.uib.marcus.common.util.FilterUtils;
import no.uib.marcus.common.util.QueryUtils;
import no.uib.marcus.range.DateRange;
import org.elasticsearch.client.Client;

import java.io.IOException;

/**
 * A runner for the sake of easy debugging
 */
public class BuilderRunner {

    public static void main(String[] args) throws IOException {
        Client c = ClientFactory.getTransportClient();
        String[] selectedFilters = {"hasGenreForm.exact#Bok"};
        SearchBuilder service = SearchBuilderFactory.marcusSearch(c);
        service.setSelectedFacets(FilterUtils.buildFilterMap(selectedFilters));
        service.setIndices("marcus-admin");
        service.setTypes("document");
        service.setSize(2);
        String aggs = "[{" +
                "\"field\": \"type.exact\"," +
                "\"size\": 10," +
                "\"operator\": \"OR\"," +
                "\"order\": \"count_desc\"," +
                "\"min_doc_count\": 1" +
                "}, {" +
                "\"label\": \"genreform\"," +
                "\"field\": \"hasGenreForm.exact\"," +
                "\"size\": 10," +
                "\"operator\": \"OR\"," +
                "\"order\": \"count_desc\"," +
                "\"min_doc_count\": 1" +
                "}, {" +
                "\"field\": \"hasZoom\"," +
                "\"size\": 2," +
                "\"operator\": \"AND\"," +
                "\"order\": \"term_asc\"," +
                "\"min_doc_count\": 0" +
                "}, {" +
                "\"field\": \"isDigitized\"," +
                "\"size\": 2," +
                "\"operator\": \"AND\"," +
                "\"order\": \"count_desc\"," +
                "\"min_doc_count\": 0" +
                "}, {" +
                "\"field\": \"subject.exact\"," +
                "\"size\": 10," +
                "\"operator\": \"AND\"," +
                "\"order\": \"count_desc\"," +
                "\"min_doc_count\": 1" +
                "}, {" +
                "\"field\": \"isPartOf.exact\"," +
                "\"size\": 10," +
                "\"operator\": \"OR\"," +
                "\"order\": \"count_desc\"," +
                "\"min_doc_count\": 1" +
                "}, {" +
                "\"field\": \"producedIn.exact\"," +
                "\"size\": 10," +
                "\"operator\": \"AND\"," +
                "\"order\": \"count_desc\"," +
                "\"min_doc_count\": 1" +
                "}, {" +
                "\"field\": \"maker.exact\"," +
                "\"size\": 10," +
                "\"operator\": \"AND\"," +
                "\"order\": \"count_desc\"," +
                "\"min_doc_count\": 1" +
                "}]";
        //service.setAggregations(aggs);
        service.setPostFilter(
                FilterUtils.getTopFilter(FilterUtils.buildFilterMap(selectedFilters),
                        aggs,
                        new DateRange("1999-07-02", "600"))
        );
        System.out.println(QueryUtils.toJsonString(service.executeSearch(), true));
        //System.out.println("Is valid year?: " + DateRange.isXSDgYear("1920"));
    }
}
