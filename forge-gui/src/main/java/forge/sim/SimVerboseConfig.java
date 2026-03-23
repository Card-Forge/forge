/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2025  Forge Team
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
package forge.sim;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import forge.localinstance.properties.ForgeProfileProperties;

/**
 * Categories for {@code forge.exe sim -v} extra logging. Loaded from the user file
 * {@code <userDir>/sim/sim-verbose.properties} when present; otherwise defaults apply.
 * See {@link forge.localinstance.properties.ForgeConstants#SIM_VERBOSE_CONFIG_EXAMPLE}.
 */
public final class SimVerboseConfig {

    /** Library to hand (draw step, mulligan, etc.). */
    public static final String DRAWS = "draws";

    /** At each turn start, log the active player's hand. */
    public static final String BEGINNING_CARDS_IN_HAND = "beginning_cards_in_hand";

    private static final Map<String, Boolean> DEFAULTS;
    static {
        Map<String, Boolean> d = new LinkedHashMap<>();
        d.put(DRAWS, Boolean.TRUE);
        // On by default with -v when no config file; set beginning_cards_in_hand=false to disable.
        d.put(BEGINNING_CARDS_IN_HAND, Boolean.TRUE);
        DEFAULTS = Collections.unmodifiableMap(d);
    }

    private final Map<String, Boolean> categories;

    private SimVerboseConfig(final Map<String, Boolean> categories0) {
        this.categories = Collections.unmodifiableMap(categories0);
    }

    /**
     * @param category case-insensitive key from the properties file (e.g. {@link #DRAWS})
     */
    public boolean isEnabled(final String category) {
        if (category == null) {
            return false;
        }
        final String key = category.trim().toLowerCase(Locale.ROOT);
        return Boolean.TRUE.equals(categories.get(key));
    }

    public boolean anyEnabled() {
        for (final Boolean b : categories.values()) {
            if (Boolean.TRUE.equals(b)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads user config and merges with defaults. Missing file uses defaults only (draws on).
     * Looks for {@link #getUserConfigFile()} first, then {@code <workingDir>/sim/sim-verbose.properties},
     * then {@code <workingDir>/sim-verbose.properties}.
     */
    public static SimVerboseConfig load() {
        final Map<String, Boolean> map = new LinkedHashMap<>(DEFAULTS);
        final File userFile = resolveConfigFile();
        if (userFile.isFile()) {
            final Properties p = new Properties();
            try (InputStream in = Files.newInputStream(userFile.toPath())) {
                p.load(in);
            } catch (final IOException e) {
                System.err.println("Could not read sim verbose config " + userFile + ": " + e.getMessage());
            }
            for (final String name : p.stringPropertyNames()) {
                String key = name.trim().toLowerCase(Locale.ROOT);
                if (key.isEmpty()) {
                    continue;
                }
                if ("begining_cards_in_hand".equals(key)) {
                    key = BEGINNING_CARDS_IN_HAND;
                }
                map.put(key, parseBool(p.getProperty(name), false));
            }
        }
        return new SimVerboseConfig(map);
    }

    /** Primary location under Forge user data (see Forge profile / install docs). */
    public static File getUserConfigFile() {
        final String userDir = ForgeProfileProperties.getUserDir();
        return new File(userDir + "sim" + File.separator + "sim-verbose.properties");
    }

    static File resolveConfigFile() {
        final File primary = getUserConfigFile();
        if (primary.isFile()) {
            return primary;
        }
        final String wd = System.getProperty("user.dir", ".");
        final File inSim = new File(wd + File.separator + "sim" + File.separator + "sim-verbose.properties");
        if (inSim.isFile()) {
            return inSim;
        }
        final File inWd = new File(wd, "sim-verbose.properties");
        if (inWd.isFile()) {
            return inWd;
        }
        return primary;
    }

    static boolean parseBool(final String raw, final boolean ifNullOrBlank) {
        if (raw == null) {
            return ifNullOrBlank;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return ifNullOrBlank;
        }
        final int hash = s.indexOf('#');
        if (hash >= 0) {
            s = s.substring(0, hash).trim();
        }
        if (s.isEmpty()) {
            return ifNullOrBlank;
        }
        final String firstToken = s.split("\\s+", 2)[0];
        if ("true".equalsIgnoreCase(firstToken) || "yes".equalsIgnoreCase(firstToken)
                || "1".equalsIgnoreCase(firstToken) || "on".equalsIgnoreCase(firstToken)) {
            return true;
        }
        if ("false".equalsIgnoreCase(firstToken) || "no".equalsIgnoreCase(firstToken)
                || "0".equalsIgnoreCase(firstToken) || "off".equalsIgnoreCase(firstToken)) {
            return false;
        }
        return ifNullOrBlank;
    }
}
