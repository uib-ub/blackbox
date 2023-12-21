package no.uib.marcus.common.util;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;


import java.io.IOException;
import java.util.List;

import static no.uib.marcus.common.util.BlackboxUtils.isNullOrEmpty;

/**
 * @author Hemed Ali
 */
public final class QueryUtils {

    private static final char WILDCARD = '*';

    //Elasticsearch reserved characters (without minus sign)
    private static final char[] RESERVED_CHARS = {
            '*', '"', '\\', '/', '=', '&', '|', '>', '<', '!', '(', ')',
            '{', '}', '[', ']', '^', '~', '?', ':', '/', '/', '!', '[', ']', '{', '}'
    };

    private QueryUtils() {
    }

    /**
     * Build a simple query string
     *
     * @param queryString a query string
     * @return a builder for simple query string
     */
    public static SimpleQueryStringQuery.Builder buildMarcusSimpleQueryString(String queryString) {
        SimpleQueryStringQuery.Builder builder = new SimpleQueryStringQuery.Builder();
        return  builder.query(queryString)
                .analyzer("default")//The custom "default" analyzer is defined in the "_settings".
                .fields(List.of("identifier", "label", "_all"))
                .defaultOperator(Operator.And);
    }

    /**
     * Build a query string query
     *
     * @param queryString a query string
     * @return a builder for query string
     */
    public static QueryStringQuery.Builder buildMarcusQueryString(String queryString) {
        QueryStringQuery.Builder builder = new QueryStringQuery.Builder();
        return builder.query(queryString)
                .analyzer("default")//The custom "default" analyzer is defined in the "_settings".
                .fields(List.of("identifier" //Not analyzed field
                ,"label" //Not analyzed field.
                ,"_all"))
                .defaultOperator(Operator.And);
    }


    /**
     * Adds a trailing wildcard to a single term query, if it does not contain reserved characters
     *
     * @param queryString a string to add such wildcard
     * @return the given string with a wildcard appended to the end
     */
    public static String appendTrailingWildcardIfSingleTerm(String queryString) {
        if (!isNullOrEmpty(queryString)
                && Character.isLetter(queryString.charAt(0))
                && !queryString.matches("\\s")
                && !containsReservedChars(queryString)) {

            return queryString + WILDCARD;
        }
        return queryString;
    }



    /**
     * Checks if a given string contains Elasticsearch reserved characters
     *
     * @param s a given string
     * @return <tt>true</tt> if a given string contains a reserved character, otherwise <tt>false</tt>
     */
    public static boolean containsReservedChars(String s) {
        if (isNullOrEmpty(s)) {
            return false;
        }
        for (char character : RESERVED_CHARS) {
            if (s.indexOf(character) > -1) {
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
    public static String toJsonString(final SearchResponse<ObjectNode> response, final boolean isPretty) {

        try {
           if (response == null) {
               return "{ \"error\" : \"" + "Could not execute search. See internal server logs" + "\"}";

           }
            return response.toString();


            //        XContentBuilder builder = XContentFactory.jsonBuilder();
    //        if (isPretty) {
     //           builder.prettyPrint();
     //       }
    //        builder.startObject();
      //      response.toXContent(builder, ToXContent.EMPTY_PARAMS);
        //    builder.endObject();
      //      return builder.toString();
    //     } catch (IOException e) {
      //      return "{ \"error\" : \"" + e.getMessage() + "\"}";
 //       }
    return "@todo serialize result using jsonp";}
}
