package no.uib.marcus.common;

import java.util.Map;

/**
 * @author Hemed
 * Date: 15/11/2016.
 */
public class Settings {
    public static final String MINUS = "-";

    //Any of this format is OK
    public static final String DEFAULT_DATE_FORMAT = "yyyy-mm-dd||yyyy-mm||yyyy";

    //Prevent this class from being initialized
    private Settings(){}


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
        if (cs == null || cs.length() == 0) {
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
}
