package no.uib.marcus.client;

import no.uib.marcus.common.loader.JsonFileLoader;
import no.uib.marcus.common.util.BlackboxUtils;
import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthRequest;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.ClusterAdminClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.transport.ConnectTransportException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;


/**
 * A singleton that connects to Elasticsearch cluster through Transport client
 * <p>
 * Note that you should define the same cluster name as the one you defined on
 * your running nodes. Otherwise, your Transport Client won't connect to the
 * node. Note also that you must define the transport client port (9300-9399)
 * and not the REST port (9200-9299). Transport client does not use REST API.
 */
final public class ClientFactory {

    private static final Logger logger = Logger.getLogger(ClientFactory.class);
    private static Client client;

    /**
     * Prevent direct instantiation of this class
     */
    private ClientFactory() {
    }

    /**
     * Create a transport client
     */
    private static Client createTransportClient(Map<String, String> properties) {
        try {
            Settings settings = Settings.builder()
                    .put("cluster.name", BlackboxUtils.getValueAsString(properties, "cluster.name"))
                    .put("node.name", BlackboxUtils.getValueAsString(properties, "cluster.node_name"))
                    .build();
            RestHighLevelClient client = new RestHighLevelClient(
                    RestClient.builder(
                            new HttpHost(InetAddress.getByName(BlackboxUtils.getValueAsString(properties, "cluster.host")),  BlackboxUtils.getValueAsInt(properties, "cluster.port"), "https")
                         ));

            ClusterHealthResponse hr = client.cluster().health(new ClusterHealthRequest());
            logger.info("Connected to Elasticsearch cluster: " + hr);
        } catch (UnknownHostException ue) {
            logger.error("Unknown host: " + ue.getMessage());
        } catch (ConnectTransportException e) {
            logger.warn("Unable to connect to Elasticsearch cluster. Is Elasticsearch running? "
                    + e.getDetailedMessage());
        }
        return client;
    }

    /**
     * Syncronize the call so that different threads do not end up creating multiple instances
     */
    public static synchronized Client getTransportClient() throws IOException {
        if (client == null) {
            JsonFileLoader loader = new JsonFileLoader();
            Map<String, String> properties = loader.loadBlackboxConfigFromResource();
            logger.info("Loaded config template from: " + loader.getPathFromResource(JsonFileLoader.CONFIG_TEMPLATE));
            client = createTransportClient(properties);
        }
        return client;
    }

    //Main method for easy debugging..
    public static void main(String[] args) throws IOException {
        getTransportClient();
    }

    /**
     * Don't allow cloning for this object
     */
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cloning for this object is not supported");
    }


}
