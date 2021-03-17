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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class FileSection {

    /** The lines. */
    private final Map<String, String> lines;

    /**
     * Gets the lines.
     *
     * @return the lines
     */
    protected final Map<String, String> getLines() {
        return this.lines;
    }

    /**
     * Instantiates a new file section.
     */
    protected FileSection() {
        this(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    protected FileSection(Map<String, String> lines0) {
        lines = lines0;
    }

    public static final Pattern DOLLAR_SIGN_KV_SEPARATOR = Pattern.compile(Pattern.quote("$"));
    public static final Pattern ARROW_KV_SEPARATOR = Pattern.compile(Pattern.quote("->"));
    public static final Pattern EQUALS_KV_SEPARATOR = Pattern.compile(Pattern.quote("="));
    public static final Pattern COLON_KV_SEPARATOR = Pattern.compile(Pattern.quote(":"));

    private static final String BAR_PAIR_SPLITTER = Pattern.quote("|");

    private static Table<String, Pattern, Map<String, String>> parseToMapCache = HashBasedTable.create();

    public static Map<String, String> parseToMap(final String line, final Pattern kvSeparator) {
        Map<String, String> result = parseToMapCache.get(line, kvSeparator);
        if (result != null) {
            return result;
        }
        result = parseToMapImpl(line, kvSeparator);
        parseToMapCache.put(line, kvSeparator, result);
        return result;
    }

    private static Map<String, String> parseToMapImpl(final String line, final Pattern kvSeparator) {
        if (StringUtils.isEmpty(line)) {
            return Collections.emptyMap();
        }

        final Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        final String[] pairs = line.split(BAR_PAIR_SPLITTER);
        for (final String dd : pairs) {
            final String[] v = kvSeparator.split(dd, 2);
            result.put(v[0].trim(), v.length > 1 ? v[1].trim() : "");
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Parses the.
     *
     * @param lines the lines
     * @param kvSeparator the kv separator
     * @return the file section
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
     * Gets the.
     *
     * @param fieldName the field name
     * @return the string
     */
    public String get(final String fieldName) {
        return this.lines.get(fieldName);
    }
    
    public String get(final String fieldName, final String defaultValue) {
        return lines.containsKey(fieldName) ? this.lines.get(fieldName) : defaultValue;
    }

    public boolean contains(String keyName) { 
        return lines.containsKey(keyName);
    }

    /**
     * Gets the double.
     *
     * @param fieldName the field name
     * @return the int
     */
    public double getDouble(final String fieldName) {
        return this.getDouble(fieldName, 0.0F);
    }

    /**
     * Gets the double.
     *
     * @param fieldName the field name
     * @param defaultValue the default value
     * @return the int
     */
    public double getDouble(final String fieldName, final double defaultValue) {
        try {
            if (this.get(fieldName) == null) {
                return defaultValue;
            }

            NumberFormat format = NumberFormat.getInstance(Locale.US);
            Number number = format.parse(this.get(fieldName));

            return number.doubleValue();
        } catch (final NumberFormatException | ParseException ex) {
            return defaultValue;
        }
    }

    /**
     * Gets the int.
     *
     * @param fieldName the field name
     * @return the int
     */
    public int getInt(final String fieldName) {
        return this.getInt(fieldName, 0);
    }

    /**
     * Gets the int.
     *
     * @param fieldName the field name
     * @param defaultValue the default value
     * @return the int
     */
    public int getInt(final String fieldName, final int defaultValue) {
        try {
            return Integer.parseInt(this.get(fieldName));
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Gets the boolean.
     *
     * @param fieldName the field name
     * @return the boolean
     */
    public boolean getBoolean(final String fieldName) {
        return this.getBoolean(fieldName, false);
    }

    /**
     * Gets the boolean.
     *
     * @param fieldName the field name
     * @param defaultValue the default value
     * @return the boolean
     */
    public boolean getBoolean(final String fieldName, final boolean defaultValue) {
        final String s = this.get(fieldName);
        if (s == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(s);
    }

    /**
     * Parses the sections.
     * 
     * @param source
     *            the source
     * @return the map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, List<String>> parseSections(final List<String> source) {
        final Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String currentSection = "";
        List<String> currentList = null;

        for (final String s : source) {
            final String st = s.trim();
            if (st.length() == 0) {
                continue;
            }
            if (st.startsWith("[") && st.endsWith("]")) {
                if ((currentList != null) && (currentList.size() > 0)) {
                    final Object oldVal = result.get(currentSection);
                    if ((oldVal != null) && (oldVal instanceof List<?>)) {
                        currentList.addAll((List<String>) oldVal);
                    }
                    result.put(currentSection, currentList);
                }

                final String newSection = st.substring(1, st.length() - 1);
                currentSection = newSection;
                currentList = null;
            } else {
                if (currentList == null) {
                    currentList = new ArrayList<>();
                }
                currentList.add(st);
            }
        }

        // save final block
        if ((currentList != null) && (currentList.size() > 0)) {
            final Object oldVal = result.get(currentSection);
            if ((oldVal != null) && (oldVal instanceof List<?>)) {
                currentList.addAll((List<String>) oldVal);
            }
            result.put(currentSection, currentList);
        }

        return result;
    }

}
