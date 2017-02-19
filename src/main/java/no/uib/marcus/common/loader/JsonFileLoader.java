package no.uib.marcus.common.loader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.elasticsearch.common.settings.loader.JsonSettingsLoader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class JsonFileLoader extends JsonSettingsLoader {
    private final Logger logger = Logger.getLogger(getClass().getName());
    public final static String BLACKBOX_CONFIG_FILE_NAME = "blackbox.json";

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
            //Throw meaningful exception
            throw new UnavailableResourceException("Unavailable file with name [" + fileName +"]");
        }
        return filePath;
    }

    /**
     * Load file from resource folder
     *
     * @param fileName
     * @return returns a string representation of file contents
     */
    public String loadFromResourceFolder(String fileName) {
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

}
