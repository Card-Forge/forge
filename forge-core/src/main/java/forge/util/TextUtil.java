package forge.util;

import forge.item.PaperCard;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
        List<String> result = new ArrayList<String>();
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
}
