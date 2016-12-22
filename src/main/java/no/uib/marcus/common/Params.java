package no.uib.marcus.common;

/**
 * List of request parameters.
 * Servlets use these parameters to process the requests,
 */
public class Params {

    public static final String QUERY_STRING = "q";
    public static final String SELECTED_FILTERS = "filter";
    public static final String SETTING_FILTER = "setting_filter";
    public static final String AGGREGATIONS = "aggs";
    public static final String INDICES = "index";
    public static final String INDEX_TYPES = "type";
    public static final String FROM = "from";
    public static final String SIZE = "size";
    public static final String FROM_DATE = "from_date";
    public static final String TO_DATE = "to_date";
    public static final String SORT = "sort";
    public static final String PRETTY_PRINT = "pretty";
    public static final String AND_BOOL_FILTER = "and_bool_filter";
    public static final String OR_BOOL_FILTER = "or_bool_filter";

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
