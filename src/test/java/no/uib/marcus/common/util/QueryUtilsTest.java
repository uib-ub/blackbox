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
        assertEquals("ali AND juma*", QueryUtils.appendTrailingWildcardIfSingleTerm("ali AND juma"));
        assertEquals("ali -juma*", QueryUtils.appendTrailingWildcardIfSingleTerm("ali -juma"));
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
        assertEquals("ubb bros-0123*", QueryUtils.appendTrailingWildcardIfSingleTerm("ubb bros-0123"));

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

}