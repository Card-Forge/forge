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
package forge.properties;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

/**
 * A collection of name/value pairs with sorted keys and utility methods.
 * 
 * @author Forge
 * @version $Id$
 */
public class Preferences {

    /** The props. */
    private final Properties props;

    /**
     * <p>
     * Constructor for Preferences.
     * </p>
     */
    public Preferences() {
        this.props = new Properties();
    }

    /**
     * <p>
     * Constructor for Preferences.
     * </p>
     * 
     * @param prefs
     *            a {@link forge.properties.Preferences} object.
     */
    public Preferences(final Preferences prefs) {
        this.props = prefs.props;
    }

    /**
     * <p>
     * keys.
     * </p>
     * 
     * @return a {@link java.util.Enumeration} object.
     */
    public final synchronized Enumeration<String> keys() {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        final Set<String> keysEnum = (Set) this.props.keySet();
        final Vector<String> keyList = new Vector<String>();
        keyList.addAll(keysEnum);
        Collections.sort(keyList);
        return keyList.elements();
    }

    /**
     * <p>
     * getInt.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param defaultValue
     *            a int.
     * @return a int.
     */
    public final int getInt(final String name, final int defaultValue) {
        final String value = this.props.getProperty(name);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * <p>
     * getBoolean.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param defaultValue
     *            a boolean.
     * @return a boolean.
     */
    public final boolean getBoolean(final String name, final boolean defaultValue) {
        final String value = this.props.getProperty(name);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    /**
     * <p>
     * getLong.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param defaultValue
     *            a long.
     * @return a long.
     */
    public final long getLong(final String name, final long defaultValue) {
        final String value = this.props.getProperty(name);
        if (value == null) {
            return defaultValue;
        }
        return Long.parseLong(value);
    }

    /**
     * <p>
     * set.
     * </p>
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @param value
     *            a {@link java.lang.Object} object.
     */
    public final void set(final String key, final Object value) {
        this.props.setProperty(key, String.valueOf(value));
    }

    /**
     * <p>
     * get.
     * </p>
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @param value
     *            a {@link java.lang.Object} object.
     * @return a {@link java.lang.String} object.
     */
    public final String get(final String key, final Object value) {
        String string = null;
        if (value != null) {
            string = String.valueOf(value);
        }
        return this.props.getProperty(key, string);
    }

    /**
     * <p>
     * load.
     * </p>
     * 
     * @param stream
     *            a {@link java.io.FileInputStream} object.
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public final void load(final FileInputStream stream) throws IOException {
        this.props.load(stream);
    }

    /**
     * <p>
     * store.
     * </p>
     * 
     * @param stream
     *            a {@link java.io.FileOutputStream} object.
     * @param comments
     *            a {@link java.lang.String} object.
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    public final void store(final FileOutputStream stream, final String comments) throws IOException {
        this.props.store(stream, comments);
    }
}
