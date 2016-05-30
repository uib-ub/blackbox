package no.uib.marcus.common.util;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;

import java.io.IOException;

/**
 * @author Hemed Ali
 */
public final class QueryUtils {

    private QueryUtils(){}

    /**
     * Build a simple query string
     *
     * @param queryString a query string
     * @return a builder for simple query string
     */
    public static SimpleQueryStringBuilder buildSimpleQueryString(String queryString) {
        SimpleQueryStringBuilder builder = QueryBuilders.simpleQueryStringQuery(queryString)
                .analyzer("default")//The custom "default" analyzer is defined in the "_settings".
                .field("identifier")//Not analyzed field
                .field("label", 3)//Not analyzed field.
                .field("_all")
                .defaultOperator(SimpleQueryStringBuilder.Operator.AND);
        return builder;
    }

    /**
     * Build a query string
     *
     * @param queryString a query string
     * @return a builder for query string
     */
    public static QueryStringQueryBuilder buildQueryString(String queryString) {
        QueryStringQueryBuilder builder = QueryBuilders.queryStringQuery(queryString)
                .analyzer("default")//The custom "default" analyzer is defined in the "_settings".
                .field("identifier")//Not analyzed field
                .field("label", 3)//Not analyzed field.
                .field("_all")
                .defaultOperator(QueryStringQueryBuilder.Operator.AND);
        return builder;
    }
    /**
     * Convert a search response to a JSON string.
     *
     * @param response a search response
     * @param isPretty a boolean value to show whether the JSON string should be pretty printed.
     * @return search hits as a JSON string
     **/
    public static String toJsonString(SearchResponse response, boolean isPretty) {
        try {
            if(response == null){
                return "{ \"error\" : \"" + "Could not execute search. Your query is malformed" + "\"}";
            }
            XContentBuilder builder = XContentFactory.jsonBuilder();
            if (isPretty) {
                builder.prettyPrint();
            }
            builder.startObject();
            response.toXContent(builder, ToXContent.EMPTY_PARAMS);
            builder.endObject();
            return builder.string();
        } catch (IOException e) {
            return "{ \"error\" : \"" + e.getMessage() + "\"}";
        }
    }
}
