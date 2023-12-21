package no.uib.marcus.common.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.util.JSONPObject;
import no.uib.marcus.common.Params;
import co.elastic.clients.elasticsearch.core.SearchResponse;


import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static no.uib.marcus.common.util.BlackboxUtils.jsonify;

/**
 * @author Hemed
 */
public final class LogUtils {

    private LogUtils (){}


    /**Log what has been queried
     *
     * @param request HTTP request
     * @param searchResponse a search response.
     *
     * @return  logs a JSON string
     ***/
    public static String createLogMessage(HttpServletRequest request, SearchResponse<JsonNode> searchResponse) throws IOException {
        //Get a copy of a parameter map
        Map<String, Object> parameterMapCopy = new HashMap<>(request.getParameterMap());
        //Remove aggregations from the logs
        parameterMapCopy.remove(Params.AGGREGATIONS);
        JSONPObject searchResponseJson = new JSONPObject(searchResponse.toString());
        searchResponseJson
        //Build log message
        XContentBuilder builder = XContentFactory.jsonBuilder()
                .startObject()
                //.field("host", request.getRemoteAddr().equals("0:0:0:0:0:0:0:1") ?
                // InetAddress.getLocalHost() : request.getRemoteAddr())
                .field("status", searchResponse == null? 404 : searchResponse.status().getStatus())
                .field("params", jsonify(parameterMapCopy))
                .field("took", searchResponse == null ? -1 : searchResponsegetTook())
                .endObject();
        return  builder.toString();
    }

}
