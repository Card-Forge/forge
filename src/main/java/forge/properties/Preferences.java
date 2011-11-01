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
    private Properties props;

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
