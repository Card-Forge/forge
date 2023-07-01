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
import java.io.FileWriter;
import java.io.IOException;
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
public abstract class PreferencesStore<T extends Enum<T>> {
    private final Map<T, String> preferenceValues;
    private final String filename;

    public PreferencesStore(final String filename0, final Class<T> clasz) {
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
    protected abstract String getPrefDefault(T key);

    public void save() {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(filename));
            for (final T key : getEnumValues()) {
                writer.write(key + "=" + getPref(key));
                writer.newLine();
            }
        } catch (final IOException ex) {
            ex.printStackTrace();
        } finally {
            if (null != writer) {
                try {
                    writer.close();
                } catch (final IOException e) {
                    System.out.println("error while closing " + filename);
                }
            }
        }
    }

    public final void reset() {
        this.preferenceValues.clear();
    }

    public final void setPref(final T q0, final String s0) {
        preferenceValues.put(q0, s0);
    }

    public final void setPref(final T q0, final boolean val) {
        setPref(q0, String.valueOf(val));
    }

    public final String getPref(final T fp0) {
        String val;

        val = preferenceValues.get(fp0);
        if (val == null) { val = getPrefDefault(fp0); }

        return val;
    }

    public final int getPrefInt(final T fp0) {
        try{
            return Integer.parseInt(getPref(fp0));
        } catch(NumberFormatException e) {
            return Integer.parseInt(getPrefDefault(fp0));
        }
    }

    public final boolean getPrefBoolean(final T fp0) {
        return Boolean.parseBoolean(getPref(fp0));
    }
    
    public final double getPrefDouble(final T fp0) {
        return Double.parseDouble(getPref(fp0));        
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
