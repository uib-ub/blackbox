package no.uib.marcus.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class QueryUtilsTest {


    @Test
    public void addLeadingWildcard() {
        assertEquals("", QueryUtils.addLeadingWildcard(""));
        assertEquals(null, QueryUtils.addLeadingWildcard(null));
        assertEquals("ali*", QueryUtils.addLeadingWildcard("ali"));
        assertEquals("ali AND juma", QueryUtils.addLeadingWildcard("ali AND juma"));
        assertEquals("ali -juma", QueryUtils.addLeadingWildcard("ali -juma"));
        assertEquals("makame*", QueryUtils.addLeadingWildcard("makame"));
        assertEquals("makame**", QueryUtils.addLeadingWildcard("makame**"));
        assertEquals("makame*", QueryUtils.addLeadingWildcard("makame*"));
        assertEquals("makame *", QueryUtils.addLeadingWildcard("makame *"));
        assertEquals("*makame*", QueryUtils.addLeadingWildcard("*makame*"));
        assertEquals("*makame^", QueryUtils.addLeadingWildcard("*makame^"));
        assertEquals("\"ali\"", QueryUtils.addLeadingWildcard("\"ali\""));
        assertEquals("\"ali*", QueryUtils.addLeadingWildcard("\"ali*"));
        assertEquals("0234", QueryUtils.addLeadingWildcard("0234"));
        assertEquals("l0234l*", QueryUtils.addLeadingWildcard("l0234l"));
    }

    @Test
    public void containsReservedChar() {
        assertFalse(QueryUtils.containsReservedChars(null));
        assertFalse(QueryUtils.containsReservedChars(" "));
        assertFalse(QueryUtils.containsReservedChars("mama"));
        assertTrue(QueryUtils.containsReservedChars("s!"));
        assertTrue(QueryUtils.containsReservedChars("ali\""));
        assertFalse(QueryUtils.containsReservedChars("ubb-ms-02"));    //We permit minus ("-")
    }
}