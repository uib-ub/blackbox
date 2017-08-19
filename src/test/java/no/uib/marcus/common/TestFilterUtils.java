package no.uib.marcus.common;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import no.uib.marcus.common.util.FilterUtils;
import org.junit.Test;

import java.text.ParseException;

public class TestFilterUtils extends RandomizedTest {

    @Test
    public void testIsValidRange() throws ParseException {
        assertTrue("From < To (valid range)" , FilterUtils.isValidRange("1990-01-01", "2000-01-01"));
        assertTrue("From = To (valid range)" , FilterUtils.isValidRange("2000", "2010-01-01"));
        assertTrue("From == To (valid range)" , FilterUtils.isValidRange("2010", "2010-01-01"));
        assertFalse("From > To (not valid range)" , FilterUtils.isValidRange("2010-01-01", "2001-01-01"));
        assertTrue("From > To but different format (valid range)" , FilterUtils.isValidRange("2010-01", "2017-01-01"));
        assertFalse("From is null (not valid range)" , FilterUtils.isValidRange(null, "2001-01-01"));
    }
}
