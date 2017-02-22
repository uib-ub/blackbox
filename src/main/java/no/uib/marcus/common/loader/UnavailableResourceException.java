package no.uib.marcus.common.loader;


public class UnavailableResourceException extends RuntimeException {

    /**
     * Constructs a new unavailable resource exception
     **/
   public UnavailableResourceException() {
        super();
    }
    /**
     * Constructs a new unavailable resource exception with the specified detail message.
     **/
    public UnavailableResourceException(String message) {
        super(message);
    }
}
