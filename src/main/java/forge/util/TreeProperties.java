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
package forge.util;

/**
 *  TreeProperties.java
 *
 * Created on 19.08.2009
 */

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Properties;
import java.util.Set;


/**
 * The class TreeProperties. This class allows for a tree-like structure of
 * properties-files. This class lays some restrictions on the keys used in
 * properties-files:
 * <ul>
 * <li>The use of "--" is forbidden</li>
 * <li>The suffixes "--properties" and "--transparent-properties" are reserved
 * for specifying additional properties-files in the tree (relative paths are
 * relative to the properties-file where they are referenced)</li>
 * <li>Other suffixes are used by {@link PropertyType}s. PropertyTypes are
 * registered or unregistered using {@link #addType(PropertyType)} or
 * {@link #removeType(PropertyType)}.</li>
 * </ul>
 * Take a look at these files:
 * <p/>
 * 
 * <pre>
 * #root.properties
 * title=directions
 * icons--properties=img/icons.properties
 * 
 * #img/icons.properties
 * left--file=left.jpg
 * right--file=right.jpg
 * up--file=up.jpg
 * down--file=down.jpg
 * #note that the path does not contain &quot;img/&quot;
 * size--transparent-properties=size.properties
 * 
 * #img/size.properties
 * width=20
 * height=20
 * </pre>
 * <p/>
 * These properties are retrieved with
 * <p/>
 * 
 * <pre>
 * getProperty(&quot;title&quot;) //directions
 * getFile(&quot;icons/left&quot;) //img/left.jpg
 * getFile(&quot;icons/right&quot;) //img/right.jpg
 * getFile(&quot;icons/up&quot;) //img/up.jpg
 * getFile(&quot;icons/down&quot;) //img/down.jpg
 * getProperty(&quot;icons/width&quot;) //20
 * getProperty(&quot;icons/height&quot;) //20
 * </pre>
 * <p/>
 * As you can see, a properties file included with "--transparent-properties"
 * hides its existence from the user. A file included with "--properties" is not
 * hidden. The child properties are accessible as if their keys were prepended
 * with the parent key, separated by a slash.
 * <p/>
 * Note that --file, --properties and --transparent-properties entries will be
 * relative to the included file, even if the properties file is transparent.
 * <p/>
 * Also, the TreeProperties can be retrieved:
 * <p/>
 * 
 * <pre>
 * getChildProperties(&quot;icons&quot;)
 * getTransparentProperties(&quot;icons/size&quot;)
 * </pre>
 * <p/>
 * <p/>
 * TODO add edit & save support
 * 
 * @author Clemens Koza
 * @version V0.0 19.08.2009
 * @see Properties
 */ 
public class TreeProperties /* implements Iterable<PropertyElement> */{
    /** Constant <code>suffixes</code>. */
    private static final Map<String, PropertyType<?>> SUFFIXES;
    /** Constant <code>types</code>. */
    private static final Map<Class<?>, PropertyType<?>> TYPES;

    /** Constant <code>transparent="transparent-properties"</code>. */
    private static final String TRANSPARENT = "transparent-properties";
    /** Constant <code>child="properties"</code>. */
    private static final String CHILD = "properties";

    static {
        TYPES = new HashMap<Class<?>, PropertyType<?>>();
        SUFFIXES = new HashMap<String, PropertyType<?>>();
        PropertyType<?>[] pt = {new FileType()};
        for (PropertyType<?> type : pt) {
            addType(type);
        }
    }

    private File path;
    private Map<Class<?>, PropertyType<?>> instanceTypes;
    private Map<String, PropertyType<?>> instanceSuffixes;
    private HashMap<String, Object> properties;
    private List<Exception> exceptions;

    /**
     * <p>
     * addType.
     * </p>
     * 
     * @param type
     *            a {@link tree.properties.PropertyType} object.
     */
    public static void addType(final PropertyType<?> type) {
        TYPES.put(type.getType(), type);
        SUFFIXES.put(type.getSuffix(), type);
    }

