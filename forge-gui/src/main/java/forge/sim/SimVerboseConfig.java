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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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

    /**
     * At each turn start, log card names from the top of the active player's library.
     * Value {@code 0} or absent: off. Positive: first {@code n} cards. {@code -1}: entire library.
     */
    public static final String BEGINNING_LIBRARY_COUNT = "beginning_library_count";
    /**
     * At each turn start, log card names from the top of the active player's graveyard.
     * Value {@code 0} or absent: off. Positive: first {@code n} cards. {@code -1}: entire graveyard.
     */
    public static final String BEGINNING_GRAVEYARD_COUNT = "beginning_graveyard_count";

    private static final Map<String, Boolean> DEFAULTS;
    static {
        Map<String, Boolean> d = new LinkedHashMap<>();
        d.put(DRAWS, Boolean.TRUE);
        // On by default with -v when no config file; set beginning_cards_in_hand=false to disable.
        d.put(BEGINNING_CARDS_IN_HAND, Boolean.TRUE);
        DEFAULTS = Collections.unmodifiableMap(d);
    }

    private final Map<String, Boolean> categories;
    /** {@code null} or {@code 0}: off; {@code -1}: log whole library; else first {@code n} cards from top. */
    private final Integer beginningLibraryCardCount;
    /** {@code null} or {@code 0}: off; {@code -1}: log whole graveyard; else first {@code n} cards. */
    private final Integer beginningGraveyardCardCount;

    private SimVerboseConfig(final Map<String, Boolean> categories0, final Integer beginningLibraryCardCount0,
            final Integer beginningGraveyardCardCount0) {
        this.categories = Collections.unmodifiableMap(categories0);
        this.beginningLibraryCardCount = beginningLibraryCardCount0;
        this.beginningGraveyardCardCount = beginningGraveyardCardCount0;
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
        if (logsBeginningLibrary() || logsBeginningGraveyard()) {
            return true;
        }
        for (final Boolean b : categories.values()) {
            if (Boolean.TRUE.equals(b)) {
                return true;
            }
        }
        return false;
    }

    /** @return configured count, or {@code null} if this logging is off */
    public Integer getBeginningLibraryCardCount() {
        return beginningLibraryCardCount;
    }

    public boolean logsBeginningLibrary() {
        return beginningLibraryCardCount != null && beginningLibraryCardCount != 0;
    }
    /** @return configured count, or {@code null} if this logging is off */
    public Integer getBeginningGraveyardCardCount() {
        return beginningGraveyardCardCount;
    }

    public boolean logsBeginningGraveyard() {
        return beginningGraveyardCardCount != null && beginningGraveyardCardCount != 0;
    }

    /**
     * Reads user config and merges with defaults. Missing file uses defaults only (draws on).
     * Merges every existing file in order (later files override earlier keys for the same name):
     * {@link #getUserConfigFile()}, {@code <workingDir>/sim/sim-verbose.properties},
     * {@code <workingDir>/sim-verbose.properties}. So a project-local file can add
     * {@code beginning_library_count} even when Forge user data already has a properties file
     * without that key.
     */
    public static SimVerboseConfig load() {
        final Map<String, Boolean> map = new LinkedHashMap<>(DEFAULTS);
        Integer beginningLibraryCount = null;
        Integer beginningGraveyardCount = null;
        final Properties p = loadMergedVerboseProperties();
        for (final String name : p.stringPropertyNames()) {
            String key = name.trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty()) {
                continue;
            }
            if ("begining_cards_in_hand".equals(key)) {
                key = BEGINNING_CARDS_IN_HAND;
            }
            if (BEGINNING_LIBRARY_COUNT.equals(key)) {
                beginningLibraryCount = parseCountOption(p.getProperty(name));
                continue;
            }
            if (BEGINNING_GRAVEYARD_COUNT.equals(key)) {
                beginningGraveyardCount = parseCountOption(p.getProperty(name));
                continue;
            }
            map.put(key, parseBool(p.getProperty(name), false));
        }
        return new SimVerboseConfig(map, beginningLibraryCount, beginningGraveyardCount);
    }

    /**
     * Loads and merges all sim-verbose.properties files that exist; same key in a later file wins.
     */
    static Properties loadMergedVerboseProperties() {
        final Properties merged = new Properties();
        for (final File f : listVerbosePropertyFiles()) {
            if (!f.isFile()) {
                continue;
            }
            try (InputStream in = Files.newInputStream(f.toPath())) {
                merged.load(in);
            } catch (final IOException e) {
                System.err.println("Could not read sim verbose config " + f + ": " + e.getMessage());
            }
        }
        return merged;
    }

    static List<File> listVerbosePropertyFiles() {
        final List<File> list = new ArrayList<>(3);
        final String wd = System.getProperty("user.dir", ".");
        list.add(getUserConfigFile());
        list.add(new File(wd + File.separator + "sim" + File.separator + "sim-verbose.properties"));
        list.add(new File(wd, "sim-verbose.properties"));
        return list;
    }

    /** Primary location under Forge user data (see Forge profile / install docs). */
    public static File getUserConfigFile() {
        final String userDir = ForgeProfileProperties.getUserDir();
        return new File(userDir + "sim" + File.separator + "sim-verbose.properties");
    }

    /** First existing file among {@link #listVerbosePropertyFiles()} (for messages / tooling). */
    static File resolveConfigFile() {
        for (final File f : listVerbosePropertyFiles()) {
            if (f.isFile()) {
                return f;
            }
        }
        return getUserConfigFile();
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

    /**
     * @return {@code null} if off or invalid; {@code -1} for whole library; positive for first {@code n} cards
     */
    static Integer parseCountOption(final String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        if (s.isEmpty()) {
            return null;
        }
        final int hash = s.indexOf('#');
        if (hash >= 0) {
            s = s.substring(0, hash).trim();
        }
        if (s.isEmpty()) {
            return null;
        }
        final String firstToken = s.split("\\s+", 2)[0];
        try {
            final int n = Integer.parseInt(firstToken);
            if (n == 0) {
                return null;
            }
            if (n == -1) {
                return -1;
            }
            if (n < -1) {
                return null;
            }
            return n;
        } catch (final NumberFormatException ignored) {
            return null;
        }
    }
}
