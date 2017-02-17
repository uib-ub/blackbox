package no.uib.marcus.common;


import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.elasticsearch.common.settings.loader.JsonSettingsLoader;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
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

        Map<String, String> settings = new HashMap<>();
        settings.putAll(new JsonSettingsLoader().load(properties));
        logger.info("Testing if cluter properties file exist: " + !settings.isEmpty());
        assertTrue(!settings.isEmpty());
        logger.info("Testing if cluster name exist : " + !settings.get("ubb_cluster.name").isEmpty());
        assertEquals("elasticsearch" , settings.get("ubb_cluster.name"));
        assertEquals("Blackbox" , settings.get("ubb_cluster.node_name"));
        assertEquals("uib.no/ub" , settings.get("ubb_cluster.host"));
    }

    /**
     * Test using fixed values
     */
    @Test
    public void testLoadingJsonFileFromResource() throws IOException {
        String path = getClass().getClassLoader()
                .getResource("cluster-settings.json")
                .getPath();
        BufferedReader reader = new BufferedReader(new FileReader(path));
        JsonElement json = new JsonParser().parse(reader);
        assertNotNull(json);
        assertTrue(!json.toString().isEmpty());
        logger.info("Contents from JSON cluster settings file: " +  json.toString());
    }
}
