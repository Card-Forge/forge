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
package forge.gamemodes.planarconquest;

import java.io.Serializable;

import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.PreferencesStore;

@SuppressWarnings("serial")
public class ConquestPreferences extends PreferencesStore<ConquestPreferences.CQPref> implements Serializable {
    /**
     * Preference identifiers, and their default values.
     */
    public enum CQPref {
        CURRENT_CONQUEST("DEFAULT"),

        AETHER_BASE_DUPLICATE_VALUE("100"),
        AETHER_BASE_EXILE_VALUE("75"),
        AETHER_BASE_RETRIEVE_COST("150"),
        AETHER_BASE_PULL_COST("200"),
        AETHER_UNCOMMON_MULTIPLIER("3"),
        AETHER_RARE_MULTIPLIER("10"),
        AETHER_MYTHIC_MULTIPLIER("25"),
        AETHER_START_SHARDS("3000"),
        AETHER_WHEEL_SHARDS("1000"),
        AETHER_USE_DEFAULT_RARITY_ODDS("true"),

        BOOSTER_COMMONS("11"),
        BOOSTER_UNCOMMONS("3"),
        BOOSTER_RARES("1"),
        BOOSTERS_PER_MYTHIC("8"),

        PLANESWALK_CONQUER_EMBLEMS("1"),
        PLANESWALK_WHEEL_EMBLEMS("5"),
        PLANESWALK_FIRST_UNLOCK("5"),
        PLANESWALK_UNLOCK_INCREASE("5"),

        CHAOS_BATTLE_WINS_MEDIUMAI("2"),
        CHAOS_BATTLE_WINS_HARDAI("5"),
        CHAOS_BATTLE_WINS_EXPERTAI("10");

        private final String strDefaultVal;

        CQPref(final String s0) {
            this.strDefaultVal = s0;
        }

        public String getDefault() {
            return this.strDefaultVal;
        }
    }

    public ConquestPreferences() {
        super(ForgeConstants.CONQUEST_PREFS_FILE, CQPref.class);
    }

    @Override
    public void save() {
        super.save();
        ConquestUtil.updateRarityFilterOdds();
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

    public String validatePreference(CQPref qpref, int val) {
        switch (qpref) {
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
        default:
            break;
        }
        return null;
    }
}
