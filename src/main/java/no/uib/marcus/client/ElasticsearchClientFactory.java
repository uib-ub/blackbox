package no.uib.marcus.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import no.uib.marcus.common.loader.JsonFileLoader;
import no.uib.marcus.common.util.BlackboxUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;


/**
 * A singleton that connects to Elasticsearch cluster through the elasticsearch-java client
 * for build request
 * and then builds a JSON that is used by the Elasticsearch low-level client
 * for creating the request
 *
 *
 */
final public class ElasticsearchClientFactory {
    private static final Logger logger = Logger.getLogger(ElasticsearchClientFactory.class.getName());
    private static ElasticsearchClient elasticsearchClient;

    /**
     * Prevent direct instantiation of this class
     */
    private ElasticsearchClientFactory() {
    }

    /**
     * Create a transport client
     */
    private static ElasticsearchClient createTransportClient(Map<String, String> properties) {
        try {

            Header[] defaultHeader = new Header[]{new BasicHeader("Authorization",
                    "ApiKey " + BlackboxUtils.getValueAsString(properties, "api_key"))};
            RestClient restClient = RestClient.builder(
                            new HttpHost(InetAddress.getByName(BlackboxUtils.getValueAsString(properties, "host")), BlackboxUtils.getValueAsInt(properties, "port"), "https")
                    ).setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setSSLHostnameVerifier((s, sslSession) -> true)).setDefaultHeaders(defaultHeader)
                    .build();

            JacksonJsonpMapper jsonMapper = new JacksonJsonpMapper();
            ElasticsearchTransport elasticsearchTransport = new RestClientTransport(restClient, jsonMapper);
            ElasticsearchClient client = new ElasticsearchClient(elasticsearchTransport);
            HealthResponse hr = client.cluster().health();
            logger.log(Level.INFO, "Connected to Elasticsearch cluster: " + hr);
            return client;
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "Unknown host: " + e.getMessage());
            throw new RuntimeException(e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unable to connect to Elasticsearch cluster. Is Elasticsearch running? ");
            throw new RuntimeException(e);
        }

    }

    /**
     *  Synchronize the call so that different threads do not end up creating multiple instances
     * Do we still need to synchronize after changing to use the java client which uses rest?
     */
    public static synchronized ElasticsearchClient getElasticsearchClient() throws IOException {
        if (elasticsearchClient == null) {
            JsonFileLoader loader = new JsonFileLoader();
            Map<String, String> properties;
            if (System.getenv("ELASTICSEARCH_CLUSTER_NAME").isEmpty()) {
                properties = loader.loadBlackboxConfigFromResource();
                logger.info("Loaded config template from: " + loader.getPathFromResource(JsonFileLoader.CONFIG_TEMPLATE));
            }
            else {
                properties = new HashMap<>();
                properties.put("name", required("ELASTICSEARCH_CLUSTER_NAME"));
                properties.put("node_name", required("ELASTICSEARCH_CLUSTER_NODE_NAME"));
                properties.put("host", required("ELASTICSEARCH_CLUSTER_HOST"));
                properties.put("port", required("ELASTICSEARCH_CLUSTER_PORT"));
                properties.put("api_key", required("ELASTICSEARCH_CLUSTER_API_KEY"));
                logger.info("configuration loaded from env variables");
            }
            elasticsearchClient = createTransportClient(properties);
        }
        return elasticsearchClient;
    }

    //Main method for easy debugging
    public static void main(String[] args) throws IOException {
        getElasticsearchClient();
    }

    private static String required(String envVar) {
        String value = System.getenv(envVar);
        if (value == null) {
            throw new IllegalArgumentException("Environment variable " + envVar + " is required but not set.");
        }
        return value;
    }

    /**
     * Don't allow cloning for this object
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cloning for this object is not supported");
    }
}