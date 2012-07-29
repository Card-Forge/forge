/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2009  Clemens Koza
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

package tree.properties.types;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tree.properties.PropertyType;
import tree.properties.TreeProperties;

/**
 * The class FileType.
 * 
 * @author Clemens Koza
 * @version V0.0 19.08.2009
 */
public class FileType implements PropertyType<File> {
    /** Constant <code>suffix="file"</code>. */
    public static final String SUFFIX = "file";
    /** Constant <code>type</code>. */
    public static final Class<File> TYPE = File.class;

    /**
     * <p>
     * Getter for the field <code>suffix</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @Override
    public final String getSuffix() {
        return SUFFIX;
    }

    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     * 
     * @return a {@link java.lang.Class} object.
     */
    @Override
    public final Class<File> getType() {
        return TYPE;
    }

    /** {@inheritDoc} */
    @Override
    public final File toObject(final TreeProperties p, final String s) {
        String path = getPath(s);
        File f = new File(path);
        if (f.isAbsolute()) {
            return f;
        } else {
            return new File(p.getPath(), path);
        }
    }

    /**
     * Returns a path path from a property value. Three substitutions are
     * applied:
     * <ul>
     * <li>A "~/" or "~\" at the beginning is replaced with the user's home
     * directory</li>
     * <li>A "$$" anywhere is replaced with a single "$"</li>
     * <li>A "${*}", where * is any string without "}", is replaced by
     *
     * @param s a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     * {@link System#getProperty(String)}</li>
     * </ul>
     */
    public static String getPath(String s) {
        if (s.startsWith("~/")) {
            s = System.getProperty("user.home") + "/" + s.substring(2);
        } else if (s.startsWith("~\\")) {
            s = System.getProperty("user.home") + "\\" + s.substring(2);
        }
        Matcher m = Pattern.compile("\\$\\$|\\$\\{([^\\}]*)\\}").matcher(s);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            if (m.group().equals("$$")) {
                m.appendReplacement(result, Matcher.quoteReplacement("$"));
            } else {
                m.appendReplacement(result, Matcher.quoteReplacement(System.getProperty(m.group(1))));
            }
        }
        m.appendTail(result);
        return result.toString();
    }
}
