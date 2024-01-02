package no.uib.marcus.common.util;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.util.WithJsonObjectBuilderBase;
import no.uib.marcus.search.IllegalParameterException;
import java.util.logging.Logger;


import java.util.HashMap;
import java.util.Map;

/**
 * @author Hemed Ali
 * University of Bergen Library
 */
public final class SortUtils {
    private static final Logger logger = Logger.getLogger(SortUtils.class.getName());
    private static final char FIELD_SORT_TYPE_SEPARATOR = ':';
    private static final String SORT_FIELD = "sort_field";
    private static final String SORT_ORDER = "sort_order";

    //Enforce non-instantiability
    private SortUtils() {
    }


    /**
     * A wrapper method for building score or field sort options
     *
     * @param sortString a sort string
     * @return either a score sort, field sort or null if the sort string is empty
     */
    public static WithJsonObjectBuilderBase<? extends WithJsonObjectBuilderBase<?>> getSort(String sortString) {
        if(!sortString.isEmpty()) {
            if (sortString.equals("_score")) {
                return SortOptionsBuilders.score();
            } else {
                return new FieldSort.Builder().field(sortString);
            }
        }
        return null;
    }

    /**
     * Extract
     *
     * @param sortString a string that contains a field and sort type in
     *                   the form of "field:asc" or "field:desc"
     * @return a map with keys sort_field and sort_order
     */
    /**
     * Build a field sort
     *
     * @param sortString
     * @return a field sort
     *
     * https://github.com/weenhall/springboot-instance/blob/f6ca05aa4ff5ae237126fe3027149bf3ac92b80f/springboot-es/src/main/java/com/ween/service/SongCiService.java#L38
     * 		SortOptions sortOptions= new SortOptions.Builder().field(SortOptionsBuilders.field().field("author").order(SortOrder.Asc).build()).build();
     *
     * 		Highlight highlight=new Highlight.Builder().fields(field, new HighlightField.Builder().preTags("<font color='red'>").postTags("</font>").build()).build();
     */


    /**
     * Building a score sort with descending order by default.
     **/
    public static  ScoreSort.Builder getScoreSort() {
        ScoreSort.Builder scoreBuilder =  new ScoreSort.Builder();
        return scoreBuilder.order(SortOrder.Desc);
    }
}
