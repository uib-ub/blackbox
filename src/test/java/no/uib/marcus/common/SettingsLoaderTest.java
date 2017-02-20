package no.uib.marcus.common;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import no.uib.marcus.common.loader.JsonFileLoader;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class SettingsLoaderTest extends RandomizedTest {
    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Test using fixed values
     */
    @Test
    public void testReadingJsonFileProperties() throws Exception {
        String properties = "{\n" +
                "  \"ubbcluster\": {\n" +
                "    \"name\": \"elasticsearch\",\n" +
                "    \"node_name\": \"Blackbox\",\n" +
                "    \"host\": \"uib.no/ub\",\n" +
                "    \"transport_port\": 80\n" +
                "  }\n" +
                "}";

        Map<String, String> settings = new JsonFileLoader().toMap(properties);
        logger.info("Testing if cluster properties file exist: " + !settings.isEmpty());
        assertTrue(!settings.isEmpty());
        assertEquals("elasticsearch", settings.get("ubbcluster.name"));
        assertEquals("Blackbox", settings.get("ubbcluster.node_name"));
        assertEquals("uib.no/ub", settings.get("ubbcluster.host"));
    }

    /**
     * Testing loading JSON file from resource
     */
    @Test
    public void testLoadingJsonFileFromResource() throws IOException {
        String fileName = "blackbox-test.json";
        JsonFileLoader loader = new JsonFileLoader();
        String jsonString = loader.loadFromResource(fileName);
        logger.info("Testing blackbox config file from: " + loader.getPathFromResource(fileName));
        Map<String, String> settings = loader.toMap(jsonString);
        assertNotNull(jsonString);
        assertNotNull(settings);
        assertTrue(!jsonString.isEmpty());
        logger.info("Testing if cluster name exist : " + !settings.get("ubbcluster.name").isEmpty());
        logger.info("Testing if host name exist : " + !settings.get("ubbcluster.host").isEmpty());
        logger.info("Testing if port exist : " + !settings.get("ubbcluster.port").isEmpty());
    }
}
