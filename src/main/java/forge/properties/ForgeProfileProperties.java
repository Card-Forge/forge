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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import forge.util.FileSection;

/**
 * Determines the user data and cache dirs, first looking at the specified file for overrides
 * then falling back to platform-specific defaults.  Resulting dir strings are guaranteed to end in a slash
 * so they can be easily appended with further path elements.
 */
public class ForgeProfileProperties {
    public final String userDir;
    public final String cacheDir;
    public final String cardPicsDir;
    public final Map<String, String> cardPicsSubDir;
    
    private static final String _USER_DIR_KEY      = "userDir";
    private static final String _CACHE_DIR_KEY     = "cacheDir";
    private static final String _CARD_PICS_DIR_KEY = "cardPicsDir";
    private static final String _CARD_PICS_SUB_DIRS_KEY = "cardPicsSubDirs";

    public ForgeProfileProperties(String filename) {
        Properties props = new Properties();
        File propFile = new File(filename);
        try {
            if (propFile.canRead()) {
                props.load(new FileInputStream(propFile));
            }
        } catch (IOException e) {
            System.err.println("error while reading from profile properties file: " + filename);
        }

        Pair<String, String> defaults = _getDefaultDirs();
        userDir     = _getDir(props, _USER_DIR_KEY,      defaults.getLeft());
        cacheDir    = _getDir(props, _CACHE_DIR_KEY,     defaults.getRight());
        cardPicsDir = _getDir(props, _CARD_PICS_DIR_KEY, cacheDir + "pics/cards/");
        cardPicsSubDir = _getMap(props, _CARD_PICS_SUB_DIRS_KEY);
    }


    private Map<String,String> _getMap(Properties props, String propertyKey) {
        String strMap = props.getProperty(propertyKey, "").trim();
        return FileSection.parseToMap(strMap, "->", "|");
    }

    private static String _getDir(Properties props, String propertyKey, String defaultVal) {
        String retDir = props.getProperty(propertyKey, defaultVal).trim();
        if (retDir.isEmpty()) {
            // use default if dir is "defined" as an empty string in the properties file
            retDir = defaultVal;
        }
        
        // canonicalize
        retDir = new File(retDir).getAbsolutePath();
        
        // ensure path ends in a slash
        if (File.separatorChar == retDir.charAt(retDir.length() - 1)) {
            return retDir;
        }
        return retDir + File.separatorChar;
    }
    
    // returns a pair <userDir, cacheDir>
    private static Pair<String, String> _getDefaultDirs() {
        String osName = System.getProperty("os.name");
        String homeDir = System.getProperty("user.home");

        if (StringUtils.isEmpty(osName) || StringUtils.isEmpty(homeDir)) {
            throw new RuntimeException("cannot determine OS and user home directory");
        }
        
        String fallbackDataDir = String.format("%s/.forge", homeDir);
        
        if (StringUtils.containsIgnoreCase(osName, "windows")) {
            // the split between appdata and localappdata on windows is relatively recent.  If
            // localappdata is not defined, use appdata for both.  and if appdata is not defined,
            // fall back to a linux-style dot dir in the home directory
            String appRoot = System.getenv().get("APPDATA");
            if (StringUtils.isEmpty(appRoot)) {
                appRoot = fallbackDataDir;
            }
            String cacheRoot = System.getenv().get("LOCALAPPDATA");
            if (StringUtils.isEmpty(cacheRoot)) {
                cacheRoot = appRoot;
            }
            // just use '/' everywhere instead of file.separator.  it always works
            // the cache dir is Forge/Cache instead of just Forge since appRoot and cacheRoot might be the
            // same directory on windows and we need to distinguish them.
            return Pair.of(String.format("%s/Forge", appRoot),
                           String.format("%s/Forge/Cache", cacheRoot));
        } else if (StringUtils.containsIgnoreCase(osName, "mac os x")) {
            return Pair.of(String.format("%s/Library/Application Support/Forge", homeDir),
                           String.format("%s/Library/Caches/Forge", homeDir));
        }

        // Linux and everything else
        return Pair.of(fallbackDataDir, String.format("%s/.cache/forge", homeDir));
    }
}
