package no.uib.marcus.rest.action;


import no.uib.marcus.client.ClientFactory;
import no.uib.marcus.rest.RestConfig;
import no.uib.marcus.search.MarcusSearchBuilder;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Implementing REST API
 * @author Hemed Ali
 * Date: 27 Nov 2016
 **/

@Path("/search")
public class Search extends RestConfig {
        
    @GET
    @Path("/all")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public String getDocumentsByQueryString(){
        MarcusSearchBuilder searchBuilder = new MarcusSearchBuilder(ClientFactory.getTransportClient());
        searchBuilder.setIndices("ska2");
        return  searchBuilder.getDocuments().toString();
    }


    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocumentsByQueryString(@Context UriInfo uri){        
         System.out.println(uri.getQueryParameters().getFirst("q"));
        //MarcusSearchBuilder searchBuilder = new MarcusSearchBuilder(ClientFactory.getTransportClient());
        //searchBuilder.setQueryString(queryString);
        return  Response
                .ok(200)
                .entity(uri.getQueryParameters().getFirst("q"))
                .build();
     }

}
