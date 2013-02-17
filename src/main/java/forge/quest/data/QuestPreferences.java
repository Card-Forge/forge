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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.properties.ForgeProps;
import forge.properties.NewConstants.Quest;
import forge.util.FileUtil;

/**
 * Holds default preference values in an enum. Loads preferred values when
 * instantiated. If a requested value is not present, default is returned.
 * 
 * @author Forge
 * @version $Id$
 */
@SuppressWarnings("serial")
public class QuestPreferences implements Serializable {
    private final Map<QPref, String> preferenceValues;

    /**
     * Preference identifiers, and their default values. When this class is
     * instantiated, these enum values are used in a map that is populated with
     * the current preferences from the text file.
     */
    public enum QPref {

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
        STARTING_COMMONS("DIFFICULTY_INDEX_REQD"), 
        STARTING_COMMONS_EASY("82"),
        STARTING_COMMONS_MEDIUM("80"),
        STARTING_COMMONS_HARD("78"),
        STARTING_COMMONS_EXPERT("76"),

        // Uncommons in your starting pool, by difficulty
        STARTING_UNCOMMONS("DIFFICULTY_INDEX_REQD"),
        STARTING_UNCOMMONS_EASY("40"),
        STARTING_UNCOMMONS_MEDIUM("36"),
        STARTING_UNCOMMONS_HARD("32"),
        STARTING_UNCOMMONS_EXPERT("28"),

        // Rares in your starting pool, by difficulty
        STARTING_RARES("DIFFICULTY_INDEX_REQD"),
        STARTING_RARES_EASY("20"),
        STARTING_RARES_MEDIUM("18"),
        STARTING_RARES_HARD("16"),
        STARTING_RARES_EXPERT("15"),

        // Credits you start the quest with, by difficulty
        STARTING_CREDITS("DIFFICULTY_INDEX_REQD"),
        STARTING_CREDITS_EASY("250"),
        STARTING_CREDITS_MEDIUM("200"),
        STARTING_CREDITS_HARD("150"),
        STARTING_CREDITS_EXPERT("100"),

        // Matches won per booster award, by difficulty
        WINS_BOOSTER("DIFFICULTY_INDEX_REQD"),
        WINS_BOOSTER_EASY("1"),
        WINS_BOOSTER_MEDIUM("1"),
        WINS_BOOSTER_HARD("2"),
        WINS_BOOSTER_EXPERT("2"),

        // Matches won per increased rank, by difficulty
        // Rank affects how many packs are opened for singles in the spell shop
        WINS_RANKUP("DIFFICULTY_INDEX_REQD"),
        WINS_RANKUP_EASY("3"),
        WINS_RANKUP_MEDIUM("4"),
        WINS_RANKUP_HARD("5"),
        WINS_RANKUP_EXPERT("6"),

        // Matches won to unlock Medium Opponents, by difficulty
        WINS_MEDIUMAI("DIFFICULTY_INDEX_REQD"),
        WINS_MEDIUMAI_EASY("10"),
        WINS_MEDIUMAI_MEDIUM("9"),
        WINS_MEDIUMAI_HARD("8"),
        WINS_MEDIUMAI_EXPERT("7"),

        // Matches won to unlock Hard Opponents, by difficulty
        WINS_HARDAI("DIFFICULTY_INDEX_REQD"),
        WINS_HARDAI_EASY("20"),
        WINS_HARDAI_MEDIUM("18"),
        WINS_HARDAI_HARD("16"),
        WINS_HARDAI_EXPERT("14"),

        // Matches won to unlock Expert Opponents, by difficulty
        WINS_EXPERTAI("DIFFICULTY_INDEX_REQD"),
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

    /** Instantiates a QuestPreferences object. */
    public QuestPreferences() {
        this.preferenceValues = new HashMap<QPref, String>();

        List<String> lines = FileUtil.readFile(ForgeProps.getFile(Quest.PREFS));

        for (String line : lines) {
            if (line.startsWith("#") || (line.length() == 0)) {
                continue;
            }

            final String[] split = line.split("=");

            if (split.length == 2) {
                this.setPreference(split[0], split[1]);
            }
        }
    }

