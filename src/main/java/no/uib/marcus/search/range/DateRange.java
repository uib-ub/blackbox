package no.uib.marcus.search.range;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.joda.time.LocalDate;

import javax.validation.constraints.NotNull;
import java.util.Objects;


public class DateRange implements Range<LocalDate> {

    //Default date format, any one of these are OK
    public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd||yyyy-MM||yyyy";

    //Null indicates unbounded/infinite value
    @Nullable
    private LocalDate fromDate;

    //Null indicates unbounded/infinite value
    @Nullable
    private LocalDate toDate;

    @NotNull
    private String dateFormat;


    public DateRange(String format) {
        this.dateFormat = Objects.requireNonNull(format, "Date format cannot be null");
    }

    public DateRange(LocalDate from, LocalDate to, String format) {
        this.dateFormat = Objects.requireNonNull(format, "Date format cannot be null");
        ;
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


    public String getDateFormat() {
        if (Objects.nonNull(dateFormat)) {
            return dateFormat;
        }
        return DEFAULT_DATE_FORMAT;
    }

    /**
     * Apply a date format
     *
     * @param dateFormat a non-null date format string
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = Objects.requireNonNull(dateFormat, "Date format cannot be null");
    }

    /**
     * Parse a string to a local date or null if that string is empty.
     * {@code Null} indicates unbounded/infinite value
     *
     * @param input date string to pass
     * @return a LocalDate
     */
    @Override
    public LocalDate parse(String input) {
        if (!Strings.hasText(input)) {
            return null;
        }
        return Joda.forPattern(getDateFormat()).parser().parseLocalDate(input);
    }

    /**
     * Check for positive range, a positive range is such that fromDate <= toDate
     * all of them are non-null
     */
    public boolean hasPositiveRange() {
        if (Objects.nonNull(fromDate) && Objects.nonNull(toDate)) {
            if (fromDate.isBefore(toDate) || fromDate.isEqual(toDate)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Check for negative range, a negative range is such that fromDate > toDate
     * all of them are non-null
     */
    public boolean hasNegativeRange() {
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



    //For debugging purpose
    public static void main(String[] args) {
        DateRange dr = new DateRange("yyyy-MM");

        //dr.setFrom(dr.parse("2013-12-30"));
        dr.setTo(dr.parse("2013-12"));

        System.out.println(dr);
    }

}
