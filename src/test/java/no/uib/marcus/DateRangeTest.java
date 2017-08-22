package no.uib.marcus;

import no.uib.marcus.range.DateRange;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DateRangeTest {

    @Test
    public void testHasPositiveValue(){
        DateRange range = new DateRange("2013", "2015");
        assertTrue("Range is positive", range.isPositive());
        assertTrue("Range is positive", new DateRange("2017", "2017").isPositive());
        assertEquals("2013-01-01", range.getFrom().toString());

    }


    @Test
    public void testHasNegativeValue(){
        DateRange range = new DateRange("2017-01-01", "2015");
        assertTrue("Range is negative", range.isNegative());
        assertEquals("2017-01-01", range.getFrom().toString());

    }

    @Test
    public void testIfReturnsNullForEmptyBounds(){
        DateRange range = new DateRange("", " ");
        assertEquals(null, range.getFrom());
        assertEquals(null, range.getTo());

    }

}
