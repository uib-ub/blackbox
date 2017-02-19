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
    public void testLoadingJsonFile() throws Exception {
        String properties = "{\n" +
                "  \"ubb_cluster\": {\n" +
                "    \"name\": \"elasticsearch\",\n" +
                "    \"node_name\": \"Blackbox\",\n" +
                "    \"host\": \"uib.no/ub\",\n" +
                "    \"transport_port\": 80\n" +
                "  }\n" +
                "}";

        Map<String, String> settings = new JsonFileLoader().toMap(properties);
        logger.info("Testing if cluter properties file exist: " + !settings.isEmpty());
        assertTrue(!settings.isEmpty());
        assertEquals("elasticsearch" , settings.get("ubb_cluster.name"));
        assertEquals("Blackbox" , settings.get("ubb_cluster.node_name"));
        assertEquals("uib.no/ub" , settings.get("ubb_cluster.host"));
    }

    /**
     * Testing loading JSON file from resource
     */
    @Test
    public void testLoadingJsonFileFromResource() throws IOException {
        String fileName = "blackbox.json";
        JsonFileLoader loader = new JsonFileLoader();
        String jsonString = loader.loadFromResourceFolder(fileName);
        logger.info("Testing blackbox file settings from: " + loader.getPathFromResource(fileName));
        Map<String, String> settings = new JsonFileLoader().toMap(jsonString);
        assertNotNull(jsonString);
        assertNotNull(settings);
        assertTrue(!jsonString.isEmpty());
        logger.info("Testing if cluster name exist : " + !settings.get("ubb_cluster.name").isEmpty());
        assertEquals("elasticsearch" , settings.get("ubb_cluster.name"));
    }
}
