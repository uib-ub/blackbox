package no.uib.marcus.common;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import no.uib.marcus.range.DateRange;
import org.junit.Test;

import java.text.ParseException;

public class  TestFilterUtils extends RandomizedTest {

    @Test
    public void testIsValidRange() throws ParseException {
        assertTrue("From < To" , new DateRange("1990-01-01", "2000-01-01").isPositive());
        assertTrue("From = To" , new DateRange("2000", "2010-01-01").isPositive());
        assertTrue("From > To" , new DateRange("2010-01-01", "2001-01-01").isNegative());
        assertTrue("From > To but different format" , new DateRange("2010-01", "2017-01-01").isPositive());
        assertFalse("From is null (not positive)" , new DateRange(null, "2001-01-01").isPositive());
    }
}
