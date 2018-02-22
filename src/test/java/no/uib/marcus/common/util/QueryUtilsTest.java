package no.uib.marcus.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class QueryUtilsTest {


    @Test
    public void addLeadingWildcard() {
        assertEquals("", QueryUtils.addLeadingWildcardIfNoWhitespace(""));
        assertEquals(null, QueryUtils.addLeadingWildcardIfNoWhitespace(null));
        assertEquals("ali*", QueryUtils.addLeadingWildcardIfNoWhitespace("ali"));
        assertEquals("ali AND juma", QueryUtils.addLeadingWildcardIfNoWhitespace("ali AND juma"));
        assertEquals("ali -juma", QueryUtils.addLeadingWildcardIfNoWhitespace("ali -juma"));
        assertEquals("makame*", QueryUtils.addLeadingWildcardIfNoWhitespace("makame"));
        assertEquals("makame**", QueryUtils.addLeadingWildcardIfNoWhitespace("makame**"));
        assertEquals("makame*", QueryUtils.addLeadingWildcardIfNoWhitespace("makame*"));
        assertEquals("makame *", QueryUtils.addLeadingWildcardIfNoWhitespace("makame *"));
        assertEquals("*makame*", QueryUtils.addLeadingWildcardIfNoWhitespace("*makame*"));
        assertEquals("*makame", QueryUtils.addLeadingWildcardIfNoWhitespace("*makame"));
        assertEquals("\"ali\"", QueryUtils.addLeadingWildcardIfNoWhitespace("\"ali\""));
        assertEquals("\"ali*", QueryUtils.addLeadingWildcardIfNoWhitespace("\"ali*"));
    }
}