package no.uib.marcus.rest;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;


@ApplicationPath("rest")
public class RestConfig extends Application {
        
    /**@GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response showInfoResponse(){
       return Response
               .status(Response.Status.OK)
               .entity("{\"info\" : \" Welcome to the Blackbox REST endpoint\"}")
               .build();
    }**/
}
