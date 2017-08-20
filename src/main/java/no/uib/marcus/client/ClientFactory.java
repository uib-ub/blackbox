package no.uib.marcus.client;

import no.uib.marcus.common.util.BlackboxUtils;
import no.uib.marcus.common.loader.JsonFileLoader;
import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
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

    private static Client client;
    private static final Logger logger = Logger.getLogger(ClientFactory.class);

    /**
     * Prevent direct instantiation of this class
     */
    private ClientFactory() {}

    /**
     * Create a transport client
     **/
    private static Client createTransportClient(Map<String, String> properties){
         try {
            Settings settings = ImmutableSettings.settingsBuilder()
                    .put("cluster.name", BlackboxUtils.getValueAsString(properties, "cluster.name"))
                    .put("node.name", BlackboxUtils.getValueAsString(properties, "cluster.node_name"))
                    .build();
            client = new TransportClient(settings)
                    //You can add more than one addresses here, depending on the number of your servers.
                    /*.addTransportAddress(new InetSocketTransportAddress(
                            //Use localhost in production
                            InetAddress.getLocalHost(), getValueAsInt(properties, "ubbcluster.port")));
                     */
                   .addTransportAddress(new InetSocketTransportAddress(
                                    InetAddress.getByName(BlackboxUtils.getValueAsString(properties , "cluster.host")),
                                    BlackboxUtils.getValueAsInt(properties, "cluster.port")));

            ClusterHealthResponse hr = client.admin().cluster().prepareHealth().get();
            logger.info("Connected to Elasticsearch cluster: " + hr);
        } catch (UnknownHostException ue) {
            logger.error("Unknown host: " + ue.getMessage());
        } catch (ElasticsearchException e) {
            if (e instanceof ConnectTransportException) {
                logger.warn("Unable to connect to Elasticsearch. Is it running? ");
            }
            logger.error(e.getDetailedMessage());
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


    /**
     * Don't allow cloning for this object
     **/
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cloning for this object is not supported");
    }

    //Main method for easy debugging..
    public static void main(String[] args) throws IOException {
        getTransportClient();
    }


}
