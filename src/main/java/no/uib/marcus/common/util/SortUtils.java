package no.uib.marcus.common.util;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.util.ObjectBuilder;
import no.uib.marcus.search.IllegalParameterException;


/**
 * @author Hemed Ali University of Bergen Library
 */
public final class SortUtils {

  private static final char FIELD_SORT_TYPE_SEPARATOR = ':';

  //Enforce non-instantiability
  private SortUtils() {
  }
  
  /**
   * A wrapper method for building score or field sort options*
   *
   * @param sortString a sort string
   * @return either a score sort, field sort or null if the sort string is empty
   */
  public static ObjectBuilder<SortOptions> getSort(String sortString) {
    if (!StringUtils.hasText(sortString)) {
      return null;
    }
    int delimiterIndex = sortString.indexOf(FIELD_SORT_TYPE_SEPARATOR);
    if (delimiterIndex == -1) {
      // Return null for malformed strings (missing separator) to avoid crashes
      return null;
    }
    String sortKey = sortString.substring(0, delimiterIndex);
    String sortOrder = sortString.substring(delimiterIndex + 1);

    if (!StringUtils.hasText(sortKey)) {
      return null;
    }
    if (!"asc".equals(sortOrder) && !"desc".equals(sortOrder)) {
      throw new IllegalParameterException("Sort order must be either 'asc' or 'desc'");
    }
    SortOrder sort = "asc".equals(sortOrder) ? SortOrder.Asc : SortOrder.Desc;
    SortOptions.Builder sortOptions = new SortOptions.Builder();
    if (sortKey.equals("_score")) {
      return sortOptions.score(SortOptionsBuilders.score().build());
    } else {
      // now missing is set to "_last" or "_first" depending on sort order
      // this has the consequence that for some sorts they appear first, would it be better to hide
      // them instead in the middle somewhere?
      // Often not relevant for the ordering, if value is missing
      String missing = sort == SortOrder.Asc ? "_last" : "_first";
      return sortOptions.field(
          new FieldSort.Builder().missing(missing).field(sortKey).order(sort).build());
    }
  }
}
