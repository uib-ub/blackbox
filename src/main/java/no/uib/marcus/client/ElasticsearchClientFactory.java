package no.uib.marcus.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.HealthResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import no.uib.marcus.common.loader.JsonFileLoader;
import no.uib.marcus.common.util.BlackboxUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.elasticsearch.client.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;


/**
 * A singleton that connects to Elasticsearch cluster through HighLevel Request client
 * <p>
 * The Client uses the REST ports (default 9200)
 */
final public class ElasticsearchClientFactory {

    private static final Logger logger = Logger.getLogger(ElasticsearchClientFactory.class.getName());
    private static ElasticsearchClient elasticsearchClient;

    private static final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();


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

            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(BlackboxUtils.getValueAsString(properties, "username"), BlackboxUtils.getValueAsString(properties, "password")));

            RestClient restClient = RestClient.builder(
                            new HttpHost(InetAddress.getByName(BlackboxUtils.getValueAsString(properties, "host")), BlackboxUtils.getValueAsInt(properties, "port"), "https")
                    ).setHttpClientConfigCallback(httpAsyncClientBuilder -> httpAsyncClientBuilder.setDefaultCredentialsProvider(
                            credentialsProvider
                    ).setSSLHostnameVerifier((s, sslSession) -> true))
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
     * Syncronize the call so that different threads do not end up creating multiple instances
     * Do we still need synchronize after changing to use the java client which uses rest?
     */
    public static synchronized ElasticsearchClient getElasticsearchClient() throws IOException {
        if (elasticsearchClient == null) {
            JsonFileLoader loader = new JsonFileLoader();
            Map<String, String> properties = loader.loadBlackboxConfigFromResource();
            logger.info("Loaded config template from: " + loader.getPathFromResource(JsonFileLoader.CONFIG_TEMPLATE));
            elasticsearchClient = createTransportClient(properties);
        }
        return elasticsearchClient;
    }

    //Main method for easy debugging..
    public static void main(String[] args) throws IOException {
        getElasticsearchClient();
    }

    /**
     * Don't allow cloning for this object
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cloning for this object is not supported");
    }


}


