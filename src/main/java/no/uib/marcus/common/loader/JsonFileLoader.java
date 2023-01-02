package no.uib.marcus.common.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import  org.elasticsearch.xcontent.json.JsonXContent;

import org.elasticsearch.common.settings.JsonSettingsLoader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/**
 * A class that provides utility methods for loading JSON config file.
 * <p>
 * author Hemed Ali
 */
public class JsonFileLoader extends JsonSettingsLoader {

    /* https://www.javadoc.io/doc/org.elasticsearch/elasticsearch/latest/org.elasticsearch.server/org/elasticsearch/common/settings/Settings.Builder.html
    * Use builder
    *  */
    public final static String CONFIG_TEMPLATE = "config.template.json";
    private final Logger logger = LogManager.getLogger(getClass().getName());

    /**
     * Get the file path from the resource folder
     *
     * @param fileName a file name
     * @return a resource path
     */
    public String getPathFromResource(String fileName) {
        String filePath;
        try {
            filePath = getClass().getClassLoader().getResource(fileName).getPath();
        } catch (NullPointerException ex) {
            //throw meaningful exception instead
            throw new UnavailableResourceException("Unavailable config file with name [" + fileName + "] " +
                    "in src/main/resources");
        }
        return filePath;
    }

    /**
     * Load file from resource folder
     *
     * @param fileName
     * @return returns a string representation of file contents
     */
    public String loadFromResource(String fileName) {
        return loadFromStream(getPathFromResource(fileName));
    }

    /**
     * Read JSON file from stream and get file contents as strings
     *
     * @param filePath a file path
     * @return returns a string representation of the file contents.
     */
    public String loadFromStream(String filePath) {
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filePath));
        } catch (FileNotFoundException e) {
            logger.error("File path does not exist for " + filePath);
            throw new UnavailableResourceException("Unavailable file for blackbox settings. " +
                    "Make sure this path exist: " + filePath);
        }
        JsonElement json = new JsonParser().parse(reader);
        return json.toString();
    }

    /**
     * Convert a JSON string to Java map
     *
     * @param source a valid JSON string
     * @return a Java map which is the result of JSON string
     * @throws IOException
     */
    public Map<String, String> toMap(String source) throws IOException {
        return super.load(source);
    }

    /**
     * A wrapper for loading config file from resource
     *
     * @return a file converted to Java map
     */
    public Map<String, String> loadBlackboxConfigFromResource() throws IOException {
        return toMap(loadFromResource(CONFIG_TEMPLATE));
    }

}
