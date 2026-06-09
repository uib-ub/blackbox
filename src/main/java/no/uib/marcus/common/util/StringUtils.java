package no.uib.marcus.common.util;
/**
 * The StringUtils class provides utility methods for working with strings.
 * The methods are copied from <a href="https://github.com/elastic/elasticsearch/blob/1.7/src/main/java/org/elasticsearch/common/Strings.java">elasticsearch implementation</a>
 * which are no longer available in the new elasticsearch-java client. The helper still exists in
 * <a href="https://github.com/elastic/elasticsearch/blob/main/server/src/main/java/org/elasticsearch/common/Strings.java">elasticsearch main Strings.java</a>, but we don't want
 * to pull in elasticsearch as a dependence but containsWhitespace is no longer present
 */
public final class StringUtils {

    /**
     * Implement hasText from elasticsearch helper, using the same logic, but less code.
     * Since we only use Strings in Blackbox, only hasText(String str) is implemented
     * The Comment below contains expected values listed by elasticsearch comments found at
     * url
     * #L142
     * Check whether the given String has actual text.
     * More specifically, returns <code>true</code> if the string not <code>null</code>,
     * its length is greater than 0, and it contains at least one non-whitespace character.
     * <p><pre>
     * StringUtils.hasText(null) = false
     * StringUtils.hasText("") = false
     * StringUtils.hasText(" ") = false
     * StringUtils.hasText("12345") = true
     * StringUtils.hasText(" 12345 ") = true
     * </pre>
     *
     * @param str the String to check (maybe <code>null</code>)
     * @return <code>true</code> if the String is not <code>null</code>,
     *         its length is greater than 0, and it does not contain whitespace only
     *
     */
    public static boolean hasText(String str) {
      return str != null && !str.isBlank();
    }

    //---------------------------------------------------------------------
    // General convenience methods for working with Strings
    //---------------------------------------------------------------------

    /**
     * Check that the given CharSequence is neither <code>null</code> nor of length 0.
     * Note: Will return <code>true</code> for a CharSequence that purely consists of whitespace.
     * <p><pre>
     * StringUtils.hasLength(null) = false
     * StringUtils.hasLength("") = false
     * StringUtils.hasLength(" ") = true
     * StringUtils.hasLength("Hello") = true
     * </pre>
     *
     * @param str the CharSequence to check (maybe <code>null</code>)
     * @return <code>true</code> if the CharSequence is not null and has length
     * @see #hasText(String)
     */
    public static boolean hasLength(CharSequence str) {
        return (str != null && !str.isEmpty());
    }

    public static boolean containsWhitespace(CharSequence str) {
        if (!hasLength(str)) {
            return false;
        }
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

  /**
   * Parses a string to an integer with bounds checking and a fallback default value.
   *
   * @param value        the string value to parse
   * @param defaultValue the default value if parsing fails or value is empty
   * @param min          the minimum allowed value (inclusive)
   * @param max          the maximum allowed value (inclusive)
   * @return the parsed integer clamped to [min, max], or defaultValue if parsing fails
   */
  public static int parseIntWithDefault(String value, int defaultValue, int min, int max) {
    if (!hasText(value)) {
      return defaultValue;
    }
    try {
      int parsed = Integer.parseInt(value.trim());
      return Math.max(min, Math.min(max, parsed));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}

