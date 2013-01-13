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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class FileSection {

    /** The lines. */
    private final Map<String, String> lines = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);

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
    }

    /**
     * Parses the.
     *
     * @param line the line
     * @param kvSeparator the kv separator
     * @param pairSeparator the pair separator
     * @return the file section
     */
    public static FileSection parse(final String line, final String kvSeparator, final String pairSeparator) {
        final String[] pairs = line.split(Pattern.quote(pairSeparator));
        final Pattern splitter = Pattern.compile(Pattern.quote(kvSeparator));
        final FileSection result = new FileSection();

        for (final String dd : pairs) {
            final String[] v = splitter.split(dd, 2);
            result.lines.put(v[0].trim(), v.length > 1 ? v[1].trim() : "");
        }

        return result;
    }

    /**
     * Parses the.
     *
     * @param lines the lines
     * @param kvSeparator the kv separator
     * @return the file section
     */
    public static FileSection parse(final Iterable<String> lines, final String kvSeparator) {
        final FileSection result = new FileSection();
        final Pattern splitter = Pattern.compile(Pattern.quote(kvSeparator));
        for (final String dd : lines) {
            final String[] v = splitter.split(dd, 2);
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
        final Map<String, List<String>> result = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
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
                    currentList = new ArrayList<String>();
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
