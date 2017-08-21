package no.uib.marcus.search.range;


/**
 * Interface for manipulating Ranges
 * @param <T> range a specified Type T
 *
 *           @author Hemed Al Ruwehy
 *           21-08-2017
 *           University of Bergen Library
 */
public interface  Range <T> {

    /**
     * Set lower bound for this range, @null} indicates unbounded/infinite value.
     */
    void setFrom(T o);


    /**
     * Set upper bound for this range, {@null} indicates unbounded/infinite value.
     */
    void setTo (T o);


    /**
     * Get lower bound for this range or {@null} if no such bound.
     */
    T getFrom();


    /**
     * Get upper bound for this range or {@null} if no such bound.
     */
    T getTo();


    /**
     * Parse a string representation of the range value
     */
    T parse(String s);

}
