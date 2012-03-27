package forge.util;

import java.util.Map;
import java.util.Map.Entry;

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
        String result;
    
        if (obj == null) {
            result = "null";
        } else {
            result = obj.toString();
        }
    
        return result;
    }
    
    public static String mapToString(Map<String,?> map) {
        StringBuilder mapAsString = new StringBuilder();
        boolean isFirst = true;
        for(Entry<String, ?> p : map.entrySet()) {
            if( isFirst )
                isFirst = false;
            else
                mapAsString.append("; ");
            mapAsString.append( p.getKey() + " => " + (p.getValue() == null ? "(null)" : p.getValue().toString()) );
        }
        return mapAsString.toString();
    }

}
