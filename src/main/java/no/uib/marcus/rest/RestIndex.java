package no.uib.marcus.rest;

import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

/**
 * Root for REST endpoint that will print HTML
 * @author Hemed
 */

@Path("/")
public class RestIndex extends RestConfig {
    
     /**
     * Return html page with information about REST api. It contains methods all
     * methods provide by REST api.
     *
     * @param servletContext Context of the servlet container.
     * @return HTML page which has information about all methods of REST API.
     */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public String sayHtmlHello(@Context ServletContext servletContext){
        return "<html>"
                + "<title>Blackbox RESTful API </title>" +
                    "<body>" +
                            "<h1>Blackbox REST API</h1>" +
                            "Server path: " + servletContext.getContextPath() + "/rest" +
                            "<h2>Index</h2>" +
                                 "<ul>" +
                                    "<li>GET /api - Returns this page.</li>" +
                                    "<li>GET /test - Returns a test page.</li>" +
                                "</ul>" +
                    "</body>" +
                "</html>";
     }
    
   /**
     * Method only for testing whether the REST API is running.
     *
     * @return String "REST API is running."
     */
    @GET
    @Path("/test")
    public String test(){
        return "REST API is up and running...";
    }

}
