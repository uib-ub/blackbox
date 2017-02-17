package no.uib.marcus.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;
import org.elasticsearch.common.settings.loader.JsonSettingsLoader;

import java.io.*;
import java.util.Map;

public class JsonFileLoader extends JsonSettingsLoader {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final String CLUSTER_FILE_NAME = "cluster-settings.json";

    /**
     * Get the file path from the resource folder
     * @param fileName a file name
     * @return a resource path
     */
    public String getPathFromResource(String fileName){
        return getClass().getClassLoader().getResource(fileName).getPath();
    }

    /**
     * Read file from stream and get file contents as strings
     * @param path a file path
     * @return returns a String representation of this element.
     */
    public String loadFromStream (String path){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(getPathFromResource(path)));
        } catch (FileNotFoundException e) {
          logger.error("File path does not exist for " + path);
        }
        if (reader == null){
            throw new NullPointerException("Unable to read file for cluster settings. " +
                    "Make sure file exists in the resource folder. ");
        }
        JsonElement json = new JsonParser().parse(reader);
        return json.toString();
    }

    @Override
    public Map<String, String> load(String resource) throws IOException {
        return super.load(resource);
    }

}
