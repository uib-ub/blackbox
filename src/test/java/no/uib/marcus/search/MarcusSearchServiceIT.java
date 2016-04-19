package no.uib.marcus.search;

import junit.framework.TestCase;
import no.uib.marcus.client.ClientFactory;
import org.apache.log4j.Logger;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.indices.IndexMissingException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.elasticsearch.test.ElasticsearchTestCase.awaitBusy;

/**
 * @author Hemed Ali
 *         2016-02-20, UiB
 */

/*
 * To run this test, you need to have Elasticsearch running 
 * with the same settings as Transport client settings.
 * Make sure you have the same version and cluster name to that of the other nodes.
 */

//@ElasticsearchIntegrationTest.ClusterScope(
//scope = ElasticsearchIntegrationTest.Scope.SUITE, transportClientRatio = 0.0)
public class MarcusSearchServiceIT extends TestCase {

    private static final Logger logger = Logger.getLogger(MarcusSearchServiceIT.class);
    private String indexName = "test";
    private Client client;

    @Override
    @Before
    protected void setUp() {
        try {
            //Establish a transport client
            client = ClientFactory.getTransportClient();
            //Delete index "test", if exists
            client.admin()
                    .indices()
                    .prepareDelete("test")
                    .execute().actionGet();
        } catch (IndexMissingException ex) {
            logger.info("No index to delete, aborting ...");
        }

        logger.info("=====>Creating index of name: " + indexName);
        ClientFactory.getTransportClient()
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
     * Test of getDocuments() method, of class
     * MarcusSearchServiceOld.
     *
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testGetDocumentsIsNull() throws IOException, InterruptedException {
        logger.info("************Testing getDocuments()*****************");
        logger.info("Indexing documents to index: " + indexName);

        final MarcusSearchBuilder searchService = ServiceFactory.createMarcusSearchService(client)
                .setIndices(indexName);

        //Index 1 document
        client.prepareIndex(indexName, "doc", "1")
                .setSource(XContentFactory.jsonBuilder()
                        .startObject()
                        .field("identifier", "ubb-ms-01")
                        .field("title", "Zanzibar ni njema, atakae aje")
                        .endObject())
                .execute()
                .actionGet();

        //Wait for 2 seconds, then get all documents.
        assertTrue(awaitBusy(new Predicate<Object>() {
            @Override
            public boolean apply(Object o) {
                SearchResponse res = searchService.getDocuments();
                logger.info("Total documents found: " + res.getHits().getTotalHits());

                return res.getHits().getTotalHits() == 1;
            }
        }, 2, TimeUnit.SECONDS));

    }


    /**
     * Test of getDocuments () method, of class
     * MarcusSearchBuilder.
     *
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testGetDocumentsBoolFilter() throws IOException, InterruptedException {
        logger.info("************Testing getDocuments()*****************");
        logger.info("Indexing documents to index: " + indexName);

        final MarcusSearchBuilder searchService = ServiceFactory.createMarcusSearchService(client)
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
                SearchResponse res = searchService.getDocuments();
                logger.info("Total documents found: " + res.getHits().getTotalHits());

                return res.getHits().getTotalHits() == 1;
            }
        }, 2, TimeUnit.SECONDS));

    }

}
