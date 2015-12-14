
package no.uib.marcus.search.client;

import java.net.InetAddress;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

/**
 * A singleton class that connect to Elasticsearch cluster through Transport client
 * <br>
 * Note that you should define the same clustername as the one you defined on your running nodes. 
 * Otherwise, your Transport Client won't connect to the node.
 * Note also that you must define the transport client port (9300-9399) and not the REST port (9200-9299). 
 * Transport client does not use REST API.
 */
 
public class ClientFactory {
    
    private static Client client; 
    
       /** We want to prevent direct instantiation of this class **/   
        private ClientFactory(){}
    
        private static Client createTransportClient(){
            try{
                    Settings settings = ImmutableSettings.settingsBuilder()
                      .put("cluster.name", "elasticsearch")
                      .build();

                    client = new TransportClient(settings)
                      /**
                       * You can add more than one addresses here, depending on the number of your servers
                       * For now, we are connecting locally.
                       */
                      .addTransportAddress(
                              new InetSocketTransportAddress(InetAddress.getLocalHost(), 9300));
                      //ClusterHealthResponse hr = client.admin().cluster().prepareHealth().get();  
            }
            catch(Exception e){
                e.getLocalizedMessage();
            }

            return client;
        }
    
      /*Syncronize the call so that different threads do not end up creating different instances**/
       public static synchronized Client getTransportClient(){
            if(client == null){
                  client = createTransportClient();
               }
          return client;
        } 

        @Override
        protected Object clone() throws CloneNotSupportedException {
           throw new CloneNotSupportedException("Cloning for this object is not supported");
        }
       
       
}
