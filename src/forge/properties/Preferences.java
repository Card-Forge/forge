package forge.properties;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;


/**
 * A collection of name/value pairs with sorted keys and utility methods.
 *
 * @author Forge
 * @version $Id$
 */
public class Preferences {
    protected Properties props;

    /**
     * <p>Constructor for Preferences.</p>
     */
    public Preferences() {
        props = new Properties();
    }

    /**
     * <p>Constructor for Preferences.</p>
     *
     * @param prefs a {@link forge.properties.Preferences} object.
     */
    public Preferences(Preferences prefs) {
        props = prefs.props;
    }

    /**
     * <p>keys.</p>
     *
     * @return a {@link java.util.Enumeration} object.
     */
    public synchronized Enumeration<String> keys() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        Set<String> keysEnum = (Set) props.keySet();
        Vector<String> keyList = new Vector<String>();
        keyList.addAll(keysEnum);
        Collections.sort(keyList);
        return keyList.elements();
    }

    /**
     * <p>getInt.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a int.
     * @return a int.
     */
    public int getInt(String name, int defaultValue) {
        String value = props.getProperty(name);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * <p>getBoolean.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a boolean.
     * @return a boolean.
     */
    public boolean getBoolean(String name, boolean defaultValue) {
        String value = props.getProperty(name);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }

    /**
     * <p>getLong.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param defaultValue a long.
     * @return a long.
     */
    public long getLong(String name, long defaultValue) {
        String value = props.getProperty(name);
        if (value == null) return defaultValue;
        return Long.parseLong(value);
    }

    /**
     * <p>set.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     */
    public void set(String key, Object value) {
        props.setProperty(key, String.valueOf(value));
    }

    /**
     * <p>get.</p>
     *
     * @param key a {@link java.lang.String} object.
     * @param value a {@link java.lang.Object} object.
     * @return a {@link java.lang.String} object.
     */
    public String get(String key, Object value) {
        String string = null;
        if (value != null) string = String.valueOf(value);
        return props.getProperty(key, string);
    }

    /**
     * <p>load.</p>
     *
     * @param stream a {@link java.io.FileInputStream} object.
     * @throws java.io.IOException if any.
     */
    public void load(FileInputStream stream) throws IOException {
        props.load(stream);
    }

    /**
     * <p>store.</p>
     *
     * @param stream a {@link java.io.FileOutputStream} object.
     * @param comments a {@link java.lang.String} object.
     * @throws java.io.IOException if any.
     */
    public void store(FileOutputStream stream, String comments) throws IOException {
        props.store(stream, comments);
    }
}
