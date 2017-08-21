package no.uib.marcus.search.range;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.joda.time.LocalDate;

import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * DateRange class that manipulates date ranges
 *
 * @author Hemed Al Ruwehy
 * 20-08-2017
 * University of Bergen Library
 */

public class DateRange implements Range<LocalDate> {

    //Default date format, any one of these are OK
    public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd||yyyy-MM||yyyy";

    //Null indicates unbounded/infinite value
    @Nullable
    private LocalDate fromDate;

    //Null indicates unbounded/infinite value
    @Nullable
    private LocalDate toDate;

    //Specified date format
    @NotNull
    private String dateFormat;

    public DateRange(String format) {
        this.dateFormat = Objects.requireNonNull(format, "Date format cannot be null");
    }

    public DateRange(LocalDate from, LocalDate to, String format) {
        this.dateFormat = Objects.requireNonNull(format, "Date format cannot be null");
        this.fromDate = from;
        this.toDate = to;
    }

    public DateRange(@Nullable String from, @Nullable String to) {
        this(from, to, DEFAULT_DATE_FORMAT);
    }

    public DateRange(String from, String to, String format) {
        this.dateFormat = Objects.requireNonNull(format, "Date format cannot be null");
        this.fromDate = parse(from);
        this.toDate =  parse(to);
    }

    @Override
    public void setFrom(LocalDate from) {
        this.fromDate = from;
    }

    @Override
    public LocalDate getFrom() {
        return fromDate;
    }

    @Override
    public LocalDate getTo() {
        return toDate;
    }


    @Override
    public void setTo(LocalDate to) {
        this.toDate = to;

    }

    /**
     * Get a date format or default format if not specified.
     * The default format is "yyyy-MM-dd||yyyy-MM||yyyy"
     */
    public String getDateFormat() {
        if (Objects.nonNull(dateFormat)) {
            return dateFormat;
        }
        return DEFAULT_DATE_FORMAT;
    }

    /**
     * Apply a date format to this range
     *
     * @param dateFormat a non-null date format string
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = Objects.requireNonNull(dateFormat, "Date format cannot be null");
    }

    /**
     * Parse a string to a local date
     *
     * @param input date string to parse
     * @return a LocalDate or null if the input string is empty. {@code Null} indicates unbounded value
     */
    @Override
    public LocalDate parse(String input) {
        if (Strings.hasText(input)) {
            return Joda.forPattern(getDateFormat()).parser().parseLocalDate(input);
        }
        return null;
    }

    /**
     * Check whether a range is positive, a positive range is the one such that
     * fromDate <= toDate for non-null boundaries.
     */
    public boolean isPositive() {
        if (Objects.nonNull(fromDate) && Objects.nonNull(toDate)) {
            if (fromDate.isBefore(toDate) || fromDate.isEqual(toDate)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Check for negative range, a negative range is such that fromDate > toDate for non-null boundaries.
     */
    public boolean isNegative() {
        if (Objects.nonNull(fromDate) && Objects.nonNull(toDate)) {
            if (fromDate.isAfter(toDate)) {
                return true;
            }
        }
        return false;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DateRange dateRange = (DateRange) o;

        if (fromDate != null ? !fromDate.equals(dateRange.fromDate) : dateRange.fromDate != null) return false;
        return toDate != null ? toDate.equals(dateRange.toDate) : dateRange.toDate == null;
    }

    @Override
    public int hashCode() {
        int result = fromDate != null ? fromDate.hashCode() : 0;
        result = 31 * result + (toDate != null ? toDate.hashCode() : 0);
        result = 31 * result + dateFormat.hashCode();
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DateRange{");
        sb.append("fromDate=").append(fromDate);
        sb.append(", toDate=").append(toDate);
        sb.append(", dateFormat='").append(dateFormat).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static void main(String[] args){
        DateRange d = null;

        System.out.println(d.getFrom());
        System.out.println(d.getTo());
    }

}
