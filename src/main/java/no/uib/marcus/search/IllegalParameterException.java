package no.uib.marcus.search;

/**
 * Custom exception class for illegal arguments
 **/
public class IllegalParameterException extends RuntimeException {

    /**
     * Constructs illegal parameter exceptionnwith {@code null} as its detail message.
     */
    public IllegalParameterException(){
        super();
    }

   /**
    * Constructs a new illegal parameter exception with the specified detail message.
    **/
    public IllegalParameterException(String message) {
        super(message);
    }

   /**
    * Constructs a new illegal parameter exception with the specified detail message and a cause.
    **/
    public IllegalParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
