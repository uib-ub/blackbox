package no.uib.marcus.common;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import no.uib.marcus.common.loader.JsonFileLoader;
import no.uib.marcus.common.loader.UnavailableResourceException;
import java.util.logging.Logger;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;

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

        Map<String, String> settings = new  JsonFileLoader().toMap("settings-loader-test.json");
        //logger.info("Testing if cluster properties file exist: " + !settings.isEmpty());
        assertTrue(!settings.isEmpty());
        assertEquals("elasticsearch", settings.get("name"));
        assertEquals("Blackbox", settings.get("node_name"));
        assertEquals("uib.no/ub", settings.get("host"));
    }


    /**
     * Testing loading non-existing file from resource
     */
    @Test(expected = UnavailableResourceException.class)
    public void testFileUnavailableFileFromResource() throws IOException {
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
        // String jsonString;
        Map<String, Map> jsonStringMap;
        try {
            // jsonString = loader.loadFromResource(fileName);
            jsonStringMap = loader.loadFromResource(fileName);
        } catch (UnavailableResourceException ex) {
            //Load from resource
            fileName = "settings-loader-test.json";
            // jsonString = loader.loadFromResource(fileName);
            jsonStringMap = loader.loadFromResource(fileName);
        }

        logger.info("Validating config file from: " + loader.getPathFromResource(fileName));
        // TODO: Simply try to convert Map to String to fix error here - Rui
        String jsonString = jsonStringMap.toString();
        Map<String,String> settings = new  JsonFileLoader().toMap(fileName);
        //Map<String, String> settings = loader.loadFromResource(fileName);
        assertNotNull(jsonString);
        assertNotNull(settings);
        assertTrue(!jsonString.isEmpty());
        logger.info("settings: " + settings.toString());

        assertTrue("Does cluster name exist? : ", !settings.get("name").isEmpty());
        assertTrue("Does host name exist? : ", !settings.get("host").isEmpty());
        assertTrue("Does port exist? : ", !settings.get("port").isEmpty());
    }


}
