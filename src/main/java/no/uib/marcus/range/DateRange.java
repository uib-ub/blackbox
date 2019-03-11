package no.uib.marcus.range;

import org.apache.log4j.Logger;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.joda.Joda;
import org.elasticsearch.common.joda.time.LocalDate;

import javax.validation.constraints.NotNull;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
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
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd||yyyy-MM||yyyy";
    private static final Logger logger = Logger.getLogger(DateRange.class);

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
        this.fromDate = parseFromDate(from);
        this.toDate = parseToDate(to);
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
     * When only year is specified as "to_date", Joda Time parser assumes that it is 1st of January. This makes
     * sense for "from_date" but not for "to_date". Therefore, this method tries to modify day and month of "to_date"
     * to 31st of December.
     */
    public LocalDate parseToDate(String toDateString) {
        LocalDate toDate = parse(toDateString);
        if (toDate != null && isXSDgYear(toDateString)) {// when only year is specified
            return new LocalDate(toDate.getYear(), 12, 31);
        }
        return toDate;
    }

    /**
     * Joda Time parses from_date correctly
     */
    public LocalDate parseFromDate(String fromDateString) {
        return parse(fromDateString);
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
            logger.warn("gYear must be a number: " + nfe.getLocalizedMessage());
            return false;
        } catch (DatatypeConfigurationException | IllegalArgumentException ex) {
            logger.warn(ex.getLocalizedMessage());
            return false;
        }
        return true;
    }

}
