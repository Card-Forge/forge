package forge.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.ArrayUtils;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TextUtil {

    /**
     * Safely converts an object to a String.
     * 
     * @param obj
     *            to convert; may be null
     * 
     * @return "null" if obj is null, obj.toString() otherwise
     */
    public static String safeToString(final Object obj) {
        return obj == null ? "null" : obj.toString();
    }

    public static String mapToString(Map<String, ?> map) {
        StringBuilder mapAsString = new StringBuilder();
        boolean isFirst = true;
        for (Entry<String, ?> p : map.entrySet()) {
            if (isFirst) {
                isFirst = false;
            } else {
                mapAsString.append("; ");
            }
            mapAsString.append(p.getKey() + " => " + (p.getValue() == null ? "(null)" : p.getValue().toString()));
        }
        return mapAsString.toString();
    }

    public static String[] split(CharSequence input, char delimiter) { 
        return splitWithParenthesis(input, delimiter, '\0', '\0', true);
    }
    
    public static String[] split(CharSequence input, char delimiter, boolean skipEmpty) {
        return splitWithParenthesis(input, delimiter, '\0', '\0', skipEmpty);
    }
    
    public static String[] splitWithParenthesis(CharSequence input, char delimiter, char openPar, char closePar) {
        return splitWithParenthesis(input, delimiter, openPar, closePar, true);
    }
    
    /** 
     * Split string separated by a single char delimiter, can take parenthesis in account
     * It's faster than String.split, and allows parenthesis 
     */
    public static String[] splitWithParenthesis(CharSequence input, char delimiter, char openPar, char closePar, boolean skipEmpty) {
        List<String> result = new ArrayList<String>();
        int nPar = 0;
        int len = input.length();
        int start = 0;
        for (int iC = 0; iC < len; iC++ ) {
            char c = input.charAt(iC);
            if( openPar > 0 && c == openPar ) nPar++;
            if( closePar > 0 && c == closePar ) { nPar = nPar > 0 ? nPar - 1 : 0; }

            if( c == delimiter && nPar == 0) {
                if( iC > start || !skipEmpty )
                    result.add(input.subSequence(start, iC).toString());
                start = iC + 1;
            }
        }

        if( len > start || !skipEmpty )
            result.add(input.subSequence(start, len).toString());

        return result.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
    }

}