    /** Saves prefs map to file. */
    public void save() {
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(ForgeProps.getFile(Quest.PREFS)));
            for (final QPref key : QPref.values()) {
                if (key.getDefault().equals("DIFFICULTY_INDEX_REQD")) {
                    writer.newLine();
                    continue;
                }
                writer.write(key + "=" + this.getPreference(key));
                writer.newLine();
            }

            writer.flush();
            writer.close();
        } catch (final FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (final IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Reset.
     */
    public void reset() {
        this.preferenceValues.clear();
    }

    /**
     * DUE TO BE DEPRECATED: Transition code between preference manager for
     * v1.2.2 and v1.2.3. (string-based vs. enum-based)
     * 
     * @param s0
     *            &emsp; {@link java.lang.String} identifier of preference
     * @param s1
     *            &emsp; {@link java.lang.String} value
     */
    public void setPreference(final String s0, final String s1) {
        try {
            this.preferenceValues.put(QPref.valueOf(s0), s1);
        } catch (final Exception e) {
        }
    }

    /**
     * Sets the preference.
     * 
     * @param q0
     *            &emsp; {@link forge.quest.data.QuestPreferences.QPref}
     * @param s0
     *            &emsp; {@link java.lang.String} value
     */
    public void setPreference(final QPref q0, final String s0) {
        this.preferenceValues.put(q0, s0);
    }

    /**
     * Returns a non-difficulty-indexed preference value.
     * 
     * @param qp0
     *            &emsp; {@link forge.quest.data.QuestPreferences.QPref}
     * @return String
     */
    public String getPreference(final QPref qp0) {
        String val;

        if (qp0.getDefault().equals("DIFFICULTY_INDEX_REQD")) {
            // This error indicates that this is a preference
            // value which is different based on difficulty.
            // A difficulty index must be passed to determine
            // which value is appropriate for this setting.
            // To do this, use getPreference(QPref, int).
            try {
                throw new Exception();
            } catch (final Exception e1) {
                e1.printStackTrace();
            }
        }

        val = this.preferenceValues.get(qp0);
        if (val == null) {
            val = qp0.getDefault();
        }

        return val;
    }

    /**
     * Returns a preference value according to a difficulty index.
     * 
     * @param qp0
     *            &emsp; {@link forge.quest.data.QuestPreferences.QPref}
     * @param i0
     *            &emsp; int difficulty index
     * @return String
     */
    public String getPreference(final QPref qp0, final int i0) {
        String val;
        String newQPref = qp0.toString();
        QPref q;

        switch (i0) {
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
                System.err.println("Difficulty index (" + i0 + ") out of bounds! ");
                e1.printStackTrace();
            }
        }

        q = QPref.valueOf(newQPref);
        val = this.preferenceValues.get(q);
        if (val == null) {
            val = q.getDefault();
        }

        return val;
    }

    /**
     * Returns a non-difficulty-indexed preference value, as an int.
     * 
     * @param qp0
     *            &emsp; {@link forge.quest.data.QuestPreferences.QPref}
     * @return int
     */
    public int getPreferenceInt(final QPref qp0) {
        return Integer.parseInt(this.getPreference(qp0));
    }

    /**
     * Returns a difficulty-indexed preference value, as an int.
     * 
     * @param qp0
     *            &emsp; {@link forge.quest.data.QuestPreferences.QPref}
     * @param i0
     *            &emsp; int difficulty index
     * @return int
     */
    public int getPreferenceInt(final QPref qp0, final int i0) {
        return Integer.parseInt(this.getPreference(qp0, i0));
    }

    /**
     * Gets the difficulty.
     * 
     * @param i
     *            &emsp; int
     * @return String
     */
    public static String getDifficulty(final int i) {
        String s;
        switch (i) {
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
