package no.uib.marcus.search.client;

import org.apache.log4j.Logger;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.ConnectTransportException;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * A singleton class that connect to Elasticsearch cluster through Transport
 * client
 * <br>
 * Note that you should define the same clustername as the one you defined on
 * your running nodes. Otherwise, your Transport Client won't connect to the
 * node. Note also that you must define the transport client port (9300-9399)
 * and not the REST port (9200-9299). Transport client does not use REST API.
 */
public class ClientFactory {

        private static final Logger logger = Logger.getLogger(ClientFactory.class);
        private static Client client;

        /**
         * We want to prevent direct instantiation of this class, thus we create
         * private constructor
         */
        private ClientFactory() {
        }

        private static Client createTransportClient(){
                try {
                        Settings settings = ImmutableSettings.settingsBuilder()
                                .put("cluster.name", "elasticsearch")
                                //.put("client.transport.ping_timeout", "100s")
                                .build();
                        client = new TransportClient(settings)
                                /*
                                 * You can add more than one addresses here,
                                 * depending on the number of your servers.
                                 */
                                .addTransportAddress(
                                        //new InetSocketTransportAddress(InetAddress.getLocalHost(), 9300));
                                        new InetSocketTransportAddress(InetAddress.getByName("kirishima.uib.no"), 9300));

                        ClusterHealthResponse hr = client.admin().cluster().prepareHealth().get();
                        logger.info("Connected to Elasticsearch cluster: " + hr);
                } catch (UnknownHostException ue) {
                        logger.error("Unknown host: " + ue.getMessage());
                } catch (ElasticsearchException e) {
                        if(e instanceof ConnectTransportException){
                            logger.warn("Unable to connect to Elasticsearch. Is it running? ");
                        }
                        logger.error(e.getDetailedMessage());
                }
                return client;
        }

        /*Syncronize the call so that different threads do not end up creating different instances**/
        public static synchronized Client getTransportClient() {
                if (client == null) {
                        client = createTransportClient();
                }
                return client;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
                throw new CloneNotSupportedException("Cloning for this object is not supported");
        }
        
        //Main method for easy debugging..
        public static void main(String[] args) {
                getTransportClient();
        }

}
