package no.uib.marcus.range;

import no.uib.marcus.common.util.StringUtils;

import jakarta.annotation.Nullable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Objects;

/**
 * DateRange class that manipulates ranges for Local Dates.
 *
 * @author Hemed Al Ruwehy
 * 20-08-2017
 * University of Bergen Library
 */

public class DateRange implements Range<LocalDate> {
    //Default date format, any one of these is OK
    // default values set for from date
    public static final  DateTimeFormatter DEFAULT_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd]")
            .appendPattern("[yyyy-MM]")
            .appendPattern("[yyyy]")
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter();
    public static final  DateTimeFormatter DEFAULT_TO_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd]")
            .appendPattern("[yyyy-MM]")
            .appendPattern("[yyyy]")
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 12)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 31)
            .toFormatter();

    //Null indicates an unbounded/infinite value
    @Nullable
    private LocalDate fromDate;

    //Null indicates an unbounded/infinite value
    @Nullable
    private LocalDate toDate;

    public DateRange(String from, String to) {
        this.fromDate = parseFromDate(from);
        this.toDate = parseToDate(to);

    }



    @Override
    public LocalDate getFrom() {
        return fromDate;
    }

    @Override
    public void setFrom(LocalDate from) {
        this.fromDate = from;
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
     * Logic for getting last date
     */
    public LocalDate parseToDate(String toDateString) {
        if (!StringUtils.hasText(toDateString))
            return null;
        return   LocalDate.parse(toDateString , DEFAULT_TO_DATE_FORMATTER);
    }


    /**
     * From date is handled by the default formatter
     */
    public LocalDate parseFromDate(String fromDateString) {
        if (!StringUtils.hasText(fromDateString))
            return null;
        return LocalDate.parse(fromDateString , DEFAULT_DATE_FORMATTER);
    }

    /**
     * Parse a string to a local date
     *
     * @param input date string to parse
     * @return a LocalDate or null if the input string is empty. {@code Null} indicates unbounded value
     */
    @Override
    public LocalDate parse(String input) {
        if (StringUtils.hasText(input)) {
            return LocalDate.parse(input, DEFAULT_DATE_FORMATTER);
        }
        return null;
    }

  public static DateRange of(@Nullable String from, @Nullable String to) {
            return new DateRange(from, to);
    }



  /**
     * Check whether a range is positive, a positive range is the one such that
     * fromDate <= toDate for non-null boundaries.
     */
    public boolean isPositive() {
        if (Objects.nonNull(fromDate) && Objects.nonNull(toDate)) {
            return fromDate.isBefore(toDate) || fromDate.isEqual(toDate);
        }
        return false;
    }


    /**
     * Check for a negative range. A negative range is such that fromDate > toDate for non-null boundaries.
     */
    public boolean isNegative() {
        if (Objects.nonNull(fromDate) && Objects.nonNull(toDate)) {
            return fromDate.isAfter(toDate);
        }
        return false;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DateRange dateRange = (DateRange) o;

        if (!Objects.equals(fromDate, dateRange.fromDate)) return false;
        return Objects.equals(toDate, dateRange.toDate);
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
        sb.append('}');
        return sb.toString();
    }


}
