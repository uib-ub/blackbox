package no.uib.marcus.common.util;

import co.elastic.clients.elasticsearch.core.SearchResponse;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import no.uib.marcus.common.Params;

import jakarta.servlet.http.HttpServletRequest;
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
    public static String createLogMessage(HttpServletRequest request, SearchResponse<ObjectNode> searchResponse) throws IOException {
        //Get a copy of a parameter map
        Map<String, Object> parameterMapCopy = new HashMap<>(request.getParameterMap());
        //Remove aggregations from the logs
        parameterMapCopy.remove(Params.AGGREGATIONS);
        //Build log message
        Map<String, Object> responseMap = new HashMap<>();

        responseMap.put("params", jsonify(parameterMapCopy));
        responseMap.put("hits", searchResponse == null ? -1 : searchResponse.hits().total().toString());
        responseMap.put("took", searchResponse == null ? -1 : searchResponse.took());

        ObjectNode objectNode = new JsonMapper().createObjectNode()
                .setAll(new JsonMapper().convertValue(jsonify(responseMap),ObjectNode.class));
        objectNode

        //@todo not able to find status .field("status", searchResponse == null? 404 : searchResponse.status().getStatus())
            //    .put("stats", searchResponse.terminatedEarly()hits() )
                .put("hits", searchResponse == null ? "-1" : searchResponse.hits().total().toString())
                .put("took", searchResponse == null ? -1 : searchResponse.took());
        return  objectNode.toPrettyString();
    }

}
