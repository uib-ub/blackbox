package no.uib.marcus.common.util;

import org.junit.Test;

import static no.uib.marcus.common.util.SignatureUtils.appendLeadingWildcardIfWABSignature;
import static no.uib.marcus.common.util.SignatureUtils.appendWildcardIfUBBSignature;
import static org.junit.Assert.*;

public class SignatureUtilsTest {

    @Test
    public void appendWildcardForUBBSignature() {
        assertEquals("", appendWildcardIfUBBSignature(""));
        assertEquals(null, appendWildcardIfUBBSignature(null));
        assertEquals("ubb", appendWildcardIfUBBSignature("ubb"));
        assertEquals("svardal", appendWildcardIfUBBSignature("svardal"));
        assertEquals("ubb-*", appendWildcardIfUBBSignature("ubb-"));
        assertEquals("-kk", appendWildcardIfUBBSignature("-kk"));
        assertEquals("ali", appendWildcardIfUBBSignature("ali"));
        assertEquals("*bros-0123-*", appendWildcardIfUBBSignature("bros-0123-"));
        assertEquals("ali AND juma", appendWildcardIfUBBSignature("ali AND juma"));
        assertEquals("ali -juma", appendWildcardIfUBBSignature("ali -juma"));
        assertEquals("\"ali\"", appendWildcardIfUBBSignature("\"ali\""));
        assertEquals("0234", appendWildcardIfUBBSignature("0234"));
        assertEquals("l0234l", appendWildcardIfUBBSignature("l0234l"));
        assertEquals("ubb-ms*", appendWildcardIfUBBSignature(" ubb-ms"));
        assertEquals("*bros-0123-*", appendWildcardIfUBBSignature("bros-0123-"));
        //It is not a single word
        assertEquals("ubb bros-0123", appendWildcardIfUBBSignature("ubb bros-0123"));

    }

    @Test
    public void isValidUBBSignature() {
        assertFalse(SignatureUtils.isUBBSignature(null));
        assertFalse(SignatureUtils.isUBBSignature(""));
        assertFalse(SignatureUtils.isUBBSignature("ubb"));
        assertTrue(SignatureUtils.isUBBSignature("ubb-ms-01"));
        assertTrue(SignatureUtils.isUBBSignature(" ubb-ms-01")); //with trim
    }


    @Test
    public void isValidWABSignature() {
        assertFalse(SignatureUtils.isWABSignature(null));
        assertFalse(SignatureUtils.isWABSignature(""));
        assertFalse(SignatureUtils.isWABSignature("ubb"));
        assertFalse(SignatureUtils.isWABSignature("ubb-ms-01"));
        assertTrue(SignatureUtils.isWABSignature(" ms-01*")); //with trim
        assertTrue(SignatureUtils.isWABSignature(" ms-01")); //with trim
    }


    @Test
    public void appendLeadingWildcarForAllWABSignatures() {
        assertEquals("", appendLeadingWildcardIfWABSignature(""));
        assertEquals(null, appendLeadingWildcardIfWABSignature(null));
        assertEquals("ubb", appendLeadingWildcardIfWABSignature("ubb"));
        assertEquals("ms-0123-*", appendLeadingWildcardIfWABSignature("ms-0123-"));
        assertEquals("ms-101,11*", appendLeadingWildcardIfWABSignature(" ms-101,11"));
        assertEquals("ts-101,11*", appendLeadingWildcardIfWABSignature(" ts-101,11*"));
        assertEquals("*ts-101", appendLeadingWildcardIfWABSignature("*ts-101"));
        //It is not a single word
        assertEquals("ts bros-0123", appendLeadingWildcardIfWABSignature("ts bros-0123"));
        assertEquals("-ms", appendWildcardIfUBBSignature("-ms"));


    }


}