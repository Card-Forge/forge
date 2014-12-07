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

        PERCENT_COMMANDER("25"),
        PERCENT_PLANECHASE("25"),
        PERCENT_DOUBLE_VARIANT("25"),

        BOOSTER_COMMONS("11"),
        BOOSTER_UNCOMMONS("3"),
        BOOSTER_RARES("1"),
        BOOSTERS_PER_MYTHIC("8"),
        BOOSTER_COMMON_REROLL("10"),
        BOOSTER_UNCOMMON_REROLL("25"),
        BOOSTER_RARE_REROLL("50"),
        BOOSTER_MYTHIC_REROLL("100"),

        RECRUIT_BONUS_CARD_ODDS("25"),
        STUDY_BONUS_CARD_ODDS("25"),
        DEFEND_BONUS_LIFE("5");

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
        switch (qpref) {
        case PERCENT_COMMANDER:
            if (val + getPrefInt(CQPref.PERCENT_PLANECHASE) + getPrefInt(CQPref.PERCENT_DOUBLE_VARIANT) > 100) {
                return "Variant Frequency values must add up to 100 or less.";
            }
            break;
        case PERCENT_PLANECHASE:
            if (val + getPrefInt(CQPref.PERCENT_COMMANDER) + getPrefInt(CQPref.PERCENT_DOUBLE_VARIANT) > 100) {
                return "Variant Frequency values must add up to 100 or less.";
            }
            break;
        case PERCENT_DOUBLE_VARIANT:
            if (val + getPrefInt(CQPref.PERCENT_COMMANDER) + getPrefInt(CQPref.PERCENT_PLANECHASE) > 100) {
                return "Variant Frequency values must add up to 100 or less.";
            }
            break;
        case BOOSTER_COMMONS:
            if (val + getPrefInt(CQPref.BOOSTER_UNCOMMONS) + getPrefInt(CQPref.BOOSTER_RARES) > 15) {
                return "Booster packs must have maximum 15 cards.";
            }
            break;
        case BOOSTER_UNCOMMONS:
            if (val + getPrefInt(CQPref.BOOSTER_COMMONS) + getPrefInt(CQPref.BOOSTER_RARES) > 15) {
                return "Booster packs must have maximum 15 cards.";
            }
            break;
        case BOOSTER_RARES:
            if (val + getPrefInt(CQPref.BOOSTER_COMMONS) + getPrefInt(CQPref.BOOSTER_UNCOMMONS) > 15) {
                return "Booster packs must have maximum 15 cards.";
            }
            break;
        case BOOSTERS_PER_MYTHIC:
            if (val + getPrefInt(CQPref.BOOSTER_COMMONS) + getPrefInt(CQPref.BOOSTER_UNCOMMONS) > 15) {
                return "Booster packs must have maximum 15 cards.";
            }
            break;
        case BOOSTER_COMMON_REROLL:
        case BOOSTER_UNCOMMON_REROLL:
        case BOOSTER_RARE_REROLL:
        case BOOSTER_MYTHIC_REROLL:
            if (val > 100) {
                return "Booster reroll chance must be between 0% and 100%.";
            }
            break;
        case RECRUIT_BONUS_CARD_ODDS:
        case STUDY_BONUS_CARD_ODDS:
            if (val > 100) {
                return "Bonus card odds must be between 0% and 50%.";
            }
            break;
        case DEFEND_BONUS_LIFE:
            if (val > 10) {
                return "Bonus life must be between 0 and 10.";
            }
            break;
        default:
            break;
        }
        return null;
    }
}
