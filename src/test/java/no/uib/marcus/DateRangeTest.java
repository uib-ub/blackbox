package no.uib.marcus;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import no.uib.marcus.search.range.DateRange;
import org.junit.Test;

public class DateRangeTest extends RandomizedTest {

    @Test
    public void testHasPositiveValue(){
        DateRange range = new DateRange("2013", "2015");
        assertTrue("Range is positive", range.hasPositiveRange());
        assertTrue("Range is positive", new DateRange("2017", "2017").hasPositiveRange());
        assertEquals("2013-01-01", range.getFrom().toString());

    }


    @Test
    public void testHasNegativeValue(){
        DateRange range = new DateRange("2017-01-01", "2015");
        assertTrue("Range is negative", range.hasNegativeRange());
        assertEquals("2017-01-01", range.getFrom().toString());

    }

}
