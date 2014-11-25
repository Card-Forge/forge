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
package forge.planarconquest;

import forge.properties.ForgeConstants;
import forge.properties.PreferencesStore;

import java.io.Serializable;

@SuppressWarnings("serial")
public class ConquestPreferences extends PreferencesStore<ConquestPreferences.CQPref> implements Serializable {
    /**
     * Preference identifiers, and their default values.
     */
    public static enum CQPref {
        CURRENT_CONQUEST("DEFAULT"),

        WINS_TO_UNLOCK_COMMANDER_2("5"),
        WINS_TO_UNLOCK_COMMANDER_3("10"),
        WINS_TO_UNLOCK_COMMANDER_4("20"),
        WINS_TO_UNLOCK_PORTAL("25"),

        PERCENT_NORMAL("25"),
        PERCENT_COMMANDER("25"),
        PERCENT_PLANECHASE("25"),
        PERCENT_DOUBLE_VARIANT("25");

        private final String strDefaultVal;

        CQPref(final String s0) {
            this.strDefaultVal = s0;
        }

        public String getDefault() {
            return this.strDefaultVal;
        }
    }

    public static enum DifficultyPrefs {
    }

    public ConquestPreferences() {
        super(ForgeConstants.CONQUEST_PREFS_FILE, CQPref.class);
    }

    protected CQPref[] getEnumValues() {
        return CQPref.values();
    }
    
    protected CQPref valueOf(String name) {
        try {
            return CQPref.valueOf(name);
        }
        catch (Exception e) {
            return null;
        }
    }

    protected String getPrefDefault(CQPref key) {
        return key.getDefault();
    }

    public String getPref(DifficultyPrefs pref, int difficultyIndex) {
        String newCQPref = pref.toString();

        switch (difficultyIndex) {
        case 0:
            newCQPref += "_EASY";
            break;
        case 1:
            newCQPref += "_MEDIUM";
            break;
        case 2:
            newCQPref += "_HARD";
            break;
        case 3:
            newCQPref += "_EXPERT";
            break;
        default:
            try {
                throw new Exception();
            } catch (final Exception e1) {
                System.err.println("Difficulty index out of bounds: " + difficultyIndex);
                e1.printStackTrace();
            }
        }

        return getPref(CQPref.valueOf(newCQPref));
    }

    public int getPrefInt(DifficultyPrefs pref, int difficultyIndex) {
        return Integer.parseInt(this.getPref(pref, difficultyIndex));
    }

    public static String getDifficulty(int difficultyIndex) {
        String s;
        switch (difficultyIndex) {
        case 1:
            s = "EASY";
            break;
        case 2:
            s = "MEDIUM";
            break;
        case 3:
            s = "HARD";
            break;
        case 4:
            s = "EXPERT";
            break;
        default:
            s = "UNKNOWN";
        }
        return s;
    }

    public String validatePreference(CQPref qpref, int val) {
        return null;
    }
}
