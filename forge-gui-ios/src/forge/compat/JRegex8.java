package forge.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * Bytecode-rewrite targets for java.util.regex.Matcher's named-group API,
 * which MobiVM's Java-7-era runtime lacks (Matcher has only group()/group(int)).
 * The group NAME -> INDEX mapping is derived by scanning the pattern source
 * for named capturing groups — the standard trick for pre-API-26 Android.
 */
public final class JRegex8 {
    private JRegex8() { }

    private static final Map<String, Map<String, Integer>> CACHE = new HashMap<>();

    public static String group(Matcher m, String name) {
        return m.group(indexOf(m, name));
    }

    public static int start(Matcher m, String name) {
        return m.start(indexOf(m, name));
    }

    public static int end(Matcher m, String name) {
        return m.end(indexOf(m, name));
    }

    private static int indexOf(Matcher m, String name) {
        String regex = m.pattern().pattern();
        Map<String, Integer> byName;
        synchronized (CACHE) {
            byName = CACHE.get(regex);
            if (byName == null) {
                byName = parseGroupNames(regex);
                CACHE.put(regex, byName);
            }
        }
        Integer idx = byName.get(name);
        if (idx == null) {
            throw new IllegalArgumentException("No group with name <" + name + ">");
        }
        return idx;
    }

    private static Map<String, Integer> parseGroupNames(String regex) {
        Map<String, Integer> result = new HashMap<>();
        int groupIndex = 0;
        boolean inClass = false;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '\\') {
                i++; // skip escaped char
            } else if (inClass) {
                if (c == ']') {
                    inClass = false;
                }
            } else if (c == '[') {
                inClass = true;
            } else if (c == '(') {
                if (i + 1 < regex.length() && regex.charAt(i + 1) == '?') {
                    // named group: (?<name>...) — but not lookbehind (?<= (?<!
                    if (i + 2 < regex.length() && regex.charAt(i + 2) == '<'
                            && i + 3 < regex.length()
                            && regex.charAt(i + 3) != '=' && regex.charAt(i + 3) != '!') {
                        int close = regex.indexOf('>', i + 3);
                        if (close > 0) {
                            groupIndex++;
                            result.put(regex.substring(i + 3, close), groupIndex);
                        }
                    }
                    // other (?...) constructs are non-capturing
                } else {
                    groupIndex++;
                }
            }
        }
        return result;
    }
}
