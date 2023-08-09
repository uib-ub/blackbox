package no.uib.marcus.common;

//import com.carrotsearch.randomizedtesting.RandomizedTest;
import no.uib.marcus.common.loader.JsonFileLoader;
import no.uib.marcus.common.loader.UnavailableResourceException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

public class SettingsLoaderTest  {
    private final Logger logger = LogManager.getLogger("Test");

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

        Map<String, String> settings = new JsonFileLoader().toMap("settings-test.json");
        //logger.info("Testing if cluster properties file exist: " + !settings.isEmpty());
        assertTrue(settings.size() > 0);
        assertEquals("elasticsearch" , settings.get("name"));
        assertEquals("Blackbox", settings.get("node"));
        assertEquals("uib.no/ub", settings.get("host"));
    }


    /**
     * Testing loading non-existing file from resource
     */
    @Test(expected = UnavailableResourceException.class)
    public void testFileUnavailableFileFromResource() {
        new JsonFileLoader().loadFromResource("hakuna-matata.json");
    }

    /**
     * Testing loading JSON file from resource
     */
    @Test
    public void testLoadingJsonFileFromResource() throws IOException {
        //Since the config file is not exposed to version-control,
        //We use this for testing..
        String fileName = "config.template.example.json";
        JsonFileLoader loader = new JsonFileLoader();
        String jsonString;
        Map<String, String> settings = null;
        try {
            settings = loader.loadFromResource(fileName).get("cluster");
        } catch (UnavailableResourceException ex) {
            //Load from resource
            fileName = "config.template.example.json";
          //  jsonString = loader.loadFromResource(fileName);
        }

        logger.info("Validating config file from: " + loader.getPathFromResource(fileName));
        assert settings != null;
      //  assertTrue(!settings.isEmpty());
        assertNotNull(settings);

        assertTrue("Does cluster name exist? : " + settings.keySet(), !settings.get("name").isEmpty());
        assertTrue("Does host name exist? : ", !settings.get("host").isEmpty());
        assertTrue("Does port exist? : ", !settings.get("port").isEmpty());
    }


}
