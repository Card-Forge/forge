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
package forge.gamemodes.net;

import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.PreferencesStore;

/**
 * Network debugging and logging preferences.
 * Stores settings for bandwidth tracking and debug logging.
 */
public class NetworkDebugPreferences extends PreferencesStore<NetworkDebugPreferences.NDPref> {

    /**
     * Preference identifiers and their default values.
     */
    public enum NDPref implements PreferencesStore.IPref {
        // Bandwidth logging
        BANDWIDTH_LOGGING_ENABLED("true"),

        // Debug logger enable/disable
        DEBUG_LOGGER_ENABLED("true"),

        // Log levels
        CONSOLE_LOG_LEVEL("INFO"),
        FILE_LOG_LEVEL("DEBUG"),

        // Log management
        MAX_LOG_FILES("20"),
        LOG_CLEANUP_ENABLED("true");

        private final String strDefaultVal;

        NDPref(final String s0) {
            this.strDefaultVal = s0;
        }

        @Override
        public String getDefault() {
            return strDefaultVal;
        }
    }

    /** Instantiates a NetworkDebugPreferences object. */
    public NetworkDebugPreferences() {
        super(ForgeConstants.USER_PREFS_DIR + "network.preferences", NDPref.class);
    }

    @Override
    protected NDPref[] getEnumValues() {
        return NDPref.values();
    }

    @Override
    protected NDPref valueOf(final String name) {
        try {
            return NDPref.valueOf(name);
        }
        catch (final Exception e) {
            return null;
        }
    }

    @Override
    protected String getPrefDefault(final NDPref key) {
        return key.getDefault();
    }

}
