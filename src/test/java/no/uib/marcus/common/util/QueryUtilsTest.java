package no.uib.marcus.common.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class QueryUtilsTest {


    @Test
    public void addLeadingWildcard() {
        assertEquals("", QueryUtils.addLeadingWildcardIfSingleWord(""));
        assertEquals(null, QueryUtils.addLeadingWildcardIfSingleWord(null));
        assertEquals("-kk", QueryUtils.addLeadingWildcardIfSingleWord("-kk"));
        assertEquals("ali*", QueryUtils.addLeadingWildcardIfSingleWord("ali"));
        assertEquals("ali AND juma", QueryUtils.addLeadingWildcardIfSingleWord("ali AND juma"));
        assertEquals("ali -juma", QueryUtils.addLeadingWildcardIfSingleWord("ali -juma"));
        assertEquals("makame*", QueryUtils.addLeadingWildcardIfSingleWord("makame"));
        assertEquals("makame**", QueryUtils.addLeadingWildcardIfSingleWord("makame**"));
        assertEquals("makame*", QueryUtils.addLeadingWildcardIfSingleWord("makame*"));
        assertEquals("makame *", QueryUtils.addLeadingWildcardIfSingleWord("makame *"));
        assertEquals("*makame*", QueryUtils.addLeadingWildcardIfSingleWord("*makame*"));
        assertEquals("*makame^", QueryUtils.addLeadingWildcardIfSingleWord("*makame^"));
        assertEquals("\"ali\"", QueryUtils.addLeadingWildcardIfSingleWord("\"ali\""));
        assertEquals("\"ali*", QueryUtils.addLeadingWildcardIfSingleWord("\"ali*"));
        assertEquals("0234", QueryUtils.addLeadingWildcardIfSingleWord("0234"));
        assertEquals("l0234l*", QueryUtils.addLeadingWildcardIfSingleWord("l0234l"));
        assertEquals("ubb-ms*", QueryUtils.addLeadingWildcardIfSingleWord("ubb-ms"));
        assertEquals("bros-0123-*", QueryUtils.addLeadingWildcardIfSingleWord("bros-0123-"));
        //It is not a single word
        assertEquals("ubb bros-0123", QueryUtils.addLeadingWildcardIfSingleWord("ubb bros-0123"));

    }


    @Test
    public void appendWildcardToSignature() {
        assertEquals("", QueryUtils.appendWildcardToSignature(""));
        assertEquals(null, QueryUtils.appendWildcardToSignature(null));
        assertEquals("ubb", QueryUtils.appendWildcardToSignature("ubb"));
        assertEquals("svardal", QueryUtils.appendWildcardToSignature("svardal"));
        assertEquals("ubb-*", QueryUtils.appendWildcardToSignature("ubb-"));
        assertEquals("-kk", QueryUtils.appendWildcardToSignature("-kk"));
        assertEquals("ali", QueryUtils.appendWildcardToSignature("ali"));
        assertEquals("*bros-0123-*", QueryUtils.appendWildcardToSignature("bros-0123-"));
        assertEquals("ali AND juma", QueryUtils.appendWildcardToSignature("ali AND juma"));
        assertEquals("ali -juma", QueryUtils.appendWildcardToSignature("ali -juma"));
        assertEquals("\"ali\"", QueryUtils.appendWildcardToSignature("\"ali\""));
        assertEquals("0234", QueryUtils.appendWildcardToSignature("0234"));
        assertEquals("l0234l", QueryUtils.appendWildcardToSignature("l0234l"));
        assertEquals("ubb-ms*", QueryUtils.appendWildcardToSignature("ubb-ms"));
        assertEquals("*bros-0123-*", QueryUtils.appendWildcardToSignature("bros-0123-"));
        //It is not a single word
        assertEquals("ubb bros-0123", QueryUtils.appendWildcardToSignature("ubb bros-0123"));

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
        assertTrue(QueryUtils.containsChar("ubb-ms-01", '-'));
        assertTrue(QueryUtils.containsChar("u-", '-'));
    }


}