    /**
     * <p>
     * removeType.
     * </p>
     * 
     * @param type
     *            a {@link tree.properties.PropertyType} object.
     */
    public static void removeType(final PropertyType<?> type) {
        TYPES.remove(type.getType());
        SUFFIXES.remove(type.getSuffix());
    }

    /**
     * Delegate to {@link #TreeProperties(File)} with a new.
     *
     * @param f a {@link java.lang.String} object.
     * @throws IOException Signals that an I/O exception has occurred.
     * {@link File#File(String)}.
     */
    public TreeProperties(final String f) throws IOException {
        this(new File(f));
    }

    /**
     * Delegate to {@link #TreeProperties(File)} with a new.
     *
     * @param parent a {@link java.io.File} object.
     * @param f a {@link java.lang.String} object.
     * @throws IOException Signals that an I/O exception has occurred.
     * {@link File#File(File, String)}.
     */
    public TreeProperties(final File parent, final String f) throws IOException {
        this(new File(parent, f));
    }

    /**
     * The constructor is forgiving in the way that Exceptions are not directly
     * forwarded. The only fatal exception is if the parameter is null or not
     * found. Instead, the rest of the properties are processed, so that the
     * erroneous properties are the only ones not present in this
     * TreeProperties. Afterwards, the exceptions can be accessed.
     *
     * @param f a {@link java.io.File} object.
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public TreeProperties(final File f) throws IOException {
        if (f == null) {
            throw new FileNotFoundException("null");
        }
        this.path = f.getParentFile();
        instanceTypes = new HashMap<Class<?>, PropertyType<?>>(TYPES);
        instanceSuffixes = new HashMap<String, PropertyType<?>>(SUFFIXES);
        Properties p = new Properties();

        // BufferedReader r = new BufferedReader(new FileReader(f));
        // p.load(r);
        // r.close();
        BufferedInputStream is = new BufferedInputStream(new FileInputStream(f));
        p.load(is);
        is.close();

        Set<Entry<Object, Object>> entries = p.entrySet();
        properties = new HashMap<String, Object>();
        List<Exception> exceptions = new ArrayList<Exception>();
        this.exceptions = unmodifiableList(exceptions);
        for (Entry<Object, Object> entry : entries) {
            String key = (String) entry.getKey();
            String value = (String) entry.getValue();
            try {
                String[] parts = key.split("--", 2);
                Object result;
                if (parts.length == 1) {
                    // Regular
                    result = value;
                } else {
                    // suffix
                    if (parts[1].equals(TRANSPARENT) || parts[1].equals(CHILD)) {
                        TreeProperties child = new TreeProperties(path, FileType.getPath(value));
                        exceptions.addAll(child.exceptions);
                        result = child;
                    } else {
                        PropertyType<?> t = instanceSuffixes.get(parts[1]);
                        if (t == null) {
                            throw new IllegalArgumentException("No content type: " + parts[1]);
                        }
                        result = t.toObject(this, value);
                    }
                }
                properties.put(key, result);
            } catch (Exception ex) {
                exceptions.add(new Exception(format("File '%s', Property '%s':%n    %s", f, key, ex.getMessage()), ex
                        .getCause()));
            }
        }
    }

    /**
     * Returns the exceptions that were thrown while creating the tree
     * properties.
     * 
     * @return a {@link java.util.List} object.
     */
    public final List<Exception> getExceptions() {
        return exceptions;
    }

    /**
     * If exceptions occurred during construction, this method throws an
     * IOException that combines the messages of those exceptions.
     *
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public final void rethrow() throws IOException {
        if (exceptions.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("The following exceptions occurred:");
        for (Exception ex : exceptions) {
            sb.append("\n");
            sb.append(ex.getMessage());
        }
        throw new IOException(sb.toString());
    }

    /**
     * Returns the parent directory of this TreeProperties.
     * 
     * @return a {@link java.io.File} object.
     */
    public final File getPath() {
        return path;
    }

