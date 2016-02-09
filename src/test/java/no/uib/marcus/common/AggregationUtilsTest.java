
package no.uib.marcus.common;

import junit.framework.TestCase;
import org.junit.Test;

/**
 * @author Hemed Ali
 */
public class AggregationUtilsTest extends TestCase{ 

        @Test
        public void testContains() throws Exception {
                String aggs = "["
                        + "{\"field\": \"assigned_to\", \"order\": \"term_asc\"},"
                        + "{\"field\": \"subject.exact\", \"size\": 10}," + "                              "
                        + "{\"field\": \"customer_name\", \"size\": 10, \"operator\": \"OR\", \"order\": \"count_desc\"}"
                        + "]";

                assertTrue(AggregationUtils.contains(aggs, "assigned_to", "order", "term_asc"));
                assertFalse(AggregationUtils.contains(aggs, "assigned_to", "operator", "blabla"));
                assertFalse(AggregationUtils.contains(aggs, "hemed", "order", "term_asc"));
                assertTrue(AggregationUtils.contains(aggs, "subject.exact", "size", "10"));
                assertFalse(AggregationUtils.contains(aggs, "subject.exact", "size", "infinity"));

        }
}
