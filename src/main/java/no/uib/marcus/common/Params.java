package no.uib.marcus.common;

/**
 * List of request parameters.
 * Servlets use these parameters to process equests,
 */
public class Params {

    public static final String SERVICE = "service";
    public static final String QUERY_STRING = "q";
    public static final String SELECTED_FILTERS = "filter";
    public static final String AGGREGATIONS = "aggs";
    public static final String INDICES = "index";
    public static final String INDEX_TYPES = "type";
    public static final String FROM = "from";
    public static final String SIZE = "size";
    public static final String FROM_DATE = "from_date";
    public static final String TO_DATE = "to_date";
    public static final String SORT = "sort";
    public static final String PRETTY_PRINT = "pretty";
    public static final String TOP_FILTER = "top_filter";
    public static final String POST_FILTER = "post_filter";
    public static final String INDEX_BOOST = "index_boost";
    public static final int DEFAULT_FROM = 0;
    public static final int DEFAULT_SIZE = 10;

    /**
     * A static inner class for holding date fields to perform date ranges.
    **/
    public static class DateField {
        public static final String AVAILABLE = "available";
        public static final String CREATED = "created";
        public static final String MADE_BEFORE = "madeBefore";
        public static final String MADE_AFTER = "madeAfter";
    }

}
