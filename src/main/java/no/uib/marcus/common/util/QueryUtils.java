package no.uib.marcus.common.util;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.SimpleQueryStringQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.json.jackson.JacksonJsonpGenerator;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.json.stream.JsonGenerator;

import java.io.IOException;
import java.io.StringWriter;

import java.util.List;

import static no.uib.marcus.common.util.BlackboxUtils.isNullOrEmpty;

/**
 * @author Hemed Ali
 */
public final class QueryUtils {

    private static final char WILDCARD = '*';

    //Elasticsearch reserved characters (without the minus sign)
    private static final char[] RESERVED_CHARS = {
            '*', '"', '\\', '/', '=', '&', '|', '>', '<', '(', ')',
            '{', '}', '^', '~', '?', ':', '!', '[', ']'
    };

  private static final JacksonJsonpMapper JSONP_MAPPER = new JacksonJsonpMapper();

    private QueryUtils() {
    }

    /**
     * Build a simple query string
     *
     * @param queryString a query string
     * @return a builder for the simple query string
     */
    public static SimpleQueryStringQuery.Builder buildMarcusSimpleQueryString(String queryString) {
        return new SimpleQueryStringQuery.Builder().query
                (queryString)
                .fields(List.of("identifier","label","all","all.exact"))
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
                .fields(List.of("identifier" //Not analyzed field
                        , "label",
                    "all",
                    "all.exact"//Not analyzed field.
                        ))
                .defaultOperator(Operator.And);
    }


    /**
     * Adds a trailing wildcard to a single-term query if it does not contain reserved characters
     * @param queryString a string to add such as wildcard
     * @return the given string with a wildcard appended to the end
     */
    public static String appendTrailingWildcardIfSingleTerm(String queryString) {
        if (!isNullOrEmpty(queryString)
                && Character.isLetter(queryString.charAt(0))
                && !StringUtils.containsWhitespace(queryString)
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
    public static String toJsonString(final SearchResponse<ObjectNode> response, final boolean isPretty)
        throws IOException {
      if (response == null) {
        return "{ \"error\" : \"" + "Could not execute search. See internal server logs"
            + "\"}";
      }

      JsonFactory jsonFactory = new JsonFactory();
      try (StringWriter writer = new StringWriter();
          com.fasterxml.jackson.core.JsonGenerator jacksonGenerator = jsonFactory.createGenerator(
              writer)) {

        if (isPretty) {
          jacksonGenerator.useDefaultPrettyPrinter();
        }
        try (JsonGenerator generator = new JacksonJsonpGenerator(jacksonGenerator)) {
          response.serialize(generator, JSONP_MAPPER);
          return writer.toString();
        }
      }
    }
}
