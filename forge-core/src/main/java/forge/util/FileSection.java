/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * Parse text file to extract [sections] and key/value data.
 * Store the result in a HashMap
 */
public class FileSection {

    /** The lines. */
    protected final Map<String, String> lines;

    /**
     * Instantiates a new file section.
     */
    protected FileSection() {
        lines = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    public static final Pattern DOLLAR_SIGN_KV_SEPARATOR = Pattern.compile(Pattern.quote("$"));
    public static final Pattern ARROW_KV_SEPARATOR = Pattern.compile(Pattern.quote("->"));
    public static final Pattern EQUALS_KV_SEPARATOR = Pattern.compile(Pattern.quote("="));
    public static final Pattern COLON_KV_SEPARATOR = Pattern.compile(Pattern.quote(":"));
    private static final String BAR_PAIR_SPLITTER = Pattern.quote("|");

    private static final Table<String, Pattern, Map<String, String>> parseToMapCache = HashBasedTable.create();

    /**
     * Parses the key=value text line and return a HashMap
     *
     * @param line the text line to parse
     * @param kvSeparator the key/value separator
     * @return a HashMap
     */
    public static Map<String, String> parseToMap(final String line, final Pattern kvSeparator) {
        Map<String, String> cached = parseToMapCache.get(line, kvSeparator);
        if (cached != null) {
            return cached;
        }

        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (!StringUtils.isEmpty(line)) {
            for (final String dd : line.split(BAR_PAIR_SPLITTER)) {
                final String[] v = kvSeparator.split(dd, 2);
                result.put(v[0].trim(), v.length > 1 ? v[1].trim() : "");
            }
        }
        cached = Collections.unmodifiableMap(result);
        parseToMapCache.put(line, kvSeparator, cached);
        return cached;
    }

    /**
     * Parses the key=value text lines and return a HashMap
     *
     * @param lines the text lines to parse
     * @param kvSeparator the key/value separator
     * @return a FileSection Object containing the HashMap
     */
    public static FileSection parse(final Iterable<String> lines, final Pattern kvSeparator) {
        final FileSection result = new FileSection();
        for (final String dd : lines) {
            final String[] v = kvSeparator.split(dd, 2);
            result.lines.put(v[0].trim(), v.length > 1 ? v[1].trim() : "");
        }

        return result;
    }

    /**
     * Parses the sections ([sectionName]) from a list of text line
     *
     * @param lines
     *            the text lines to parse
     * @return a LinkedHashMap containing the sections and text line associated. The order of the sections is preserved
     */
    public static Map<String, List<String>> parseSections(final List<String> lines) {
        final Map<String, List<String>> result = new LinkedHashMap<>();
        int lineNumber = 0;
        String section = null;

        do{
            String line = lines.get(lineNumber++);
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1);
                if(!result.containsKey(section)) {
                    result.put(section, new ArrayList<>());
                }
            }
            else if(null != section && !line.isEmpty() && !line.startsWith("#")){
                result.get(section).add(line);
            }
        }
        while(lineNumber<lines.size());

        return result;
    }



    public String get(final String fieldName) {
        return this.lines.get(fieldName);
    }
    
    public String get(final String fieldName, final String defaultValue) {
        return lines.containsKey(fieldName) ? this.lines.get(fieldName) : defaultValue;
    }

    public boolean contains(String fieldName) {
        return lines.containsKey(fieldName);
    }

    public double getDouble(final String fieldName, final double defaultValue) {
        final String field = this.get(fieldName);
        if (null == field)  return defaultValue;
        try {
            NumberFormat format = NumberFormat.getInstance(Locale.US);
            Number number = format.parse(field);
            return number.doubleValue();
        } catch (final NumberFormatException | ParseException ex) {
            return defaultValue;
        }
    }

    public int getInt(final String fieldName) {
        return this.getInt(fieldName, 0);
    }


    public int getInt(final String fieldName, final int defaultValue) {
        try {
            return Integer.parseInt(this.get(fieldName));
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    public boolean getBoolean(final String fieldName) {
        return this.getBoolean(fieldName, false);
    }

    public boolean getBoolean(final String fieldName, final boolean defaultValue) {
        final String field = this.get(fieldName);
        if (field == null) return defaultValue;
        return "true".equalsIgnoreCase(field);
    }
}
