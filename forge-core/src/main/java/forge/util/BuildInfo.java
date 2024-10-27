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

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Provides access to information about the current version and build ID.
 */
public class BuildInfo {
    private static Date timestamp = null;
    // disable instantiation
    private BuildInfo() { }

    /**
     * Get the current version of Forge.
     * 
     * @return a String representing the version specifier, or "GIT" if unknown.
     */
    public static final String getVersionString() {
        String version = BuildInfo.class.getPackage().getImplementationVersion();
        if (StringUtils.isEmpty(version)) {
            return "GIT";
        }
        return version;
    }

    public static boolean isDevelopmentVersion() {
        String forgeVersion = getVersionString();
        return StringUtils.containsIgnoreCase(forgeVersion, "git") ||
                StringUtils.containsIgnoreCase(forgeVersion, "snapshot");
    }

    public static Date getTimestamp() {
        if (timestamp != null)
            return timestamp;
        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String b = Files.readString(
               Paths.get(BuildInfo.class.getClassLoader().getResource("build.txt").toURI()),
                   Charset.defaultCharset());
            timestamp = simpleDateFormat.parse(b);
        } catch (Exception ignored) {}
        return timestamp;
    }

    public static boolean verifyTimestamp(Date updateTimestamp) {
        if (updateTimestamp == null)
            return false;
        if (getTimestamp() == null)
            return false;
        System.err.println("Update Timestamp: " + updateTimestamp + "\nBuild Timestamp: " + getTimestamp());
        return updateTimestamp.after(getTimestamp());
    }
    public static String getUserAgent() {
        return "Forge/" + getVersionString();
    }
}
