package no.uib.marcus.common.util;

import org.elasticsearch.common.Strings;

import java.util.Locale;

import static no.uib.marcus.common.util.BlackboxUtils.containsChar;
import static no.uib.marcus.common.util.BlackboxUtils.isNeitherNullNorEmpty;
import static no.uib.marcus.common.util.BlackboxUtils.isNullOrEmpty;
import static no.uib.marcus.common.util.QueryUtils.containsReservedChars;

public class SignatureUtils {

    private static final char WILDCARD = '*';

    //Special signature character
    private static final char SPECIAL_SIGNATURE_CHAR = '-';

    //List of signature prefixes for University of Bergen Library
    private static final String[] SIGNATURE_PREFIXES = {"ubb", "ubm", "sab"};

    //Ensure non-instantiability
    private SignatureUtils() {
    }

    /**
     * Appends wildcard if a given input is a valid signature, if it does not contain reserved characters
     *
     * @param value a value string to append such wildcard if it is thought to be a signature
     * @return the given string with a wildcard appended to the end
     */
    public static String appendWildcardIfValidSignature(String value) {
        if (isNeitherNullNorEmpty(value)
                && Character.isLetter(value.charAt(0))
                && !Strings.containsWhitespace(value)
                && !containsReservedChars(value)) {

            //E.g "ubb-ms-01" should be transformed to "ubb-ms-01*"
            // but not ubb+ms
            if (isSignature(value)) {
                return value + WILDCARD;
            }
            //E.g, "bros-2000" should be transformed to "*bros-2000*"
            //but not "-bros-2000" (which has different meaning)
            if (containsChar(value, SPECIAL_SIGNATURE_CHAR)) {
                return WILDCARD + value + WILDCARD;
            }

        }
        return value;
    }


    /**
     * Checks if a given string value is a valid signature
     */
    public static boolean isSignature(String value) {
        return beginsWithSignaturePrefix(value) && containsChar(value, SPECIAL_SIGNATURE_CHAR);
    }


    /**
     * Checks if a given query is likely a signature, that means it begins with signature prefix
     */
    private static boolean beginsWithSignaturePrefix(String query) {
        if (isNullOrEmpty(query)) {
            return false;
        }
        for (String prefix : SIGNATURE_PREFIXES) {
            if (query.trim().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

}
