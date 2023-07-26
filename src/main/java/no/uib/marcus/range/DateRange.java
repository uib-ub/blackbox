package no.uib.marcus.range;

import org.elasticsearch.core.Nullable;
import org.elasticsearch.common.Strings;

import javax.validation.constraints.NotNull;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * DateRange class that manipulates ranges for Local Dates.
 *
 * @author Hemed Al Ruwehy
 * 20-08-2017
 * University of Bergen Library
 */

public class DateRange implements Range<LocalDate> {
    //Default date format, any one of these is OK
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd||yyyy-MM||yyyy";

    // default values set for from date
    public static final  DateTimeFormatter DEFAULT_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("[yyyy-MM-dd]")
            .appendPattern("[yyyy-MM]")
            .appendPattern("[yyyy]")
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter();
    private static final Logger logger = Logger.getLogger(String.valueOf(DateRange.class));

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

    public DateRange(@Nullable String from, @Nullable String to) {
        this(from, to, DEFAULT_DATE_FORMAT);
    }

    public DateRange(String from, String to, String format) {
        this(format);


        this.fromDate = from != null && !from.isEmpty()  ? parseFromDate(from) : null;
        this.toDate = to != null && !to.isEmpty()  ? parseToDate(to) : null;
    }

    public static DateRange of(@Nullable String from, @Nullable String to) {
        return new DateRange(from, to);
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
        if (toDateString.trim().isEmpty())
            return null;

        String defaultDay = "";
        String defaultMonth = toDateString.length() == 4 ? "-12" : "";

        if (!toDateString.matches("^\\d{4}-\\d{2}-\\d{2}$") && toDateString.matches("^\\d{4}-\\d{2}-\\d{2}$")){
            if (toDateString.length() != 7)
                isXSDgYear(toDateString);
            LocalDate nextMonthFirstDay  = LocalDate.parse(toDateString + defaultMonth + "-01").plusMonths(1);
            defaultDay = "-" +Integer.toString(nextMonthFirstDay.minusDays(1).get(ChronoField.DAY_OF_MONTH));
        }

        return   LocalDate.parse(toDateString + defaultMonth + defaultDay ,DEFAULT_DATE_FORMATTER);
    }


    /**
     * From date is handled by default formatter
     */
    public LocalDate parseFromDate(String fromDateString) {
            return LocalDate.parse(fromDateString , DEFAULT_DATE_FORMATTER);
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
            return LocalDate.parse(input, DateTimeFormatter.ofPattern(getDateFormat()));
        }
        return null;
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
     * Check for negative range, a negative range is such that fromDate > toDate for non-null boundaries.
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

    /**
     * Checks if a provided string is valid xsd:gYear
     *
     * @param yearString a gYear string to parse
     * @return {@code empty string} true if it is valid gYear, otherwise false
     */
    public static boolean isXSDgYear(String yearString) {
        try {
            int inputYear = Integer.parseInt(yearString);
            /*
             * Input year should be at most 4 digits number. I had to make sure about this because if input year
             * is greater than 4 digits, it will be truncated by toXMLFormat() method anyway.
             **/
            if (inputYear > 0 && String.valueOf(inputYear).length() > 4) {
                return true;
            }
            //Negative gYear with at most 4 digits is allowed. e.g -0160 for 160BC
            if (inputYear < 0 && String.valueOf(inputYear).length() > 5) {
                return true;
            }

            XMLGregorianCalendar gCalendar = DatatypeFactory
                    .newInstance()
                    .newXMLGregorianCalendarDate(
                            inputYear,
                            DatatypeConstants.FIELD_UNDEFINED,
                            DatatypeConstants.FIELD_UNDEFINED,
                            DatatypeConstants.FIELD_UNDEFINED
                    );
            gCalendar.toXMLFormat();
        } catch (NumberFormatException nfe) {
           // logger.warning("OLG gYear must be a number: " + nfe.getLocalizedMessage());
            return false;
        } catch (DatatypeConfigurationException | IllegalArgumentException ex) {
            logger.warning(ex.getLocalizedMessage());
            return false;
        }
        return true;
    }

}
