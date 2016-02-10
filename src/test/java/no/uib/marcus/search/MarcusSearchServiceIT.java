package no.uib.marcus.search;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import no.uib.marcus.search.client.ClientFactory;
import org.apache.log4j.Logger;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_SHARDS;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.common.base.Predicates;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.indices.IndexMissingException;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.client;
import static org.elasticsearch.test.ElasticsearchTestCase.awaitBusy;
import org.elasticsearch.test.hamcrest.ElasticsearchAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import sun.print.resources.serviceui;

/**
 * @author Hemed Ali Al Ruwehy
 */

/** 
 * To run this test, you need to have Elasticsearch running 
 * with the same settings as Transport client settings.
 * Make sure you have the same version to that of the other nodes and the same cluster name.
 ***/

//@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.SUITE)
public class MarcusSearchServiceIT extends TestCase {

        private static final Logger logger = Logger.getLogger(MarcusSearchServiceIT.class);
        private String indexName = "test";

        @Before
        public void setUp() {
                try {
                        //Delete index "test", if exists
                        ClientFactory.getTransportClient()
                                .admin()
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

        @After
        @Override
        public void tearDown() throws InterruptedException {
                //Delete index "admin-test"
                ClientFactory.getTransportClient()
                        .admin()
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
         * Test of getDocuments (String queryString) method, of class
         * MarcusSearchService.
         *
         * @throws java.io.IOException
         * @throws java.lang.InterruptedException
         */
        @Test
        public void testGetDocumentsIsNull() throws IOException, InterruptedException {
                logger.info("************Testing getDocuments(String queryString)*****************");
                logger.info("Indexing documents to index: " + indexName);

                final MarcusSearchService service = new MarcusSearchService();
                service.setIndices(indexName);

                //Index 1 document
                ClientFactory.getTransportClient()
                        .prepareIndex("test", "doc", "1")
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
                                SearchResponse res = service.getDocuments(null);
                                logger.info("Total documents found: " + res.getHits().getTotalHits());

                                return res.getHits().getTotalHits() == 1;
                        }
                }, 2, TimeUnit.SECONDS));

        }

}
