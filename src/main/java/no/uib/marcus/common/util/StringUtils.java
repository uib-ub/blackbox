package no.uib.marcus.common.util;
public final class StringUtils {

    /**
     * Implement hasText from elasticsearch helper, using same logic, but less code.
     *
     * Since we only use Strings in blackbox, only hasText(String str) is implemented
     * Comment below contains expected values listed by elasticsearch comments found at
     * url https://github.com/elastic/elasticsearch/blob/1.7/src/main/java/org/elasticsearch/common/Strings.java#L142
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
        return str != null && !str.isEmpty() && !str.isBlank();
    }
}

