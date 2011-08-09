package forge.properties;
/**
 * ForgeProps.java
 *
 * Created on 30.08.2009
 */


import forge.error.ErrorViewer;
import treeProperties.TreeProperties;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static java.lang.String.format;


/**
 * The class ForgeProps. Wrapper around TreeProperties to support the uses in forge.
 *
 * @author Forge
 * @version $Id$
 */
public class ForgeProps {
    /** Constant <code>properties</code> */
    private static final TreeProperties properties;

    static {
        TreeProperties p;
        try {
            p = new TreeProperties("forge.properties");
            p.rethrow();
        } catch (IOException ex) {
            ErrorViewer.showError(ex);
            p = null;
        }
        properties = p;
    }

    /**
     * Returns the tree properties of forge
     *
     * @return a {@link treeProperties.TreeProperties} object.
     */
    public static TreeProperties getProperties() {
        return properties;
    }

    /**
     * Returns the string property value, or null if there's no such property
     *
     * @param key a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    /**
     * Returns the string property value, or def if there's no such property
     *
     * @param key a {@link java.lang.String} object.
     * @param def a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getProperty(String key, String def) {
        String result;
        try {
            result = getProperties().getProperty(key);
            if (result == null) result = def;
        } catch (Exception ex) {
            result = def;
        }
        return result;
    }

    /**
     * Returns the File property value, or null if there's no such property
     *
     * @param key a {@link java.lang.String} object.
     * @return a {@link java.io.File} object.
     */
    public static File getFile(String key) {
        return getFile(key, null);
    }

    /**
     * Returns the File property value, or def if there's no such property
     *
     * @param key a {@link java.lang.String} object.
     * @param def a {@link java.io.File} object.
     * @return a {@link java.io.File} object.
     */
    public static File getFile(String key, File def) {
        File result;
        try {
            result = getProperties().getFile(key);
            if (result == null) result = def;
        } catch (Exception ex) {
            result = def;
        }
        return result;
    }

    /**
     * Returns the localized version of the specified property. The key is a format string containing "%s", which
     * is replaced with a language code (ISO 639-1, see {@link Locale#getLanguage()}). First, the configured
     * language is used. Second, the locale's code is used. If none of them contains the requested key, "en" is
     * used as the language code.
     *
     * @param key a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getLocalized(String key) {
        return getLocalized(key, null);
    }

    /**
     * Returns the localized version of the specified property. The key is a format string containing "%s", which
     * is replaced with a language code (ISO 639-1, see {@link Locale#getLanguage()}). First, the configured
     * language is used. Second, the locale's code is used. If none of them contains the requested key, "en" is
     * used as the language code. If even that has no value, the def parameter is returned.
     *
     * @param key a {@link java.lang.String} object.
     * @param def a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getLocalized(String key, String def) {
        //the list of languages to look for, in the order to be used
        //the first is the configured language
        //the second is the default locale's language code
        String[] languages = {getProperty(NewConstants.LANG.LANGUAGE), Locale.getDefault().getLanguage(), "en"};
        try {
            for (String lang : languages) {
                //could be if a property does not exist
                //just skip it, and try the next
                if (lang == null) continue;
                String result = getProperty(format(key, lang));
                if (result != null) return result;
            }
            //exceptions are skipped here; also the error viewer uses this, and reporting exceptions may result
            //in a more fatal error (stack overflow)
        } catch (Exception ex) {
        }
        //if no property was found, or an error occurred, return the default value
        return def;
    }
}
