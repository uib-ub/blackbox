package no.uib.marcus.common;

import org.apache.log4j.Logger;
import org.elasticsearch.common.Strings;

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
     * Skeivtarkiv service name
     */
    SKA,

    /**
     * Wittgenstein Archives service name
     */
    WAB,

    /**
     * Marcus Admin service name
     */
    MARCUS_ADMIN
    ;

    /**
     * Logger
     */
    private static final Logger logger = Logger.getLogger(ServiceName.class);

    /**
     * Get corresponding enum from it's string representation
     * @param serviceString a service parameter passed by user
     *
     * @return enum which is the result of a string representation
     */
    public static ServiceName toEnum(String serviceString) {
        ServiceName service;
        try {
            //Fall to default service, if nothing is specified
            if(!Strings.hasText(serviceString)) {
                return ServiceName.MARCUS;
            }
            service = ServiceName.valueOf(serviceString.toUpperCase());
        }
        catch (IllegalArgumentException e){
            logger.error("Service parameter is not recognized. Found [" + serviceString + "]" +
                    " but expected one of " + Arrays.asList(ServiceName.values()).toString());
            /*Fail and do not continue*/
            throw e;
        }
        return service;
    }

}
