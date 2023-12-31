package no.uib.marcus.common.util;

import com.carrotsearch.randomizedtesting.RandomizedTest;
import com.carrotsearch.randomizedtesting.annotations.Repeat;
import com.carrotsearch.randomizedtesting.annotations.Seed;
import no.uib.marcus.range.DateRange;
import java.util.logging.Logger;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;

import java.util.*;

import static no.uib.marcus.common.util.AggregationUtils.contains;

/**
 * @author Hemed Ali
 */
public class AggregationUtilsTest extends RandomizedTest {
    private final Logger logger = Logger.getLogger(AggregationUtilsTest.class.getName());
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
            //Here exception will be thrown, because aggregations are not valid.
            assertFalse(AggregationUtils.contains("Test facets", "subject.exact", "size", "10"));
            //Check for null and and empty string
            assertFalse(AggregationUtils.contains(null, "subject", "size", "10"));
            assertFalse(AggregationUtils.contains("", "subject", "size", "10"));
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
        public void testGetFilterMap01() {
                String[] selectedFilter = {"hemed#ali", "status#sent", "status#draft"};

                Map<String, List<String>> expectedMap = new HashMap<>();
                expectedMap.put("hemed", Arrays.asList("ali"));
                expectedMap.put("status", Arrays.asList("sent", "draft"));

                assertEquals(FilterUtils.buildFilterMap(selectedFilter), expectedMap);
        }

        /**
         * Test if the method returns NULL for wrong inputs
         **/
        @Test
        public void testGetFilterMap02() throws Exception {
                //Separate keys with "_", it should return null.
                String[] selectedFilter = {"hemed", "status_sent_draft", "status_draft"};
                String[] selectedFilter2 = {"type#Fotografi"};

                //Test for empty map
                assertEquals(Collections.emptyMap(), FilterUtils.buildFilterMap(selectedFilter));
                assertEquals(Collections.emptyMap(), FilterUtils.buildFilterMap(new String[0]));
                assertEquals(Collections.emptyMap(), FilterUtils.buildFilterMap(null));
                //Test if a filter map contains a key
                assertTrue(FilterUtils.buildFilterMap(selectedFilter2).containsKey("type"));
        }

        @Test
        public void testGetFilterMap03() throws Exception {
                //Separate keys with "_", it should return null.
                String[] selectedFilter = {"http://marcus.uib.no#photography", "status_sent", "status_draft"};
                Map<String, List<String>> expectedMap = new HashMap<>();
                expectedMap.put("http://marcus.uib.no", Arrays.asList("photography"));
                assertEquals(FilterUtils.buildFilterMap(selectedFilter), expectedMap);

        }

        @Test
        public void testBoolFilterOnDates(){
            BoolQueryBuilder boolFilter = QueryBuilders.boolQuery();
            //Filter will not contain any clause because dates are empty
            assertFalse(FilterUtils.addDateRangeFilter(boolFilter, new DateRange("","")).hasClauses());
            //Filter shall contain must or should clauses
            assertTrue(FilterUtils.addDateRangeFilter(boolFilter, new DateRange("1999", "2000")).hasClauses());
        }


        @Test
        public void testGetFilterMap05() {
                String[] input = {"status#sent", "status#draft", "status#makame", "status#bee"};
                Map<String, List<String>> expectedMap = new HashMap<>();
                expectedMap.put("status", Arrays.asList("sent", "draft", "makame", "bee"));
                assertEquals(FilterUtils.buildFilterMap(input), expectedMap);
        }
}
