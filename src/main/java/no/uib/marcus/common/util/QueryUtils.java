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

/**
 * @author Hemed Ali
 */
public final class QueryUtils {

    //Special signature character
    private static final char SPECIAL_SIGNATURE_CHAR = '-';
    //List of signature prefixes for University of Bergen Library
    private static final String[] UBB_SIGNATURE_PREFIXES = {"ubb", "ubm"};
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
        return QueryBuilders.queryStringQuery(queryString)
                .analyzer("default")//The custom "default" analyzer is defined in the "_settings".
                .field("identifier")//Not analyzed field
                .field("label", 3)//Not analyzed field.
                .field("_all")
                .defaultOperator(QueryStringQueryBuilder.Operator.AND);
    }


    /**
     * Adds a leading wildcard to a single word query, if it does not contain reserved characters
     *
     * @param queryString a string to add such wildcard
     * @return the given string with a wildcard appended to the end
     */
    public static String addLeadingWildcardIfSingleWord(String queryString) {
        if (!isNullOrEmpty(queryString)
                && Character.isLetter(queryString.charAt(0))
                && !Strings.containsWhitespace(queryString)
                && !containsReservedChars(queryString)) {

            return queryString + WILDCARD;
        }
        return queryString;
    }


    /**
     * Adds a wildcard to a given signature, if it does not contain one from before
     *
     * @param id a signature string to add such wildcard
     * @return the given string with a wildcard appended to the end
     */
    public static String appendWildcardToSignature(String id) {
        if (!isNullOrEmpty(id)
                && Character.isLetter(id.charAt(0))
                && !Strings.containsWhitespace(id)
                && !containsReservedChars(id)) {

            //e.g ubb-ms-001 but not ubb+ms
            if (isUBBSignature(id)) {
                return id + WILDCARD;
            }
            //E.g, "bros-2000" but not "-bros-2000" because it has different meaning
            if (containsChar(id, SPECIAL_SIGNATURE_CHAR)) {
                return WILDCARD + id + WILDCARD;
            }

        }
        return id;
    }


    /**
     * Checks if it is UBB signature
     */
    public static boolean isUBBSignature(String signature) {
        return beginsWithSignaturePrefix(signature) && containsChar(signature, SPECIAL_SIGNATURE_CHAR);
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
     * Checks if a given string does not start with a given character
     */
    public static boolean containsChar(String s, char character) {
        return !isNullOrEmpty(s) && s.indexOf(character) > -1;
    }


    /**
     * Checks if a given query is likely a signature, that means it begins with signature prefix
     */
    private static boolean beginsWithSignaturePrefix(String query) {
        for (String prefix : UBB_SIGNATURE_PREFIXES) {
            if (query.toLowerCase().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Checks if a given string is either null or empty
     *
     * @param s  a string to check
     * @return true if this string is null or empty, otherwise false
     */
    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * Convert a search response to a JSON string.
     *
     * @param response a search response
     * @param isPretty a boolean value to show whether the JSON string should be pretty printed.
     * @return search hits as a JSON string
     **/
    public static String toJsonString(final SearchResponse response, final boolean isPretty) {
        try {
            if (response == null) {
                return "{ \"error\" : \"" + "Could not execute search. "
                        + "Your query is malformed" + "\"}";
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
