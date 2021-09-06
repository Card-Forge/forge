package forge.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import forge.item.IPaperCard;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableSortedMap;

import forge.item.PaperCard;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TextUtil {

    static ImmutableSortedMap<Integer,String> romanMap = ImmutableSortedMap.<Integer,String>naturalOrder()
    .put(1000, "M").put(900, "CM")
    .put(500, "D").put(400, "CD")
    .put(100, "C").put(90, "XC")
    .put(50, "L").put(40, "XL")
    .put(10, "X").put(9, "IX")
    .put(5, "V").put(4, "IV").put(1, "I").build();
    
    public static String toRoman(int number) {
        if (number <= 0) {
            return "";
        }
        int l = romanMap.floorKey(number);
        return romanMap.get(l) + toRoman(number-l);
    }
    public static String normalizeText(String text) {
        if (text == null)
            return IPaperCard.NO_ARTIST_NAME;
        return Normalizer.normalize(text, Normalizer.Form.NFD);

    }
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
            mapAsString.append(p.getKey()).append(" => ").append(p.getValue() == null ? "(null)" : p.getValue().toString());
        }
        return mapAsString.toString();
    }

    public static String[] split(CharSequence input, char delimiter) {
        return splitWithParenthesis(input, delimiter, Integer.MAX_VALUE, '\0', '\0', true);
    }

    public static String[] split(CharSequence input, char delimiter, int limit) {
        return splitWithParenthesis(input, delimiter, limit, '\0', '\0', true);
    }
    public static String[] splitWithParenthesis(CharSequence input, char delimiter) {
        return splitWithParenthesis(input, delimiter, Integer.MAX_VALUE, '(', ')', true);
    }

    public static String[] splitWithParenthesis(CharSequence input, char delimiter, char openPar, char closePar) {
        return splitWithParenthesis(input, delimiter, Integer.MAX_VALUE, openPar, closePar, true);
    }

    public static String[] splitWithParenthesis(CharSequence input, char delimiter, int limit) {
        return splitWithParenthesis(input, delimiter, limit, '(', ')', true);
    }

    public static String[] splitWithParenthesis(CharSequence input, char delimiter, char openPar, char closePar, int limit) {
        return splitWithParenthesis(input, delimiter, limit, openPar, closePar, true);
    }

    /** 
     * Split string separated by a single char delimiter, can take parenthesis in account
     * It's faster than String.split, and allows parenthesis
     */
    public static String[] splitWithParenthesis(CharSequence input, char delimiter, int maxEntries, char openPar, char closePar, boolean skipEmpty) {
        List<String> result = new ArrayList<>();
        // Assume that when equal non-zero parenthesis are passed, they need to be discarded
        boolean trimParenthesis = openPar == closePar && openPar > 0;
        int nPar = 0;
        int len = input.length();
        int start = 0;
        int idx = 1;
        for (int iC = 0; iC < len; iC++) {
            char c = input.charAt(iC);
            if (closePar > 0 && c == closePar && nPar > 0) { nPar--; }
            else if (openPar > 0 && c == openPar) nPar++;

            if (c == delimiter && nPar == 0 && idx < maxEntries) {
                if (iC > start || !skipEmpty) {
                    result.add(input.subSequence(start, iC).toString());
                    idx++;
                }
                start = iC + 1;
            }
        }

        if (len > start || !skipEmpty)
            result.add(input.subSequence(start, len).toString());

        String[] toReturn = result.toArray(ArrayUtils.EMPTY_STRING_ARRAY);
        return trimParenthesis ? StringUtils.stripAll(toReturn, String.valueOf(openPar)) : toReturn;
    }

    public static String join(Iterable<String> strs, String delim) {
    	StringBuilder sb = new StringBuilder();
    	for (String str : strs) {
    		if (sb.length() > 0) {
    			sb.append(delim);
    		}
    		sb.append(str);
    	}
    	return sb.toString();
    }

    /**
     * Converts an enum value to a printable label but upcasing the first letter
     * and lcasing all subsequent letters
     */
    public static String enumToLabel(Enum<?> val) {
        return val.toString().substring(0, 1).toUpperCase(Locale.ENGLISH) +
                val.toString().substring(1).toLowerCase(Locale.ENGLISH);
    }

    public static String buildFourColumnList(String firstLine, Iterable<PaperCard> cAnteRemoved) {
        StringBuilder sb = new StringBuilder(firstLine);
        int i = 0;
        for (PaperCard cp: cAnteRemoved) {
            if (i != 0) { sb.append(", "); }
            if (i % 4 == 0) { sb.append("\n"); }
            sb.append(cp);
            i++;
        }
        return sb.toString();
    }

    private static final char CHAR_UNDEFINED = (char)65535; //taken from KeyEvent.CHAR_UNDEFINED which can't live here since awt library can't be referenced

    public static boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return (!Character.isISOControl(c)) &&
                c != CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }

    public enum PhraseCase {
        Title,
        Sentence,
        Lower
    }

    public static String splitCompoundWord(String word, PhraseCase phraseCase) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            char ch = word.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0) {
                    builder.append(" ");
                }
                switch (phraseCase) {
                case Title:
                    builder.append(ch);
                    break;
                case Sentence:
                    if (i > 0) {
                        builder.append(ch);
                    }
                    else {
                        builder.append(Character.toLowerCase(ch));
                    }
                    break;
                case Lower:
                    builder.append(Character.toLowerCase(ch));
                    continue;
                }
            }
            else {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public static String capitalize(final String s) {
        return s.substring(0, 1).toUpperCase()
                + s.substring(1);

    }

    //concatenate with spaces
    public static String concatWithSpace(String ... s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length; i++) {
            sb.append(s[i]);
            if (i < s.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    //concatenate no spaces
    public static String concatNoSpace(String ... s) {
        StringBuilder sb = new StringBuilder();
        for (String str : s) {
            sb.append(str);
        }
        return sb.toString();
    }

    //enclosed in Parentheses
    public static String enclosedParen(String s){
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(s);
        sb.append(")");
        return sb.toString();
    }

    //enclosed in Brackets
    public static String enclosedBracket(String s){
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(s);
        sb.append("]");
        return sb.toString();
    }

    //enclosed in Single Quote
    public static String enclosedSingleQuote(String s){
        StringBuilder sb = new StringBuilder();
        sb.append("'");
        sb.append(s);
        sb.append("'");
        return sb.toString();
    }

    //enclosed in Double Quote
    public static String enclosedDoubleQuote(String s){
        StringBuilder sb = new StringBuilder();
        sb.append("\"");
        sb.append(s);
        sb.append("\"");
        return sb.toString();
    }

    //suffix
    public static String addSuffix(String s, String suffix){
        StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.append(suffix);
        return sb.toString();
    }

    //prefix
    public static String addPrefix(String prefix, String s){
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append(s);
        return sb.toString();
    }

    //fast Replace
    public static String fastReplace( String str, String target, String replacement ) {
        if (str == null) {
            return null;
        }
        int targetLength = target.length();
        if( targetLength == 0 ) {
            return str;
        }
        int idx2 = str.indexOf( target );
        if( idx2 < 0 ) {
            return str;
        }
        StringBuilder sb = new StringBuilder( targetLength > replacement.length() ? str.length() : str.length() * 2 );
        int idx1 = 0;
        do {
            sb.append( str, idx1, idx2 );
            sb.append( replacement );
            idx1 = idx2 + targetLength;
            idx2 = str.indexOf( target, idx1 );
        } while( idx2 > 0 );
        sb.append( str, idx1, str.length() );
        return sb.toString();
    }
    //Convert to Mana String
    public static String toManaString(String ManaProduced){
        if (ManaProduced == "mana"|| ManaProduced.contains("Combo")|| ManaProduced.contains("Any"))
            return "mana";//fix manamorphose stack description and probably others..
        return "{"+TextUtil.fastReplace(ManaProduced," ","}{")+"}";
    }
}
