package no.uib.marcus.common;

import java.util.logging.Logger;
import no.uib.marcus.common.util.StringUtils;

import java.util.Arrays;

/**
 * A list of service names
 ***/
public enum ServiceName {
    /**
     * Marcus service name
     */
    MARCUS,

    /**
     * Wittgenstein Archives service name
     */
    WAB,

    /**
     * Marcus Admin service name
     */
    MARCUS_ADMIN,

    /**
     * Naturen service name
     */
    NATUREN
    ;

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(ServiceName.class.getName());

    /**
     * Get the corresponding enum from its string representation
     * @param serviceString a service parameter passed by the user
     *
     * @return enum which is the result of a string representation
     */
    public static ServiceName toEnum(String serviceString) {
        ServiceName service;
        try {
            if(!StringUtils.hasText(serviceString)) {//Use the default service if nothing is specified
                return ServiceName.MARCUS;
            }
            service = ServiceName.valueOf(serviceString.toUpperCase());
        }
        catch (IllegalArgumentException e){
            logger.severe("Service parameter is not recognized. Found [" + serviceString + "]" +
                    " but expected one of " + Arrays.asList(ServiceName.values()));
            /*Fail and do not continue*/
            throw e;
        }
        return service;
    }

}
