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
        Map<String, String> settings = new JsonFileLoader().toMap("settings-loader-test.json");
        assertFalse(settings.isEmpty());
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
        Map<String, String> settings;
        try {
            settings = loader.toMap(fileName);
        } catch (UnavailableResourceException ex) {
            fileName = "settings-loader-test.json";
            settings = loader.toMap(fileName);
        }

        logger.info("Validating config file from: " + loader.getPathFromResource(fileName));
        assertNotNull(settings);
        assertFalse(settings.isEmpty());
        assertFalse("Does cluster name exist? : ", settings.get("name").isEmpty());
        assertFalse("Does host name exist? : ", settings.get("host").isEmpty());
        assertFalse("Does port exist? : ", settings.get("port").isEmpty());
    }


}
