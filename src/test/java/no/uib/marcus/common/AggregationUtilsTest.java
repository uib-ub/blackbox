package no.uib.marcus.common;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static no.uib.marcus.common.AggregationUtils.contains;
import org.junit.Test;

/**
 * @author Hemed Ali
 */
public class AggregationUtilsTest extends RandomizedTest {
        private static final String aggs = "["
                        + "{\"field\": \"assigned_to\", \"order\": \"term_asc\"},"
                        + "{\"field\": \"subject.exact\", \"size\": 10}," + "                              "
                        + "{\"field\": \"customer_name\", \"size\": 21, \"operator\": \"OR\", \"order\": \"count_desc\"}"
                        + "]";
          
        /**
         * Test using fixed values
         */
        @Test
        public void testContains01() throws Exception {
                assertTrue(AggregationUtils.contains(aggs, "assigned_to", "order", "term_asc"));
                assertFalse(AggregationUtils.contains(aggs, "assigned_to", "operator", "blabla"));
                assertFalse(AggregationUtils.contains(aggs, "hemed", "order", "term_asc"));
                assertTrue(AggregationUtils.contains(aggs, "subject.exact", "size", "10"));
                assertFalse(AggregationUtils.contains(aggs, "subject.exact", "size", "infinity"));
                assertFalse(AggregationUtils.contains("Wrong facets", "subject.exact", "size", "10"));

        }

        /**
         * Test for random size values
         */
        @Test
        @Repeat(iterations = 10, useConstantSeed = false)
        public void testContains02() {
                assertFalse(AggregationUtils.contains(aggs, "customer_name", "size", randomIntBetween(0, 19) + ""));
        }

        /**
         * Run seed #79A48AE18A1844A8, this seed failed the test before
         */
        @Repeat(iterations = 10, useConstantSeed = true)
        @Seed("79A48AE18A1844A8")
        @Test
        public void testContains03() {
                assertTrue(contains(aggs, "customer_name", "size", randomIntBetween(20, 21) + ""));
        }
        
        
        @Test
        public void testGetFilterMap01(){
                String [] selectedFilter = {"hemed.ali", "status.sent", "status.draft"};
                
                 Map<String, List<String>> expectedMap = new HashMap<>();
                 expectedMap.put("hemed", Arrays.asList("ali"));
                 expectedMap.put("status", Arrays.asList("sent", "draft"));
                 
                assertSame(AggregationUtils.getFilterMap(selectedFilter), expectedMap);
        
        }
        
        @Test
        public void testGetFilterMap02(){
                //Separate keys with "_", it should fail.
                 String [] selectedFilter = {"hemed_ali", "status_sent", "status.draft"};
                
                 Map<String, List<String>> expectedMap = new HashMap<>();
                 expectedMap.put("hemed", Arrays.asList("ali"));
                 expectedMap.put("status", Arrays.asList("sent", "draft"));
                 
                 assertNotSame(AggregationUtils.getFilterMap(selectedFilter), expectedMap);
        }
                 
        @Test
        public void testGetFilterMap03(){
                //Separate keys with "_", it should fail.
                 String [] input = {"status.sent", "status.draft", "status.makame", "status.bee"};
                 //String [] randomArray = {randomFrom(input)};
                
                 Map<String, List<String>> expectedMap = new HashMap<>();
                 expectedMap.put("status", Arrays.asList("sent", "draft", "makame", "bee"));
                 
                 assertSame(AggregationUtils.getFilterMap(input), expectedMap);
        
        }
}
