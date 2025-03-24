package no.uib.marcus.common.util;
/**
 * The StringUtils class provides utility methods for working with strings.
 * The methods are copied from https://github.com/elastic/elasticsearch/blob/1.7/src/main/java/org/elasticsearch/common/Strings.java
 * which are no longer available in the new elasticsearch-java client. The helper still exists in
 * https://github.com/elastic/elasticsearch/blob/main/server/src/main/java/org/elasticsearch/common/Strings.java but we don't want
 * to pull in elasticsearch as a dependence but containsWhitespace is no longer present
 */
public final class StringUtils {

    /**
     * Implement hasText from elasticsearch helper, using same logic, but less code.
     *
     * Since we only use Strings in blackbox, only hasText(String str) is implemented
     * Comment below contains expected values listed by elasticsearch comments found at
     * url
     * #L142
     *
     *
     * Check whether the given  has actual text.
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
     * @param str the String to check (may be <code>null</code>)
     * @return <code>true</code> if the String is not <code>null</code>,
     *         its length is greater than 0, and it does not contain whitespace only
     *
     */
    public static boolean hasText(String str) {
        if (str == null || str.isEmpty() || str.isBlank())
            return false;
        return true;
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
     * @param str the CharSequence to check (may be <code>null</code>)
     * @return <code>true</code> if the CharSequence is not null and has length
     * @see #hasText(String)
     */
    public static boolean hasLength(CharSequence str) {
        return (str != null && str.length() > 0);
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
}

