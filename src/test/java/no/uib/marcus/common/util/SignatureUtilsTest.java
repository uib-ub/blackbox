package no.uib.marcus.common.util;

import org.junit.Test;

import static no.uib.marcus.common.util.SignatureUtils.appendWildcardIfValidSignature;
import static org.junit.Assert.*;

public class SignatureUtilsTest {

    @Test
    public void appendWildcard() {
        assertEquals("", appendWildcardIfValidSignature(""));
        assertEquals(null, appendWildcardIfValidSignature(null));
        assertEquals("ubb", appendWildcardIfValidSignature("ubb"));
        assertEquals("svardal", appendWildcardIfValidSignature("svardal"));
        assertEquals("ubb-*", appendWildcardIfValidSignature("ubb-"));
        assertEquals("-kk", appendWildcardIfValidSignature("-kk"));
        assertEquals("ali", appendWildcardIfValidSignature("ali"));
        assertEquals("*bros-0123-*", appendWildcardIfValidSignature("bros-0123-"));
        assertEquals("ali AND juma", appendWildcardIfValidSignature("ali AND juma"));
        assertEquals("ali -juma", appendWildcardIfValidSignature("ali -juma"));
        assertEquals("\"ali\"", appendWildcardIfValidSignature("\"ali\""));
        assertEquals("0234", appendWildcardIfValidSignature("0234"));
        assertEquals("l0234l", appendWildcardIfValidSignature("l0234l"));
        assertEquals("ubb-ms*", appendWildcardIfValidSignature("ubb-ms"));
        assertEquals("*bros-0123-*", appendWildcardIfValidSignature("bros-0123-"));
        //It is not a single word
        assertEquals("ubb bros-0123", appendWildcardIfValidSignature("ubb bros-0123"));

    }

    @Test
    public void isValidSignature() {
        assertFalse(SignatureUtils.isSignature(null));
        assertFalse(SignatureUtils.isSignature(""));
        assertFalse(SignatureUtils.isSignature("ubb"));
        assertTrue(SignatureUtils.isSignature("ubb-ms-01"));
        assertTrue(SignatureUtils.isSignature(" ubb-ms-01")); //with trim
    }


}