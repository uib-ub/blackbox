package no.uib.marcus.common.util;

import java.util.Map;

/**
 * A utility class for different convenience methods
 *
 * @author Hemed Al Ruwehy
 * Date: 15/11/2016.
 */
public class BlackboxUtils {
    public static final String MINUS = "-";


    //Prevent this class from being initialized
    private BlackboxUtils(){}


    /**
     * If input map contains an array of only one element, then convert the
     * the array to string making it a proper JSON string.
     *
     * @param map input map
     * @return a map where a value of type Array that has only one element is converted to a string.
     */
    public static Map<String, Object> jsonify(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getValue().getClass().isArray()) {
                String[] valueArray = (String[])entry.getValue();
                if (valueArray.length == 1) {
                    String value = valueArray[0];
                    if (isNumeric(value)) {
                        map.put(entry.getKey(), Integer.parseInt(value));
                    }
                    else if (value.equalsIgnoreCase("true")){
                      map.put(entry.getKey(), true);
                    }
                    else if(value.equalsIgnoreCase("false")){
                        map.put(entry.getKey(), false);
                    }
                    else {
                        map.put(entry.getKey(), value);
                    }

                }
            }
        }
        return map;
    }

    /**
     * Check whether the character sequence is numeric
     *
     * @param cs character sequence
     **/
    public static boolean isNumeric(CharSequence cs) {
        if (isNullOrEmpty(cs)) {
            return false;
        } else {
            int sz = cs.length();
            for (int i = 0; i < sz; ++i) {
                if (!Character.isDigit(cs.charAt(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * A wrapper for getting map key and throw exception if value does not exist
     *
     * @param map a source map
     * @param key a key which it's value need to be retrieved
     * @return a value for the corresponding key, if exists
     * <p>
     * throws NullPointerException if key does not exist
     */
    public static String getValueAsString(Map<String, String> map, String key) {
        if (map.get(key) == null) {
            throw new NullPointerException("Value not found for key [" + key + "]. " +
                    "Make sure both key and value exist " +
                    "and are the same to those of your running Elasticsearch cluster");
        }
        return map.get(key);
    }

    /**
     * See #getValueAsString
     */
    public static int getValueAsInt(Map<String, String> map, String key) {
        return Integer.parseInt(getValueAsString(map, key));
    }


    /**
     * Checks if a given string is either null or empty
     *
     * @param s  a string to check
     * @return true if this string is null or empty, otherwise false
     */
    public static boolean isNullOrEmpty(CharSequence s) {
        return s == null || s.length() == 0;
    }

    /**
     * Checks if a given string is neither null nor empty
     *
     * @param s  a string to check
     * @return true if this string is not null or not empty, otherwise false
     */
    public static boolean isNeitherNullNOrEmpty(CharSequence s) {
        return s != null && s.length() > 0;
    }

    /**
     * Checks if a given string does not start with a given character
     */
    public static boolean containsChar(String s, char character) {
        return !isNullOrEmpty(s) && s.indexOf(character) > -1;
    }

}
