package no.uib.marcus.range;


/**
 * Interface for manipulating Ranges
 * @param <T> range of a specified Type
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
     * Parse a string representation to a range value of type T
     */
    T parse(String s);

}
