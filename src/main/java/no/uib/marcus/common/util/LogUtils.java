package no.uib.marcus.common.util;

import no.uib.marcus.common.RequestParams;
import no.uib.marcus.common.Settings;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Hemed
 */
public final class LogUtils {

    private LogUtils (){}


    /**
     * Log what has been queried
     * @param request HTTP request
     * @param searchResponse a search response.
     ***/
    public static String logSearchResponse(HttpServletRequest request, SearchResponse searchResponse) throws IOException {
        //Get a copy of a parameter map
        Map<String, Object> parameterMapCopy = new HashMap<>(request.getParameterMap());
        //Remove aggregations from the logs
        parameterMapCopy.remove(RequestParams.AGGREGATIONS);
        XContentBuilder builder = XContentFactory.jsonBuilder()
                .startObject()
                //.field("host", request.getRemoteAddr().equals("0:0:0:0:0:0:0:1") ? InetAddress.getLocalHost() : request.getRemoteAddr())
                .field("params", Settings.beautify(parameterMapCopy))
                .field("hits", searchResponse.getHits().getTotalHits())
                .field("took", searchResponse.getTook())
                .endObject();
        return  builder.string();
    }

}
