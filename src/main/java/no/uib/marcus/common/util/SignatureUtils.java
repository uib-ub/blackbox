package no.uib.marcus.common.util;

import org.elasticsearch.common.Strings;

import static no.uib.marcus.common.util.BlackboxUtils.containsChar;
import static no.uib.marcus.common.util.BlackboxUtils.isNullOrEmpty;
import static no.uib.marcus.common.util.QueryUtils.containsReservedChars;

public class SignatureUtils {

    private static final char WILDCARD = '*';

    //Special signature character
    private static final char SPECIAL_SIGNATURE_CHAR = '-';

    //List of signature prefixes for University of Bergen Library
    private static final String[] UBB_SIGNATURE_PREFIXES = {"ubb", "ubm"};


    private SignatureUtils(){
    }

    /**
     * Appends wildcard if a given input is UBB signature, but only if
     * it does not contain reserved characters
     *
     * @param signature a signature string to append such wildcard to
     * @return the given string with a wildcard appended to the end
     */
    public static String appendWildcardIfValidSignature(String signature) {
        if (!isNullOrEmpty(signature)
                && Character.isLetter(signature.charAt(0))
                && !Strings.containsWhitespace(signature)
                && !containsReservedChars(signature)) {

            //E.g "ubb-ms-01" should be transformed to "ubb-ms-01*"
            // but not ubb+ms
            if (isValidSignature(signature)) {
                return signature + WILDCARD;
            }
            //E.g, "bros-2000" should be transformed to "*bros-2000*"
            // but not "-bros-2000" (which has different meaning)
            if (containsChar(signature, SPECIAL_SIGNATURE_CHAR)) {
                return WILDCARD + signature + WILDCARD;
            }

        }
        return signature;
    }


    /**
     * Checks if it is UBB signature
     */
    public static boolean isValidSignature(String signature) {
        return beginsWithSignaturePrefix(signature) && containsChar(signature, SPECIAL_SIGNATURE_CHAR);
    }


    /**
     * Checks if a given query is likely a signature, that means it begins with signature prefix
     */
    private static boolean beginsWithSignaturePrefix(String query) {
        if (isNullOrEmpty(query)) {
            return false;
        }
        for (String prefix : UBB_SIGNATURE_PREFIXES) {
            if (query.toLowerCase().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

}
