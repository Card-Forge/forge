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

/**
 * ForgeProps.java
 *
 * Created on 30.08.2009
 */

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import tree.properties.TreeProperties;
import forge.error.ErrorViewer;

/**
 * The class ForgeProps. Wrapper around TreeProperties to support the uses in
 * forge.
 * 
 * @author Forge
 * @version $Id$
 */
public class ForgeProps {
    /** Constant <code>properties</code>. */
    private static final TreeProperties PROPERTIES;

    static {
        TreeProperties p;
        try {
            p = new TreeProperties("forge.properties");
            p.rethrow();
        } catch (final IOException ex) {
            ErrorViewer.showError(ex);
            p = null;
        }
        PROPERTIES = p;
    }

    /**
     * Returns the tree properties of forge.
     * 
     * @return a {@link tree.properties.TreeProperties} object.
     */
    public static TreeProperties getProperties() {
        return ForgeProps.PROPERTIES;
    }

    /**
     * Returns the string property value, or null if there's no such property.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getProperty(final String key) {
        return ForgeProps.getProperty(key, null);
    }

    /**
     * Returns the string property value, or def if there's no such property.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @param def
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getProperty(final String key, final String def) {
        String result;
        try {
            result = ForgeProps.getProperties().getProperty(key);
            if (result == null) {
                result = def;
            }
        } catch (final Exception ex) {
            result = def;
        }
        return result;
    }

    /**
     * Returns the File property value, or null if there's no such property.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @return a {@link java.io.File} object.
     */
    public static File getFile(final String key) {
        return ForgeProps.getFile(key, null);
    }

    /**
     * Returns the File property value, or def if there's no such property.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @param def
     *            a {@link java.io.File} object.
     * @return a {@link java.io.File} object.
     */
    public static File getFile(final String key, final File def) {
        File result;
        try {
            result = ForgeProps.getProperties().getFile(key);
            if (result == null) {
                result = def;
            }
        } catch (final Exception ex) {
            result = def;
        }
        return result;
    }

    /**
     * Returns the localized version of the specified property. The key is a
     * format string containing "%s", which is replaced with a language code
     * (ISO 639-1, see {@link Locale#getLanguage()}). First, the configured
     * language is used. Second, the locale's code is used. If none of them
     * contains the requested key, "en" is used as the language code.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getLocalized(final String key) {
        return ForgeProps.getLocalized(key, null);
    }

    /**
     * Returns the localized version of the specified property. The key is a
     * format string containing "%s", which is replaced with a language code
     * (ISO 639-1, see {@link Locale#getLanguage()}). First, the configured
     * language is used. Second, the locale's code is used. If none of them
     * contains the requested key, "en" is used as the language code. If even
     * that has no value, the def parameter is returned.
     * 
     * @param key
     *            a {@link java.lang.String} object.
     * @param def
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String getLocalized(final String key, final String def) {
        // the list of languages to look for, in the order to be used
        // the first is the configured language
        // the second is the default locale's language code
        final String[] languages = { ForgeProps.getProperty(NewConstants.Lang.LANGUAGE),
                Locale.getDefault().getLanguage(), "en" };
        try {
            for (final String lang : languages) {
                // could be if a property does not exist
                // just skip it, and try the next
                if (lang == null) {
                    continue;
                }
                final String result = ForgeProps.getProperty(String.format(key, lang));
                if (result != null) {
                    return result;
                }
            }
            // exceptions are skipped here; also the error viewer uses this, and
            // reporting exceptions may result
            // in a more fatal error (stack overflow)
        } catch (final Exception ex) {
        }
        // if no property was found, or an error occurred, return the default
        // value
        return def;
    }
}
