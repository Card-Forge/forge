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

    public static <T> String nounWithAmount(int cnt, String noun) {
        String suffix = cnt <= 1 ? "" : ( noun.endsWith("s") || noun.endsWith("x") ? "es" : "s");
        final String strCount;
        if( cnt == 1 )
            strCount = startsWithVowel(noun) ? "an " : "a ";
        else 
            strCount = String.valueOf(cnt) + " "; 
        return strCount + noun + suffix;
    }    
    
    /**
     * TODO: Write javadoc for this method.
     * @param name
     * @return
     */
    public static String getPossesive(String name) {
        return name.endsWith("s") ? name + "'" : name + "'s";
    }
    
    public static boolean startsWithVowel(String word) {
        return isVowel(word.trim().charAt(0));
    }
    
    private static final char[] vowels = { 'a', 'i', 'e', 'o', 'u' }; 
    public static boolean isVowel(char letter) {
        char l = Character.toLowerCase(letter);
        for(char c : vowels)
            if ( c == l ) return true;
        return false;
        
    }
}
