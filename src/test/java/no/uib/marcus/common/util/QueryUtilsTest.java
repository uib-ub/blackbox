package no.uib.marcus.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class QueryUtilsTest {


    @Test
    public void addLeadingWildcard() {
        assertEquals("", QueryUtils.appendTrailingWildcardIfSingleTerm(""));
        assertEquals(null, QueryUtils.appendTrailingWildcardIfSingleTerm(null));
        assertEquals("-kk", QueryUtils.appendTrailingWildcardIfSingleTerm("-kk"));
        assertEquals("ali*", QueryUtils.appendTrailingWildcardIfSingleTerm("ali"));
        assertEquals("ali AND juma", QueryUtils.appendTrailingWildcardIfSingleTerm("ali AND juma"));
        assertEquals("ali -juma", QueryUtils.appendTrailingWildcardIfSingleTerm("ali -juma"));
        assertEquals("makame*", QueryUtils.appendTrailingWildcardIfSingleTerm("makame"));
        assertEquals("makame**", QueryUtils.appendTrailingWildcardIfSingleTerm("makame**"));
        assertEquals("makame*", QueryUtils.appendTrailingWildcardIfSingleTerm("makame*"));
        assertEquals("makame *", QueryUtils.appendTrailingWildcardIfSingleTerm("makame *"));
        assertEquals("*makame*", QueryUtils.appendTrailingWildcardIfSingleTerm("*makame*"));
        assertEquals("*makame^", QueryUtils.appendTrailingWildcardIfSingleTerm("*makame^"));
        assertEquals("\"ali\"", QueryUtils.appendTrailingWildcardIfSingleTerm("\"ali\""));
        assertEquals("\"ali*", QueryUtils.appendTrailingWildcardIfSingleTerm("\"ali*"));
        assertEquals("0234", QueryUtils.appendTrailingWildcardIfSingleTerm("0234"));
        assertEquals("l0234l*", QueryUtils.appendTrailingWildcardIfSingleTerm("l0234l"));
        assertEquals("ubb-ms*", QueryUtils.appendTrailingWildcardIfSingleTerm("ubb-ms"));
        assertEquals("bros-0123-*", QueryUtils.appendTrailingWildcardIfSingleTerm("bros-0123-"));
        //It is not a single word
        assertEquals("ubb bros-0123", QueryUtils.appendTrailingWildcardIfSingleTerm("ubb bros-0123"));

    }


    @Test
    public void containsReservedChar() {
        assertFalse(QueryUtils.containsReservedChars(null));
        assertFalse(QueryUtils.containsReservedChars(" "));
        assertFalse(QueryUtils.containsReservedChars("mama"));
        assertTrue(QueryUtils.containsReservedChars("s!"));
        assertTrue(QueryUtils.containsReservedChars("ali\""));
        assertFalse(QueryUtils.containsReservedChars("ubb-ms-02")); //"-" is OK due
        assertFalse(QueryUtils.containsReservedChars("ms-02"));
    }


    @Test
    public void containsChar() {
        //assertEquals(true, QueryUtils.containsChar("ubb-ms-01", '-'));
        //assertEquals(true, QueryUtils.containsChar("u-", '-'));
    }


    @Test
    public void escapeStructuralNullAndEmpty() {
        assertEquals(null, QueryUtils.escapeStructural(null));
        assertEquals("", QueryUtils.escapeStructural(""));
        assertEquals("  ", QueryUtils.escapeStructural("  "));
    }

    @Test
    public void escapeStructuralPlainTextUnchanged() {
        assertEquals("philosophical grammar", QueryUtils.escapeStructural("philosophical grammar"));
        //',' and '.' are not structural, the analyzer drops them
        assertEquals("p.g, e.d 1969 rhees", QueryUtils.escapeStructural("p.g, e.d 1969 rhees"));
    }

    @Test
    public void escapeStructuralParens() {
        //Balanced parens (no quotes) are still escaped: parens are literal in titles
        assertEquals("philosophical grammar \\(p.g, e.d 1969 rhees\\)",
                QueryUtils.escapeStructural("philosophical grammar (p.g, e.d 1969 rhees)"));
        //Unbalanced parens must not reach the parser un-escaped (would throw)
        assertEquals("philosophical grammar \\(p.g",
                QueryUtils.escapeStructural("philosophical grammar (p.g"));
    }

    @Test
    public void escapeStructuralColonAndSlash() {
        //':' must not be read as a field selector
        assertEquals("bemerkung\\: zettel", QueryUtils.escapeStructural("bemerkung: zettel"));
        //'/' must not start a regex
        assertEquals("ms 1\\/2", QueryUtils.escapeStructural("ms 1/2"));
    }

    @Test
    public void escapeStructuralKeepsWildcardsAndOperators() {
        //Injected signature wildcards and term operators pass through untouched
        assertEquals("ms-132* modell", QueryUtils.escapeStructural("ms-132* modell"));
        assertEquals("*some-ref*", QueryUtils.escapeStructural("*some-ref*"));
        assertEquals("foo~ bar^2 +baz -qux", QueryUtils.escapeStructural("foo~ bar^2 +baz -qux"));
    }

    @Test
    public void escapeStructuralBalancedQuotesOptInToLucene() {
        //A balanced quoted phrase is a deliberate query: pass through untouched,
        //including parens, so grouping/phrase syntax still works
        assertEquals("\"philosophical grammar\"",
                QueryUtils.escapeStructural("\"philosophical grammar\""));
        assertEquals("\"philosophical grammar\" (rhees)",
                QueryUtils.escapeStructural("\"philosophical grammar\" (rhees)"));
    }

    @Test
    public void escapeStructuralStrayQuoteFallsBackToLiteral() {
        //An odd (unbalanced) quote is not a deliberate phrase: escape it so it
        //cannot leave an open phrase that throws
        assertEquals("philosophical \\\"grammar",
                QueryUtils.escapeStructural("philosophical \"grammar"));
    }

    @Test
    public void hasBalancedQuotes() {
        assertFalse(QueryUtils.hasBalancedQuotes(null));
        assertFalse(QueryUtils.hasBalancedQuotes(""));
        assertFalse(QueryUtils.hasBalancedQuotes("no quotes"));
        assertFalse(QueryUtils.hasBalancedQuotes("one \" quote"));
        assertTrue(QueryUtils.hasBalancedQuotes("\"a phrase\""));
        assertTrue(QueryUtils.hasBalancedQuotes("a \"b\" c \"d\""));
        assertFalse(QueryUtils.hasBalancedQuotes("a \"b\" c \"d"));
    }

}