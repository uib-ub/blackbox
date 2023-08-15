package no.uib.marcus.client;

import no.uib.marcus.common.loader.JsonFileLoader;
import no.uib.marcus.common.util.BlackboxUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.transport.ConnectTransportException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;


/**
 * A singleton that connects to Elasticsearch cluster through HighLevel Request client
 * <p>
 * The Client uses the REST ports (default 9200)
 */
final public class RestHighLevelClientFactory {

    private static final Logger logger = Logger.getLogger(RestHighLevelClientFactory.class.getName());
    private static RestHighLevelClient restHighLevelClient;

    private static final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

    /**
     * Prevent direct instantiation of this class
     */
    private RestHighLevelClientFactory() {
    }

    /**
     * Create a transport client
     */
    private static RestHighLevelClient createTransportClient(Map<String, String> properties) {
        try {
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(BlackboxUtils.getValueAsString(properties, "username"), BlackboxUtils.getValueAsString(properties, "password")));
            Settings settings = Settings.builder()
                    .put("cluster.name", BlackboxUtils.getValueAsString(properties, "name"))
                    .put("node.name", BlackboxUtils.getValueAsString(properties, "node_name"))
                    .build();
            // something here fails
            ClusterHealthResponse hr;
            try {
                RestHighLevelClient restHighLevelClient = new RestHighLevelClientBuilder(
                        RestClient.builder(
                                        new HttpHost(InetAddress.getByName(BlackboxUtils.getValueAsString(properties, "host")), BlackboxUtils.getValueAsInt(properties, "port"), "https")
                                ).setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(
                                        credentialsProvider
                                ).setSSLHostnameVerifier((s, sslSession) -> true))
                                .build())
                        .setApiCompatibilityMode(true)
                        .build();
                ClusterHealthRequest ch = new ClusterHealthRequest();
                hr = restHighLevelClient.cluster().health(ch, RequestOptions.DEFAULT);
                logger.log(Level.INFO, "Connected to Elasticsearch cluster: " + hr);
                logger.warning("Client: " + RestHighLevelClientFactory.restHighLevelClient.toString());
                return RestHighLevelClientFactory.restHighLevelClient;
            } catch (UnknownHostException ue) {
                logger.log(Level.SEVERE, "Unknown host: " + ue.getMessage());
            } catch (ConnectTransportException e) {
                logger.log(Level.SEVERE, "Unable to connect to Elasticsearch cluster. Is Elasticsearch running? "
                        + e.getDetailedMessage());
            } catch (IOException e) {
                logger.severe(e.getMessage());
                throw new RuntimeException(e);
            }

        } finally {

            // if (client == null) {
            logger.log(Level.WARNING, "finally clause");
            //     return client;
        }

        logger.info("should already have returned from here");
        return restHighLevelClient;
    }



    /**
     * Syncronize the call so that different threads do not end up creating multiple instances
     */
    public static synchronized RestHighLevelClient getHighLevelRestClient() throws IOException {
        if (restHighLevelClient == null) {
            JsonFileLoader loader = new JsonFileLoader();
            Map<String, String> properties = loader.loadBlackboxConfigFromResource();
            logger.info("Loaded config template from: " + loader.getPathFromResource(JsonFileLoader.CONFIG_TEMPLATE));
            restHighLevelClient = createTransportClient(properties);
        }
        return restHighLevelClient;
    }

    //Main method for easy debugging..
    public static void main(String[] args) throws IOException {
        getHighLevelRestClient();
    }

    /**
     * Don't allow cloning for this object
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cloning for this object is not supported");
    }


}


