package no.uib.marcus.common.util;

import org.elasticsearch.common.Strings;

import java.util.Locale;

import static no.uib.marcus.common.util.BlackboxUtils.*;
import static no.uib.marcus.common.util.QueryUtils.containsReservedChars;

public class SignatureUtils {

    private static final char WILDCARD = '*';

    //Special signature character
    private static final char SPECIAL_SIGNATURE_CHAR = '-';

    //List of signature prefixes for the University of Bergen Library (UBB)
    private static final String[] UBB_SIGNATURE_PREFIXES = {"ubb", "ubm", "sab"};

    //List of signature prefixes for Wittgensteins Archives at UiB (WAB)
    private static final String[] WAB_SIGNATURE_PREFIXES = {"ms-", "ts-"};

    //Ensure non-instantiability
    private SignatureUtils() {
    }

    /**
     * Appends wildcard if a given input is a valid signature, if it does not contain reserved characters
     *
     * @param value a value string to append such wildcard if it is thought to be a signature
     * @return the given string with a wildcard appended to the end
     */
    public static String appendLeadingWildcardIfWABSignature(String value) {
        if (isNeitherNullNorEmpty(value)) {
            value = value.trim();
            if (!value.matches("\\s") && value.indexOf(WILDCARD) == -1) {
                if (isWABSignature(value)) {
                    return value + WILDCARD; //"ms-101" should be transformed to "ms-101*"
                }
            }
        }
        return value;
    }


    /**
     * Appends wildcard if a given input is a valid signature, if it does not contain reserved characters
     *
     * @param value a value string to append such wildcard if it is thought to be a signature
     * @return the given string with a wildcard appended to the end
     */
    public static String appendWildcardIfUBBSignature(String value) {
        if (isNeitherNullNorEmpty(value)) {
            value = value.trim();
            if (Character.isLetter(value.charAt(0))
                    && !value.matches("\\s")
                    && !containsReservedChars(value)) {

                //"ubb-ms-01" should be transformed to "ubb-ms-01*" but not ubb+ms
                if (isUBBSignature(value)) {
                    return value + WILDCARD;
                }
                //"bros-2000" should be transformed to "*bros-2000*" but not "-bros-2000"
                if (containsChar(value, SPECIAL_SIGNATURE_CHAR)) {
                    return WILDCARD + value + WILDCARD;
                }
            }
        }
        return value;
    }


    /**
     * Checks if a given string value is a valid UBB signature
     */
    public static boolean isUBBSignature(String value) {
        return beginsWithSignaturePrefix(value, UBB_SIGNATURE_PREFIXES)
                && containsChar(value, SPECIAL_SIGNATURE_CHAR);
    }

    /**
     * Checks if a given string value is a valid UBB signature
     */
    public static boolean isWABSignature(String value) {
        return beginsWithSignaturePrefix(value, WAB_SIGNATURE_PREFIXES);
    }


    /**
     * Checks if a given query is likely a signature, that means it begins with signature prefixes
     *
     * @param query    query string to check
     * @param prefixes prefixes
     */
    private static boolean beginsWithSignaturePrefix(String query, String[] prefixes) {
        if (isNullOrEmpty(query)) {
            return false;
        }
        for (String prefix : prefixes) {
            if (query.trim().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

}
