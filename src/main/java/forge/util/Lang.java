package forge.util;

import java.util.List;

import com.google.common.base.Function;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class Lang {

    /**
     * TODO: Write javadoc for this method.
     * @param position
     * @return
     */
    public static String getOrdinal(int position) {
        String[] sufixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (position % 100) {
        case 11:
        case 12:
        case 13:
            return position + "th";
        default:
            return position + sufixes[position % 10];
        }
    }
    
    public static <T> String joinHomogenous(List<T> objects) { return joinHomogenous(objects, null); }
    public static <T> String joinHomogenous(List<T> objects, Function<T, String> accessor) {
        int remaining = objects.size();
        StringBuilder sb = new StringBuilder();
        for(T obj : objects) {
            remaining--;
            if( accessor != null )
                sb.append(accessor.apply(obj));
            else 
                sb.append(obj);
            if( remaining > 1 ) sb.append(", ");
            if( remaining == 1 ) sb.append(" and ");
        }
        return sb.toString();
    }
    
    public static <T> String joinVerb(List<T> subjects, String verb) {
        // English is simple - just add (s) for multiple objects. 
        return subjects.size() > 1 ? verb : verb + "s";
    }

    public static <T> String joinNounToAmount(int cnt, String noun) {
        //  Simpliest check
        return cnt > 1 ? noun : ( noun.endsWith("s") || noun.endsWith("x") ? noun + "es" : noun + "s");
    }    
    
    /**
     * TODO: Write javadoc for this method.
     * @param name
     * @return
     */
    public static String getPossesive(String name) {
        return name.endsWith("s") ? name + "'" : name + "'s";
    }
}
