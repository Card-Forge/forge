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
package forge.quest.data;

import java.io.Serializable;

import forge.properties.NewConstants;
import forge.properties.PreferencesStore;

@SuppressWarnings("serial")
public class QuestPreferences extends PreferencesStore<QuestPreferences.QPref> implements Serializable {
    /**
     * Preference identifiers, and their default values.
     */
    public static enum QPref {

        // How many of each rarity comes in a won booster pack
        BOOSTER_COMMONS("11"),
        BOOSTER_UNCOMMONS("3"),
        BOOSTER_RARES("1"),
        // The preferred format of the won booster pack
        BOOSTER_FORMAT("Standard"),

        // How many credits are lost for losing a match
        PENALTY_LOSS("15"),

        // Currently chosen quest and deck 
        CURRENT_QUEST("DEFAULT"),
        CURRENT_DECK("DEFAULT"),

        // All of the rewards given out End of Match
        // Awarded to every match winner
        REWARDS_BASE("25"),
        // Didn't lose a game in the match
        REWARDS_UNDEFEATED("25"),
        // For each of your previous wins gain a small multiplier
        // This is here to award long quests with more money for buying expensive cards
        REWARDS_WINS_MULTIPLIER("0.3"),

        // Winning each game by other means "Poison", "Milling" or "Alternative" Win
        REWARDS_POISON("50"),
        REWARDS_MILLED("40"),
        REWARDS_ALTERNATIVE("100"),

        // If you Mulligan to 0 to start a game
        REWARDS_MULLIGAN0("500"),

        // How many turns it took you to win the game
        REWARDS_TURN15("5"),
        REWARDS_TURN10("50"),
        REWARDS_TURN5("250"),
        REWARDS_TURN1("1500"),

        // How many basic your starting pool has (if appropriate)
        STARTING_BASIC_LANDS("20"),
        STARTING_SNOW_LANDS("5"),

        // Commons in your starting pool, by difficulty
        STARTING_COMMONS_EASY("82"),
        STARTING_COMMONS_MEDIUM("80"),
        STARTING_COMMONS_HARD("78"),
        STARTING_COMMONS_EXPERT("76"),

        // Uncommons in your starting pool, by difficulty
        STARTING_UNCOMMONS_EASY("40"),
        STARTING_UNCOMMONS_MEDIUM("36"),
        STARTING_UNCOMMONS_HARD("32"),
        STARTING_UNCOMMONS_EXPERT("28"),

        // Rares in your starting pool, by difficulty
        STARTING_RARES_EASY("20"),
        STARTING_RARES_MEDIUM("18"),
        STARTING_RARES_HARD("16"),
        STARTING_RARES_EXPERT("15"),

        // Credits you start the quest with, by difficulty
        STARTING_CREDITS_EASY("250"),
        STARTING_CREDITS_MEDIUM("200"),
        STARTING_CREDITS_HARD("150"),
        STARTING_CREDITS_EXPERT("100"),

        // Matches won per booster award, by difficulty
        WINS_BOOSTER_EASY("1"),
        WINS_BOOSTER_MEDIUM("1"),
        WINS_BOOSTER_HARD("2"),
        WINS_BOOSTER_EXPERT("2"),

        // Matches won per increased rank, by difficulty
        // Rank affects how many packs are opened for singles in the spell shop
        WINS_RANKUP_EASY("3"),
        WINS_RANKUP_MEDIUM("4"),
        WINS_RANKUP_HARD("5"),
        WINS_RANKUP_EXPERT("6"),

        // Matches won to unlock Medium Opponents, by difficulty
        WINS_MEDIUMAI_EASY("10"),
        WINS_MEDIUMAI_MEDIUM("9"),
        WINS_MEDIUMAI_HARD("8"),
        WINS_MEDIUMAI_EXPERT("7"),

        // Matches won to unlock Hard Opponents, by difficulty
        WINS_HARDAI_EASY("20"),
        WINS_HARDAI_MEDIUM("18"),
        WINS_HARDAI_HARD("16"),
        WINS_HARDAI_EXPERT("14"),

        // Matches won to unlock Expert Opponents, by difficulty
        WINS_EXPERTAI_EASY("40"),
        WINS_EXPERTAI_MEDIUM("36"),
        WINS_EXPERTAI_HARD("32"),
        WINS_EXPERTAI_EXPERT("28"),

        // Maximum amount of "Packs" opened by the Shop and available as singles
        SHOP_MAX_PACKS("6"),

        // Rarity distribution of Singles in an Opened Shop Pack
        SHOP_SINGLES_COMMON("7"),
        SHOP_SINGLES_UNCOMMON("3"),
        SHOP_SINGLES_RARE("1"),

        // How many wins it takes to open an additional pack in the shop
        SHOP_WINS_FOR_ADDITIONAL_PACK("10"),
        // How many packs the shop start with. 
        SHOP_STARTING_PACKS("4");

        private final String strDefaultVal;

        /**
         * Instantiates a new q pref.
         * 
         * @param s0
         *            &emsp; {@link java.lang.String}
         */
        QPref(final String s0) {
            this.strDefaultVal = s0;
        }

        /**
         * Gets the default.
         * 
         * @return {@link java.lang.String}
         */
        public String getDefault() {
            return this.strDefaultVal;
        }
    }

    public static enum DifficultyPrefs {
        STARTING_COMMONS, 
        STARTING_UNCOMMONS,
        STARTING_RARES,
        STARTING_CREDITS,
        WINS_BOOSTER,
        WINS_RANKUP,
        WINS_MEDIUMAI,
        WINS_HARDAI,
        WINS_EXPERTAI
    }
    
    /** Instantiates a QuestPreferences object. */
    public QuestPreferences() {
        super(NewConstants.QUEST_PREFS_FILE);
    }

    protected QPref[] getEnumValues() {
        return QPref.values();
    }
    
    protected QPref valueOf(String name) {
        try {
            return QPref.valueOf(name);
        }
        catch (Exception e) {
            return null;
        }
    }

    protected String getPrefDefault(QPref key) {
        return key.getDefault();
    }

    /**
     * Returns a preference value according to a difficulty index.
     */
    public String getPref(DifficultyPrefs pref, int difficultyIndex) {
        String newQPref = pref.toString();

        switch (difficultyIndex) {
        case 0:
            newQPref += "_EASY";
            break;
        case 1:
            newQPref += "_MEDIUM";
            break;
        case 2:
            newQPref += "_HARD";
            break;
        case 3:
            newQPref += "_EXPERT";
            break;
        default:
            try {
                throw new Exception();
            } catch (final Exception e1) {
                System.err.println("Difficulty index out of bounds: " + difficultyIndex);
                e1.printStackTrace();
            }
        }

        return getPref(QPref.valueOf(newQPref));
    }

    /**
     * Returns a difficulty-indexed preference value, as an int.
     */
    public int getPrefInt(DifficultyPrefs pref, int difficultyIndex) {
        return Integer.parseInt(this.getPref(pref, difficultyIndex));
    }

    /**
     * Gets the difficulty.
     */
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
}
