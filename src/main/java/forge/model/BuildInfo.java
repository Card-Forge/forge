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
package forge.model;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Provides access to information about the current version and build ID.
 */
public class BuildInfo {
    /** Exception-free means of getting the ASCII Charset. */
    public static final Charset US_ASCII_CHARSET = Charset.forName("US-ASCII");

    /** Convenience for file.separator. */
    /*
     * private static final String FILE_SEP =
     * System.getProperty("file.separator");
     */

    /** Convenience for path.separator. */
    /*
     * private static final String PATH_SEP =
     * System.getProperty("path.separator");
     */

    /*
     * private static final Pattern FORGE_JAR_REGEX_2G = // NOPMD by Braids on
     * 8/12/11 10:18 AM Pattern.compile("^(.*" + Pattern.quote(FILE_SEP) + ")?"
     * + Pattern.quote("forge-") + "([^" + Pattern.quote(FILE_SEP) +
     * Pattern.quote(PATH_SEP) + "]*)" + Pattern.quote("-with-dependencies.jar")
     * + "$", Pattern.CASE_INSENSITIVE);
     */

    private transient String pathToForgeJar;

    /**
     * Construct a standard BuildInfo object.
     * 
     * Package access is intentional for unit testing.
     * 
     * @see forge.model.FModel#getBuildInfo()
     */
    BuildInfo() {
        // empty
    }

    /**
     * Unit-testable constructor which allows a specific jar file to be set.
     * 
     * Dependency injection! Relax, this won't hurt a bit.
     * 
     * @param pathToMockJarFile
     *            where to find the mock Forge jar
     */
    public BuildInfo(final String pathToMockJarFile) {
        this.pathToForgeJar = pathToMockJarFile;
    }

    /**
     * Get the current build ID for Forge.
     * 
     * @return a String representing the build identifier, or null if we could
     *         not determine the value.
     */
    public final String getBuildID() {
        String manifestResult = "0000";
        String[] manifestResultArray;
        String result = "0000";

        final String version = this.getVersion();
        manifestResultArray = version.split("r");
        manifestResult = manifestResultArray[manifestResultArray.length - 1];

        if (manifestResult == null) {
            result = "0000";
        } else {
            result = manifestResult;
        }

        return result;
    }

    /**
     * Get the current version of Forge.
     * 
     * @return a String representing the version specifier, or "SVN" if unknown.
     */
    public final String getVersion() {
        String manifestResult;
        String result;

        manifestResult = BuildInfo.class.getPackage().getImplementationVersion();

        if (manifestResult == null) {
            result = "SVN";
        } else {
            result = manifestResult;
        }

        // The code above will always return the SVN rev as "r15897" even with builds
        // that are using a later revision for some unknown reason. This happens on
        // Chris' dev system but not on Dave's dev system. We should note that many
        // users are refering to the snapshot build by only using the SVS rev number
        // and the code below may convince them to instead use the date of the archive.
        // Note that the number appears to change at some point, now removing:
        // r16084
        if (result.endsWith("-r16084")) {
            result = result.replace("-16084", "");
        }
        return result;
    }

    /**
     * Fetch an attribute from the Forge main jar's manifest.
     * 
     * @param manifestAttrName
     *            the name of the attribute you want from the manifest
     * @return the attribute's value, which may be empty or null
     * @throws IOException
     *             if a (unique) Forge jar could not be found
     */
    protected final String getManifestAttribute(final String manifestAttrName) throws IOException {
        String result = null;
        JarFile jar = null;
        InputStream manifestStream = null;

        try {
            if (this.pathToForgeJar == null) {
                // We're definitely not unit testing. Try to load from the
                // currently running jar.

                manifestStream = ClassLoader.getSystemResourceAsStream("META-INF/MANIFEST.MF");
                final Manifest manifest = new Manifest(manifestStream);

                result = this.getMainManifestAttribute(manifest, manifestAttrName);
            }

            /*
             * if (result == null && pathToForgeJar == null) {
             * 
             * // Try to find a unique Forge jar in the class path.
             * 
             * final String classPath = System.getProperty("java.class.path");
             * final String[] paths = classPath.split(PATH_SEP);
             * 
             * for (String path : paths) { final Matcher matcher =
             * FORGE_JAR_REGEX_2G.matcher(path);
             * 
             * if (matcher.matches()) { if (pathToForgeJar == null) {
             * pathToForgeJar = path; } else { // Error: we found more than one.
             * pathToForgeJar = null; // NOPMD by Braids on 8/12/11 10:21 AM
             * 
             * throw new MultipleForgeJarsFoundError( "Classpath = " +
             * System.getProperty("java.class.path")); } } } }
             */

            if ((result == null) && (this.pathToForgeJar == null)) {
                throw new FileNotFoundException(
                        "There is nothing matching forge-*-with-dependencies.jar in the class path.");
            }

            if (result == null) {
                jar = new JarFile(this.pathToForgeJar);
                final Manifest manifest = jar.getManifest();

                if (manifest == null) {
                    throw new IOException("Forge jar at <<" + this.pathToForgeJar + ">> has no manifest.");
                }

                result = this.getMainManifestAttribute(manifest, manifestAttrName);
            }
        } finally {
            try {
                manifestStream.close();
            } catch (final Throwable ignored) {
                // 10:21 AM
                // ignored
            }

            try {
                jar.close();
            } catch (final Throwable ignored) {
                // 10:21 AM
                // ignored
            }
        }

        return result;
    }

    /**
     * Convience method for fetching an attribute from the main section of a
     * jar's manifest.
     * 
     * @param manifest
     *            the manifest that provides attributes
     * @param manifestAttrName
     *            the name of the attribute to fetch
     * @return the value of the attribute, or null if not set
     */
    protected final String getMainManifestAttribute(final Manifest manifest, final String manifestAttrName) {
        final Attributes atts = manifest.getMainAttributes();
        return atts.getValue(manifestAttrName);
    }

    /**
     * Generate a user-friendly string describing the version and build ID.
     * 
     * This replaces the old system property program/version.
     * 
     * @return a user-friendly string describing the version and build ID
     */
    public final String toPrettyString() {
        final String rawVersion = this.getVersion();
        // final String rawBuildID = getBuildID();

        String version;
        if (rawVersion == null) {
            version = "Unknown";
        } else {
            version = rawVersion;
        }

        /*
         * String buildID; if (rawBuildID == null) { buildID = "Unknown"; } else
         * { buildID = rawBuildID; }
         */

        return "Forge version " + version; // ", build ID " + buildID;
    }

}
