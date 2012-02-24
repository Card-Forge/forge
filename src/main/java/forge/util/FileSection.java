package forge.util;

import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class FileSection {

    protected final Map<String, String> lines = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

    protected final Map<String, String> getLines() {
        return lines;
    }

    protected FileSection() { }

    public static FileSection parse(String line, String kvSeparator, String pairSeparator) {
        String[] pairs = line.split(Pattern.quote(pairSeparator));
        Pattern splitter = Pattern.compile(Pattern.quote(kvSeparator));
        FileSection result = new FileSection();

        for (final String dd : pairs) {
            final String[] v = splitter.split(dd, 2);
            result.lines.put(v[0].trim(), v.length > 1 ? v[1].trim() : "");
        }

        return result;
    }

    public static FileSection parse(Iterable<String> lines, String kvSeparator) {
        FileSection result = new FileSection();
        Pattern splitter = Pattern.compile(Pattern.quote(kvSeparator));
        for (final String dd : lines) {
            final String[] v = splitter.split(dd, 2);
            result.lines.put(v[0].trim(), v.length > 1 ? v[1].trim() : "");
        }

        return result;
    }

    public String get(String fieldName) {
        return lines.get(fieldName);
    }

    public int getInt(String fieldName) { return getInt(fieldName, 0); }
    public int getInt(String fieldName, int defaultValue) {
        try {
            return Integer.parseInt(get(fieldName));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    public boolean getBoolean(String fieldName) { return getBoolean(fieldName, false); }
    public boolean getBoolean(String fieldName, boolean defaultValue) {
        String s = get(fieldName);
        if (s == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(s);
    }


}
