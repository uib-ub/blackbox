package no.uib.marcus.common;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static no.uib.marcus.common.AggregationUtils.contains;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author Hemed Ali
 */
public class AggregationUtilsTest extends RandomizedTest {
        private static final String AGGS = "["
                        + "{\"field\": \"assigned_to\", \"order\": \"term_asc\"},"
                        + "{\"field\": \"subject.exact\", \"size\": 10}," + "                              "
                        + "{\"field\": \"customer_name\", \"size\": 21, \"operator\": \"OR\", \"order\": \"count_desc\"}"
                        + "]";
          
        /**
         * Test using fixed values
         */
        @Test
        public void testContains01() throws Exception {
                assertTrue(AggregationUtils.contains(AGGS, "assigned_to", "order", "term_asc"));
                assertFalse(AggregationUtils.contains(AGGS, "assigned_to", "operator", "blabla"));
                assertFalse(AggregationUtils.contains(AGGS, "hemed", "order", "term_asc"));
                assertTrue(AggregationUtils.contains(AGGS, "subject.exact", "size", "10"));
                assertFalse(AggregationUtils.contains(AGGS, "subject.exact", "size", "infinity"));
                assertFalse(AggregationUtils.contains("Test facets", "subject.exact", "size", "10"));

        }

        /**
         * Test for random size values
         */
        @Test
        @Repeat(iterations = 10, useConstantSeed = false)
        public void testContains02() {
                assertFalse(AggregationUtils.contains(AGGS, "customer_name", "size", randomIntBetween(0, 19) + ""));
        }

        /**
         * Run seed #79A48AE18A1844A8, this seed failed the test before
         */
        @Repeat(iterations = 10, useConstantSeed = true)
        @Seed("79A48AE18A1844A8")
        @Test
        public void testContains03() {
                assertTrue(contains(AGGS, "customer_name", "size", randomIntBetween(20, 21) + ""));
        }
        
        
        @Test
        public void testGetFilterMap01(){
                String [] selectedFilter = {"hemed.ali", "status.sent", "status.draft"};
                
                 Map<String, List<String>> expectedMap = new HashMap<>();
                 expectedMap.put("hemed", Arrays.asList("ali"));
                 expectedMap.put("status", Arrays.asList("sent", "draft"));
                 
                assertEquals(AggregationUtils.getFilterMap(selectedFilter), expectedMap);
                
        
        }
        /**Test if the method returns NULL for wrong inputs**/
        @Test
        public void testGetFilterMap02() throws  Exception{
                //Separate keys with "_", it should return null.
                 String [] selectedFilter = {"hemed", "status_sent_draft", "status_draft"};
                 
                 assertEquals(Collections.emptyMap(), AggregationUtils.getFilterMap(selectedFilter));
                 assertEquals(Collections.emptyMap(), AggregationUtils.getFilterMap(new String[0]));
                 assertEquals(Collections.emptyMap(), AggregationUtils.getFilterMap(null));
        }
        
        @Test
        public void testGetFilterMap03() throws  Exception{
                //Separate keys with "_", it should return null.
                 String [] selectedFilter = {"http://marcus.uib.no.photography", "status_sent", "status_draft"};
                 Map<String, List<String>> expectedMap = new HashMap<>();
                 expectedMap.put("http://marcus.uib.no", Arrays.asList("photography"));
                 assertEquals(AggregationUtils.getFilterMap(selectedFilter), expectedMap);
        }
                 
        @Test
        public void testGetFilterMap05(){
                 String [] input = {"status.sent", "status.draft", "status.makame", "status.bee"};
                 
                 Map<String, List<String>> expectedMap = new HashMap<>();
                 expectedMap.put("status", Arrays.asList("sent", "draft", "makame", "bee"));
                 
                 assertEquals(AggregationUtils.getFilterMap(input), expectedMap);
        
        }
}
