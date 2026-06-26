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

    //Structural Lucene characters escaped for free-text in "literal" mode.
    //These are grouping/range/phrase/field/regex delimiters that either throw
    //(unbalanced parens, stray quote) or reroute parsing (':' field selector,
    //'/' regex). Wildcards and term operators (* ? ~ ^ + -) are deliberately
    //NOT here, so they stay usable and injected signature wildcards survive.
    private static final char[] STRUCTURAL_ESCAPE_CHARS = {
            '\\', '"', '(', ')', '[', ']', '{', '}', ':', '/'
    };

  private static final JacksonJsonpMapper JSONP_MAPPER = new JacksonJsonpMapper();
  private static final JsonFactory JSON_FACTORY = new JsonFactory();
  private static final List<String> SEARCH_FIELDS = List.of("identifier", "label", "all", "all.exact","all_keyword");
  private static final List<String> WAB_SEARCH_FIELDS = List.of("label", "publishedIn", "publishedInPart", "all", "all_keyword");

  private QueryUtils() {
    }

    /**
     * Build a simple query string
     *
     * @param queryString a query string
     * @return a builder for the simple query string
     */
    public static SimpleQueryStringQuery.Builder buildMarcusSimpleQueryString(String queryString) {
        return new SimpleQueryStringQuery.Builder()
            .query(queryString)
            .fields(SEARCH_FIELDS)
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
                .fields(SEARCH_FIELDS)
                .defaultOperator(Operator.And);
    }

    /**
     * Build a query string query for WAB
     *
     * @param queryString a query string
     * @return a builder for query string
     */
    public static QueryStringQuery.Builder buildWabQueryString(String queryString) {
        return new QueryStringQuery.Builder()
                .query(queryString)
                .fields(WAB_SEARCH_FIELDS)
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
     * @return {@code true} if a given string contains a reserved character, otherwise {@code false}
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
     * Escapes structural Lucene characters so free-text input is matched
     * literally, unless the query carries a balanced pair of quotes. Balanced
     * quotes are treated as an explicit opt-in to phrase / Lucene syntax: the
     * string is then passed through untouched and the caller trusts the user's
     * query. Without that signal the input is assumed to be a literal value
     * (e.g. a title selected from autocomplete, where parens, colons and
     * slashes are part of the text), so {@link #STRUCTURAL_ESCAPE_CHARS} are
     * backslash-escaped. Wildcards and term operators ({@code * ? ~ ^ + -})
     * always pass through, so signature wildcards added upstream survive.
     *
     * @param s the (already wildcard-injected) query string
     * @return the string with structural characters escaped, or {@code s}
     *         unchanged when it is empty or contains balanced quotes
     */
    public static String escapeStructural(String s) {
        if (isNullOrEmpty(s) || hasBalancedQuotes(s)) {
            return s;
        }
        StringBuilder sb = new StringBuilder(s.length() + 8);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            for (char r : STRUCTURAL_ESCAPE_CHARS) {
                if (c == r) {
                    sb.append('\\');
                    break;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Checks whether a string contains at least one quote and an even number of
     * them, i.e. a balanced phrase the user typed deliberately.
     *
     * @param s a given string
     * @return {@code true} if the quote count is non-zero and even
     */
    static boolean hasBalancedQuotes(String s) {
        if (isNullOrEmpty(s)) {
            return false;
        }
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '"') {
                count++;
            }
        }
        return count > 0 && count % 2 == 0;
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
        // @todo: replace with jackson building and adding to ObjectNode
        return "{ \"error\" : \"" + "Could not execute search. See internal server logs"
            + "\"}";
      }

      try (StringWriter writer = new StringWriter();
          com.fasterxml.jackson.core.JsonGenerator jacksonGenerator = JSON_FACTORY.createGenerator(
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
