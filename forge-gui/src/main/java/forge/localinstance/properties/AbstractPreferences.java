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
package forge.localinstance.properties;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.game.GameType;
import forge.util.FileUtil;
import forge.util.TextUtil;

/**
 * Holds default preference values in an enum.
 * Loads preferred values when instantiated.
 * If a requested value is not present, default is returned.
 */
public abstract class AbstractPreferences<T extends Enum<T> & IPreferences.IPref> implements IPreferences<T> {
    private final Map<T, String> preferenceValues;
    private final String filename;

    public AbstractPreferences(final String filename0, final Class<T> clasz) {
        preferenceValues = new EnumMap<>(clasz);
        filename = filename0;

        final List<String> lines = FileUtil.readFile(filename);
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") || (line.isEmpty())) {
                continue;
            }

            final String[] split = line.split("=");
            final T pref = valueOf(split[0]);

            if (null == pref) {
                System.out.println("unknown preference: " + line);
                continue;
            }

            if (split.length == 2) {
                this.setPref(pref, split[1]);
            } else if (split.length == 1 && line.endsWith("=")) {
                this.setPref(pref, "");
            }
        }
    }

    protected abstract T[] getEnumValues();
    protected abstract T valueOf(String name);

    @Override
    public void save() {
        final File target = new File(filename);
        final File parent = target.getParentFile();
        if (parent != null && !FileUtil.ensureDirectoryExists(parent)) {
            System.err.println("Error creating preferences directory: " + parent);
            return;
        }

        // Write to a temp file first, then replace the live file.
        final File tmp = new File(filename + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tmp))) {
            for (final T key : getEnumValues()) {
                writer.write(key + "=" + getPref(key));
                writer.newLine();
            }
        } catch (final IOException ex) {
            System.err.println("Error writing preferences to " + tmp + ": " + ex);
            ex.printStackTrace();
            FileUtil.deleteFile(tmp.getAbsolutePath());
            return;
        }

        try {
            try {
                Files.move(tmp.toPath(), target.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (final AtomicMoveNotSupportedException e) {
                Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (final IOException ex) {
            System.err.println("Error replacing preferences file " + filename + ": " + ex);
            ex.printStackTrace();
            FileUtil.deleteFile(tmp.getAbsolutePath());
        }
    }

    @Override
    public final void reset() {
        this.preferenceValues.clear();
    }

    @Override
    public final void setPref(final T q0, final String s0) {
        preferenceValues.put(q0, s0);
    }

    @Override
    public final String getPref(final T fp0) {
        String val;

        val = preferenceValues.get(fp0);
        if (val == null) { val = getPrefDefault(fp0); }

        return val;
    }

    public void setGameType(final T q0, final Set<GameType> gameTypes) {
        String s0 = "";
        Set<String> e = new HashSet<>();
        for (GameType g : gameTypes)
            e.add(g.getEnglishName());
        if (!e.isEmpty())
            s0 += String.join(",", e);
        setPref(q0, s0);
    }
    public final Set<GameType> getGameType(final T fp0) {
        Set<GameType> gameTypes = new HashSet<>();
        String value;
        value = preferenceValues.get(fp0);
        if (value != null) {
            if (value.contains(",")) {
                String[] values = TextUtil.split(value, ',');
                for (String gameType : values) {
                    addGameType(gameTypes, gameType);
                }
            } else {
                addGameType(gameTypes, value);
            }
        }
        return gameTypes;
    }
    void addGameType(Set<GameType> result, String gameType) {
        if (gameType.equals("Vanguard"))
            result.add(GameType.Vanguard);
        else if (gameType.equals("Momir Basic"))
            result.add(GameType.MomirBasic);
        else if (gameType.equals("MoJhoSto"))
            result.add(GameType.MoJhoSto);
        else if (gameType.equals("Commander"))
            result.add(GameType.Commander);
        else if (gameType.equals("Oathbreaker"))
            result.add(GameType.Oathbreaker);
        else if (gameType.equals("Tiny Leaders"))
            result.add(GameType.TinyLeaders);
        else if (gameType.equals("Brawl"))
            result.add(GameType.Brawl);
        else if (gameType.equals("Planechase"))
            result.add(GameType.Planechase);
        else if (gameType.equals("Archenemy"))
            result.add(GameType.Archenemy);
        else if (gameType.equals("Archenemy Rumble"))
            result.add(GameType.ArchenemyRumble);
    }
}
