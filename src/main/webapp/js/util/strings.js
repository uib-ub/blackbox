/**
 * A factory JavaScript file that contains common methods
 * to be used throughout the system
 * **/


/**
 A method to append * or ~ in the query string
 <p/>
 @param query_string a query string.
 @param default_freetext_fuzzify - should be either * or ~,
 if *, * will be prepended and appended to each string in the freetext search term
 if ~, then ~ will be appended to each string in the freetext search term.
 If * or ~ or : are already in the freetext search term, no action will be taken.
 @param query_string - a query string.
 **/
function fuzzify(query_string, default_freetext_fuzzify) {
    var rqs = query_string;
    if (default_freetext_fuzzify !== undefined) {
        if (default_freetext_fuzzify === "*" || default_freetext_fuzzify === "~") {
            //Do not do anything if query string has either one of the following chars.
            if (query_string.indexOf('*') === -1 && query_string.indexOf('~') === -1 &&
                query_string.indexOf(':') === -1 && query_string.indexOf('"') === -1 &&
                query_string.indexOf('[') === -1) {
                var option_parts = query_string.split(' ');
                var pq = "";
                for (var oi = 0; oi < option_parts.length; oi++) {
                    var oip = option_parts[oi];

                    //We want the string part to be greater than 1 char,
                    // and it should not contain the following special chars.
                    if (oip.length > 1 && oip.indexOf(')') === -1 && oip.indexOf('(') === -1) {
                        oip = oip + default_freetext_fuzzify;
                    }
                    pq += oip + ' ';
                }
                rqs = pq;
            }
        }
    }
    return rqs;
}
/**
 * Check for the string and return null if it is empty or undefined
 * @param s
 * @returns {*}
 */
function stripEmpty(s) {
    if(s == undefined || s === "") {
        return null;
    }
    return s;
}
