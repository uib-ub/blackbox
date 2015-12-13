
package no.uib.marcus.search.connection;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

/**
 * Connect to Elasticsearch cluster through Transport client
 * <br>
 * Note that you should define the same clustername as the one you defined on your running nodes. 
 * Otherwise, your Transport Client won't connect to the node.
 * Note also that you must define the transport client port (9300-9399) and not the REST port (9200-9299). 
 * Transport client does not use REST API.
 */
 
public class ConnectionFactory {
    
    private static Client client = null; 
    
    public static Client getTransportClient(){
        try{
               //Establish HTTP Transport client to join the Elasticsearch cluster
                client = new TransportClient(ImmutableSettings.settingsBuilder()
                  .put("cluster.name", "elasticsearch").build())
                  .addTransportAddress(
                          new InetSocketTransportAddress("127.0.0.1" , 9300));
                  ClusterHealthResponse hr = client.admin().cluster().prepareHealth().get();  
        }
        catch(Exception e){
            e.getLocalizedMessage();
        }
        
        return client;
    }
    
}
