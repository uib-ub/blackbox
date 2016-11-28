package no.uib.marcus.rest.action;


import java.util.List;
import javax.servlet.http.HttpServletRequest;
import no.uib.marcus.client.ClientFactory;
import no.uib.marcus.rest.RestConfig;
import no.uib.marcus.search.MarcusSearchBuilder;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Implementing RESTful API.
 * We are considering to extend the application for using
 * REST API rather than simple Servlet because we may want to
 * support more than just GET/POST verbs in the future.
 * Note: We must have authentication for PUT/POST/HEAD etc
 * 
 * <p/>
 * @author Hemed Ali
 * Date: 27 Nov 2016
 **/

@Path("/search")
public class Search {
        
    /**
     * Testing: Get documents.
     **/
    @GET
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocuments(@QueryParam("q") String queryString, 
            @QueryParam("type") List<String> types,
            @QueryParam("index") List<String> indices,
            @QueryParam("aggs") String aggs,
            @Context HttpServletRequest request){ 
        
        MarcusSearchBuilder searchBuilder = new MarcusSearchBuilder(ClientFactory.getTransportClient());
        searchBuilder.setIndices("ska2");
        System.out.println("Types: " + types.toString());
        System.out.println("Request: " + request.getParameter("q"));
        searchBuilder.setQueryString(queryString);
        return  Response
                .ok(searchBuilder.getDocuments().toString())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_TYPE.withCharset("UTF-8"))
                .build();
     }

}
