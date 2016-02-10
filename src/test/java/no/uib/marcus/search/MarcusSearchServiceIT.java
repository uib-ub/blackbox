
package no.uib.marcus.search;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_REPLICAS;
import static org.elasticsearch.cluster.metadata.IndexMetaData.SETTING_NUMBER_OF_SHARDS;
import org.elasticsearch.common.base.Predicate;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.test.ElasticsearchIntegrationTest;
import static org.elasticsearch.test.ElasticsearchIntegrationTest.client;
import static org.elasticsearch.test.ElasticsearchTestCase.awaitBusy;
import org.elasticsearch.test.hamcrest.ElasticsearchAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Hemed
 */
@ElasticsearchIntegrationTest.ClusterScope(scope = ElasticsearchIntegrationTest.Scope.SUITE)
public class MarcusSearchServiceIT extends ElasticsearchIntegrationTest {
            private static final Logger logger = Logger.getLogger(MarcusSearchServiceIT.class);
        
        @Before
        public void setUp() {
                String indexName = "admin-test";
                
                logger.info("=====Creating index of name==== " + indexName);
                // We want to force "admin-test" index to use 1 shard 1 replica
                client().admin().indices().prepareCreate(indexName)
                        .setSettings(ImmutableSettings.builder()
                                .put(SETTING_NUMBER_OF_SHARDS, 1)
                                .put(SETTING_NUMBER_OF_REPLICAS, 0)).get();

        }
        
        @After
        public void tearDown() throws InterruptedException {
                //Delete index "admin-test"
                client().admin().indices().prepareDelete("admin-test");
                //Wait for 5 seconds
                awaitBusy(new Predicate<Object>() {
                        @Override
                        public boolean apply(Object o) {
                                return false;
                        }
                }, 5, TimeUnit.SECONDS);

        }

        /**
         * Test of getAllDocuments method, of class MarcusSearchService.
         */
        @Test
        public void testGetAllDocuments() throws IOException {
                 logger.info("Testing getAllDocuments()");
                 logger.info("Index some documents");
                 MarcusSearchService service = new MarcusSearchService();
                 String[] index = {"admin-index"};
                 String [] type = { "doc" }; 
               
                 index("admin-test", "doc", XContentFactory.jsonBuilder()
                         .startObject()
                         .field("identifier", "ubb-ms-01")
                         .field("title", "Zanzibar ni njema, atakae aje")
                         .endObject());
                 
                 service.setIndices(index);
                 service.setTypes(type);
                 
                 long cout = client().prepareCount(index).get().getCount();
                 
                 logger.info("Total counts: " + cout);
                 
                  ElasticsearchAssertions.assertHitCount(client().prepareCount(index).get(), 1);
        }

        
}
