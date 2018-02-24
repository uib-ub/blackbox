package no.uib.marcus.common.util;

import org.junit.Test;

import static no.uib.marcus.common.util.SignatureUtils.appendWildcard;
import static org.junit.Assert.*;

public class SignatureUtilsTest {

    @Test
    public void testAppendWildcard() {
        assertEquals("", appendWildcard(""));
        assertEquals(null, appendWildcard(null));
        assertEquals("ubb", appendWildcard("ubb"));
        assertEquals("svardal", appendWildcard("svardal"));
        assertEquals("ubb-*", appendWildcard("ubb-"));
        assertEquals("-kk", appendWildcard("-kk"));
        assertEquals("ali", appendWildcard("ali"));
        assertEquals("*bros-0123-*", appendWildcard("bros-0123-"));
        assertEquals("ali AND juma", appendWildcard("ali AND juma"));
        assertEquals("ali -juma", appendWildcard("ali -juma"));
        assertEquals("\"ali\"", appendWildcard("\"ali\""));
        assertEquals("0234", appendWildcard("0234"));
        assertEquals("l0234l", appendWildcard("l0234l"));
        assertEquals("ubb-ms*", appendWildcard("ubb-ms"));
        assertEquals("*bros-0123-*", appendWildcard("bros-0123-"));
        //It is not a single word
        assertEquals("ubb bros-0123", appendWildcard("ubb bros-0123"));

    }

    @Test
    public void isValidSignature() {
        assertFalse(SignatureUtils.isValidSignature(null));
        assertFalse(SignatureUtils.isValidSignature(""));
        assertFalse(SignatureUtils.isValidSignature("ubb"));
        assertTrue(SignatureUtils.isValidSignature("ubb-ms-01"));
    }


}