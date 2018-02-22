package no.uib.marcus.common.util;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.SimpleQueryStringBuilder;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Hemed Ali
 */
public final class QueryUtils {

    private QueryUtils(){}

    //Elasticsearch reserved characters (without minus sign)
    private static final char[] reservedChars = {
            '*', '"', '\\', '/', '=', '&', '|', '>', '<', '!', '(', ')',
            '{', '}', '[', ']', '^', '~', '?', ':', '/', '/', '!', '[', ']', '{', '}'
    };

    /**
     * Build a simple query string
     *
     * @param queryString a query string
     * @return a builder for simple query string
     */
    public static SimpleQueryStringBuilder buildSimpleQueryString(String queryString) {
        return QueryBuilders.simpleQueryStringQuery(queryString)
                .analyzer("default")//The custom "default" analyzer is defined in the "_settings".
                .field("identifier")//Not analyzed field
                .field("label", 3)//Not analyzed field.
                .field("_all")
                .defaultOperator(SimpleQueryStringBuilder.Operator.AND);
    }

    /**
     * Build a query string
     *
     * @param queryString a query string
     * @return a builder for query string
     */
    public static QueryStringQueryBuilder buildQueryString(String queryString) {
        return QueryBuilders
                .queryStringQuery(queryString)
                .analyzer("default")//The custom "default" analyzer is defined in the "_settings".
                .field("identifier")//Not analyzed field
                .field("label", 3)//Not analyzed field.
                .field("_all")
                .defaultOperator(QueryStringQueryBuilder.Operator.AND);
    }


    /**
     * Adds a leading wildcard to a single word, if it does not contain reserved characters
     *
     * @param queryString a string to add such wildcard
     * @return the given string with a wildcard appended to the end
     */
    public static String addLeadingWildcard(String queryString) {
        if(queryString != null
                && !queryString.isEmpty()
                && Character.isLetter(queryString.charAt(0))
                && !Strings.containsWhitespace(queryString)
                && !containsReservedChars(queryString)){

            return queryString + '*';
        }
        return queryString;
    }

    /**
     * Checks if a given string contains Elasticsearch reserved characters
     *
     * @param s  a given string
     * @return <tt>true</tt> if a given string contains a reserved character, otherwise <tt>false</tt>
     */
    public static boolean containsReservedChars(String s) {
        if(Objects.isNull(s)) {
            return false;
        }
        for (char character : reservedChars) {
            if(s.indexOf(character) > -1) {
                return true;
            }
        }
        return false;
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
