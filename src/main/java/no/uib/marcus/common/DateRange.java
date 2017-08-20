package no.uib.marcus.common;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.joda.time.LocalDate;

import java.util.Objects;


public class DateRange {

    //Default date format, any one of these are OK
    private static String DEFAULT_DATE_FORMAT = "yyyy-mm-dd||yyyy-mm||yyyy";

    //Null indicates unbounded/infinite value
    @Nullable
    private LocalDate fromDate;

    //Null indicates unbounded/infinite value
    @Nullable
    private LocalDate toDate;

    private String dateFormat;


    DateRange(String format) {
        this.dateFormat = format;
    }

    DateRange(LocalDate from, LocalDate to, String format) {
        this.dateFormat = format;
        this.fromDate = from;
        this.toDate = to;
    }

    DateRange(@Nullable String from, @Nullable String to) {
        this(from, to, DEFAULT_DATE_FORMAT);
    }

    DateRange(String from, String to, String format) {
        this.dateFormat = format;
        this.fromDate = Strings.hasText(from) ? parseLocalDate(from) : null;
        this.toDate = Strings.hasText(to) ? parseLocalDate(to) : null;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(String from) {
        if (Strings.hasText(from)) {
            this.fromDate = parseLocalDate(from);
        }
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(String to) {
        if (Strings.hasText(to)) {
            this.toDate = parseLocalDate(to);
        }
    }

    public String getDateFormat() {
        if (Objects.nonNull(dateFormat)) {
            return dateFormat;
        }
        return DEFAULT_DATE_FORMAT;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    /**
     * Parse a string to a local date
     * @param input date string to pass
     *
     * @return a LocalDate
     */
    public LocalDate parseLocalDate(String input) {
        return Joda.forPattern(getDateFormat())
                .parser()
                .parseLocalDate(input);
    }


    /**
     * Check for positive range, a positive range is such that fromDate <= toDate
     * all all of them are non-null
     */
    public boolean hasPositiveRange() {
        if (Objects.nonNull(fromDate) && Objects.nonNull(toDate)) {
            if (fromDate.isBefore(toDate) || fromDate.isEqual(toDate)) {
                return true;
            }
        }
        return false;
    }


    public boolean hasNegativeRange() {
        if (Objects.nonNull(fromDate) && Objects.nonNull(toDate)) {
            if (fromDate.isAfter(toDate)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "DateRange [ fromDate: " + fromDate + " toDate: " + toDate + " dateFormat: " + dateFormat + " ]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DateRange) {
            if (this.fromDate.isEqual(((DateRange) obj).fromDate )
                    && this.toDate.isEqual(((DateRange) obj).toDate)) {
                return true;
            }
        }
        return false;
    }

    //For debugging purpose
    public static void main(String[] args) {
        DateRange dr = new DateRange("yyyy-mm-dd");

        dr.setFromDate("2012-03-01");
        dr.setToDate("2013-03-01");

        System.out.println(dr + " " + dr.hasPositiveRange());
    }

}
