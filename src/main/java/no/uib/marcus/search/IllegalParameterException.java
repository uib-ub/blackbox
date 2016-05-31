package no.uib.marcus.search;

/**
 * Custom exception class for illegal arguments.
 * Thrown to indicate that a method has been passed an illegal or
 * inappropriate parameter.
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
