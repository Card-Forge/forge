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
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Determines the user data and cache dirs, first looking at the specified file for overrides
 * then falling back to platform-specific defaults.  Resulting dir strings are guaranteed to end in a slash
 * so they can be easily appended with further path elements.
 */
public class ForgeProfileProperties {
    public final String userDir;
    public final String cacheDir;
    public final String cardPicsDir;
    
    private final String _USER_DIR_KEY      = "userDir";
    private final String _CACHE_DIR_KEY     = "cacheDir";
    private final String _CARD_PICS_DIR_KEY = "cardPicsDir";

    public ForgeProfileProperties(String filename) {
        Properties props = new Properties();
        File propFile = new File(filename);
        try {
            if (propFile.canRead()) {
                props.load(new FileInputStream(propFile));
            }
        } catch (IOException e) {
            // ignore
        }

        Pair<String, String> defaults = _getDefaultDirs();
        
        String propUserDir = props.getProperty(_USER_DIR_KEY, defaults.getLeft());
        String propCacheDir = props.getProperty(_CACHE_DIR_KEY, defaults.getRight());
        
        // use defaults if the dirs are "defined" as empty strings in the properties file
        propUserDir  = StringUtils.isEmpty(propUserDir)  ? defaults.getLeft()  : propUserDir.trim();
        propCacheDir = StringUtils.isEmpty(propCacheDir) ? defaults.getRight() : propCacheDir.trim();
        
        propUserDir  += propUserDir.endsWith("/")  || propUserDir.endsWith(File.pathSeparator)  ? "" : "/";
        propCacheDir += propCacheDir.endsWith("/") || propCacheDir.endsWith(File.pathSeparator) ? "" : "/";
        
        String propCardPicsDir = props.getProperty(_CARD_PICS_DIR_KEY, propCacheDir + "pics/cards/");
        propCardPicsDir += propCardPicsDir.endsWith("/") || propCardPicsDir.endsWith(File.pathSeparator) ? "" : "/";
        
        userDir     = propUserDir;
        cacheDir    = propCacheDir;
        cardPicsDir = propCardPicsDir;
    }
    
    // returns a pair <userDir, cacheDir>
    private Pair<String, String> _getDefaultDirs() {
        String osName = System.getProperty("os.name");
        String homeDir = System.getProperty("user.home");

        if (StringUtils.isEmpty(osName) || StringUtils.isEmpty(homeDir)) {
            throw new RuntimeException("cannot determine OS and user home directory");
        }
        
        String fallbackDataDir = String.format("%s/.forge", homeDir);
        
        if (StringUtils.containsIgnoreCase("windows", osName)) {
            // the split between appdata and localappdata on windows is relatively recent.  If
            // localappdata is not defined, use appdata for both.
            String appRoot = System.getenv().get("APPDATA");
            if (StringUtils.isEmpty(appRoot)) {
                appRoot = fallbackDataDir;
            }
            String cacheRoot = System.getenv().get("LOCALAPPDATA");
            if (StringUtils.isEmpty(cacheRoot)) {
                cacheRoot = appRoot;
            }
            // just use '/' everywhere instead of file.separator.  it always works
            return Pair.of(String.format("%s/Forge", appRoot),
                           String.format("%s/Forge/Cache", cacheRoot));
        } else if (StringUtils.containsIgnoreCase("mac os x", osName)) {
            return Pair.of(String.format("%s/Library/Application Support/Forge", homeDir),
                           String.format("%s/Library/Caches/Forge", homeDir));
        }

        // Linux and everything else
        return Pair.of(fallbackDataDir, String.format("%s/.cache/forge", homeDir));
    }
}