    /**
     * Checks if the key is valid for a query and throws an
     * {@link IllegalArgumentException} if not. Slashes are allowed in this
     * method, but suffixes are not
     * 
     * @param key
     *            a {@link java.lang.String} object.
     */
    private void checkQueryKey(final String key) {
        if (key.contains("--")) {
            throw new IllegalArgumentException("Invalid key: " + key);
        }
    }

    /**
     * Retrieves the string property for the given key.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public final String getProperty(final String key) {
        return getProperty(key, String.class);
    }

    /**
     * Convenience for {@code getProperty(key, File.class)}.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @return a {@link java.io.File} object.
     */
    public final File getFile(final String key) {
        return getProperty(key, File.class);
    }

    /**
     * Retrieves the child properties for the given key. Transparent properties
     * can't be retrieved this way.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @return a {@link forge.util.TreeProperties} object.
     */
    public final TreeProperties getChildProperties(final String key) {
        return (TreeProperties) getProperty(key, "--" + CHILD, true);
    }

    /**
     * Retrieves the child properties for the given key.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @return a {@link forge.util.TreeProperties} object.
     */
    public final TreeProperties getTransparentProperties(final String key) {
        return (TreeProperties) getProperty(key, "--" + TRANSPARENT, true);
    }

    /**
     * Returns a property of the given type. This does not work to retrieve tree
     * properties.
     *
     * @param <T> a T object.
     * @param key a {@link java.lang.String} object.
     * @param cls a {@link java.lang.Class} object.
     * @return a T object.
     */
    @SuppressWarnings("unchecked")
    public final <T> T getProperty(final String key, final Class<T> cls) {
        String suffix;
        if (cls == String.class) {
            suffix = "";
        } else {
            PropertyType<?> t = instanceTypes.get(cls);
            suffix = "--" + t.getSuffix();
        }
        return (T) getProperty(key, suffix, true);
    }

    /**
     * <p>
     * getProperty.
     * </p>
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @param suffix
     *            a {@link java.lang.String} object.
     * @param top
     *            a boolean.
     * @return a {@link java.lang.Object} object.
     */
    private Object getProperty(final String key, final String suffix, final boolean top) {
        checkQueryKey(key);
        // first, try the key in the current file, as if there were no slash
        // No subpath - either directly in the properties...
        Object result = properties.get(key + suffix);
        if (result != null) {
            return result;
        }

        // ...or in a transparent properties

        // look for all --transparent-properties
        for (Entry<String, Object> entry : properties.entrySet()) {
            if (entry.getKey().endsWith("--" + TRANSPARENT)) {
                TreeProperties p = (TreeProperties) entry.getValue();
                result = p.getProperty(key, suffix, false);
                if (result != null) {
                    return result;
                }
            }
        }

        // if there is no immediate containment, try the children
        // try every combination
        // for a/b/c, there could be a child "a/b--properties" that contains "c"
        // or "a--properties" with "b/c"
        int index = -1;
        while ((index = key.indexOf('/', index + 1)) != -1) {
            String first = key.substring(0, index), second = key.substring(index + 1);

            TreeProperties p = (TreeProperties) getProperty(first, "--" + CHILD, false);
            if (p == null) {
                continue;
            }
            result = p.getProperty(second, suffix, false);
            if (result != null) {
                return result;
            }
        }
        if (top) {
            Exception ex = new Exception("TreeProperties returns null for " + key + suffix);
            // ex.printStackTrace();
            System.err.println(ex);
        }
        return null;
    }

    public interface PropertyType<T> {
        /**
         * The suffix, not including "--", that identifies this content type.
         * 
         * @return a {@link java.lang.String} object.
         */
        String getSuffix();

        /**
         * The class that identifies this content type.
         * 
         * @return a {@link java.lang.Class} object.
         */
        Class<T> getType();

        /**
         * Returns an object for the specified value, in the context of a
         * TreeProperties.
         * 
         * @param p
         *            a {@link forge.util.TreeProperties} object.
         * @param s
         *            a {@link java.lang.String} object.
         * @return a T object.
         */
        T toObject(TreeProperties p, String s);
    }    
    
    public static class FileType implements PropertyType<File> {
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
}
