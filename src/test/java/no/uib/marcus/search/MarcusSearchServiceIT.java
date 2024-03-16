package no.uib.marcus.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.databind.json.JsonMapper;
import junit.framework.TestCase;
import no.uib.marcus.client.ElasticsearchClientFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.test.ElasticsearchTestCase.awaitBusy;

/**
 * @author Hemed Ali
 *         2016-02-20, UiB
 */

/*
 * To run this test, you need to have Elasticsearch running
 * with the same settings as as Blackbox settings.
 * Make sure you have the same version and cluster name to that of the other nodes.
 */

//@ElasticsearchIntegrationTest.ClusterScope(
//scope = ElasticsearchIntegrationTest.Scope.SUITE, transportClientRatio = 0.0)
public class MarcusSearchServiceIT extends TestCase {

    private static final Logger logger = Logger.getLogger(MarcusSearchServiceIT.class.getName());
    private final String indexName = "test";
    private ElasticsearchClient client;

    @Override
    @Before
    protected void setUp() throws IOException{
        try {
            //Establish a transport client
            client = ElasticsearchClientFactory.getElasticsearchClient();
            //Delete index "test", if exists
            client.admin()
                    .indices()
                    .prepareDelete("test")
                    .execute().actionGet();
        } catch (IndexMissingException ex) {
            logger.info("No index to delete, aborting ...");
        }

        logger.info("=====>Creating index of name: " + indexName);
        ElasticsearchClientFactory.getElasticsearchClient()
                .admin()
                .indices()
                .prepareCreate("test")
                .execute().actionGet();

    }

    @Override
    @After
    protected void tearDown() throws InterruptedException {
        //Delete index "admin-test"
        client.admin()
                .indices()
                .prepareDelete("test")
                .execute().actionGet();
        //Wait for 2 seconds
        awaitBusy(new Predicate<Object>() {
            @Override
            public boolean apply(Object o) {
                return false;
            }
        }, 2, TimeUnit.SECONDS);

        logger.info("Deleted index: " + indexName);

    }

    /**
     * Test of executeSearch() method, of class
     * MarcusSearchServiceOld.
     *
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testGetDocumentsIsNull() throws IOException, InterruptedException {
        logger.info("************Testing executeSearch()*****************");
        logger.info("Indexing documents to index: " + indexName);

        final MarcusSearchBuilder searchService = (MarcusSearchBuilder) SearchBuilderFactory.marcusSearch(client)
                .setIndices(indexName);
        JsonMapper jsonMapper = new JsonMapper();
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("identifier", "ubb-ms-01");
        requestMap.put("title","Zanzibar ni njema, atakae aje");
        //Index 1 document
        client.prepareIndex(indexName, "doc", "1")
                .setSource(requestMap)
                .execute()
                .actionGet();

        //Wait for 2 seconds, then get all documents.
        assertTrue(awaitBusy(new Predicate<Object>() {
            @Override
            public boolean apply(Object o) {
                SearchResponse res = searchService.executeSearch();
                logger.info("Total documents found: " + res.getHits().getTotalHits());

                return res.getHits().getTotalHits() == 1;
            }
        }, 2, TimeUnit.SECONDS));

    }


    /**
     * Test of executeSearch () method, of class
     * MarcusSearchBuilder.
     *
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testGetDocumentsBoolFilter() throws IOException, InterruptedException {
        logger.info("************Testing executeSearch()*****************");
        logger.info("Indexing documents to index: " + indexName);

        final MarcusSearchBuilder searchService = (MarcusSearchBuilder) SearchBuilderFactory.marcusSearch(client)
                .setIndices(indexName);

        //Index 1 document
        client.prepareIndex(indexName, "doc", "2")
                .setSource(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("identifier", "ubb-ms-02")
                        .field("title", "Stone town")
                        .field("status", "signed")
                        .endObject())
                .execute()
                .actionGet();

        //Wait for 2 seconds, then get all documents.
        assertTrue(awaitBusy(new Predicate<Object>() {
            @Override
            public boolean apply(Object o) {
                searchService.setQueryString("stone*");
                searchService.setFilter(FilterBuilders.matchAllFilter());
                SearchResponse res = searchService.executeSearch();
                logger.info("Total documents found: " + res.getHits().getTotalHits());

                return res.getHits().getTotalHits() == 1;
            }
        }, 2, TimeUnit.SECONDS));

    }

}
