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

        /** The BOOSTE r_ commons. */
        BOOSTER_COMMONS("11"),
        /** The BOOSTE r_ uncommons. */
        BOOSTER_UNCOMMONS("3"),
        /** The BOOSTE r_ rares. */
        BOOSTER_RARES("1"),
        /** The BOOSTE r_ format. */
        BOOSTER_FORMAT("Standard"),
        /** The PENALT y_ loss. */

        PENALTY_LOSS("15"), /** The CURREN t_ quest. */
        CURRENT_QUEST("DEFAULT"),
        /** The CURREN t_ deck. */
        CURRENT_DECK("DEFAULT"),
        /** The REWARD s_ base. */

        REWARDS_BASE("25"), /** The REWARD s_ undefeated. */
        REWARDS_UNDEFEATED("25"),
        /** The REWARD s_ win s_ multiplier. */
        REWARDS_WINS_MULTIPLIER("0.3"),
        /** The REWARD s_ poison. */
        REWARDS_POISON("50"),
        /** The REWARD s_ milled. */
        REWARDS_MILLED("40"),
        /** The REWARD s_ mulliga n0. */
        REWARDS_MULLIGAN0("500"),
        /** The REWARD s_ alternative. */
        REWARDS_ALTERNATIVE("100"),
        /** The REWARD s_ tur n15. */
        REWARDS_TURN15("5"),
        /** The REWARD s_ tur n10. */
        REWARDS_TURN10("50"),
        /** The REWARD s_ tur n5. */
        REWARDS_TURN5("250"),
        /** The REWARD s_ tur n1. */
        REWARDS_TURN1("1500"),
        /** The STARTIN g_ basi c_ lands. */

        STARTING_BASIC_LANDS("20"), /** The STARTIN g_ sno w_ lands. */
        STARTING_SNOW_LANDS("5"),
        /** The STARTIN g_ commons. */

        STARTING_COMMONS("DIFFICULTY_INDEX_REQD"), /**
         * The STARTIN g_ common s_
         * easy.
         */
        STARTING_COMMONS_EASY("82"),
        /** The STARTIN g_ common s_ medium. */
        STARTING_COMMONS_MEDIUM("80"),
        /** The STARTIN g_ common s_ hard. */
        STARTING_COMMONS_HARD("78"),
        /** The STARTIN g_ common s_ expert. */
        STARTING_COMMONS_EXPERT("76"),
        /** The STARTIN g_ uncommons. */

        STARTING_UNCOMMONS("DIFFICULTY_INDEX_REQD"), /**
         * The STARTIN g_ uncommon
         * s_ easy.
         */
        STARTING_UNCOMMONS_EASY("40"),
        /** The STARTIN g_ uncommon s_ medium. */
        STARTING_UNCOMMONS_MEDIUM("36"),
        /** The STARTIN g_ uncommon s_ hard. */
        STARTING_UNCOMMONS_HARD("32"),
        /** The STARTIN g_ uncommon s_ expert. */
        STARTING_UNCOMMONS_EXPERT("28"),
        /** The STARTIN g_ rares. */

        STARTING_RARES("DIFFICULTY_INDEX_REQD"), /** The STARTIN g_ rare s_ easy. */
        STARTING_RARES_EASY("20"),
        /** The STARTIN g_ rare s_ medium. */
        STARTING_RARES_MEDIUM("18"),
        /** The STARTIN g_ rare s_ hard. */
        STARTING_RARES_HARD("16"),
        /** The STARTIN g_ rare s_ expert. */
        STARTING_RARES_EXPERT("15"),
        /** The STARTIN g_ credits. */

        STARTING_CREDITS("DIFFICULTY_INDEX_REQD"), /**
         * The STARTIN g_ credit s_
         * easy.
         */
        STARTING_CREDITS_EASY("250"),
        /** The STARTIN g_ credit s_ medium. */
        STARTING_CREDITS_MEDIUM("200"),
        /** The STARTIN g_ credit s_ hard. */
        STARTING_CREDITS_HARD("150"),
        /** The STARTIN g_ credit s_ expert. */
        STARTING_CREDITS_EXPERT("100"),
        /** The WIN s_ booster. */

        WINS_BOOSTER("DIFFICULTY_INDEX_REQD"), /** The WIN s_ booste r_ easy. */
        WINS_BOOSTER_EASY("1"),
        /** The WIN s_ booste r_ medium. */
        WINS_BOOSTER_MEDIUM("1"),
        /** The WIN s_ booste r_ hard. */
        WINS_BOOSTER_HARD("2"),
        /** The WIN s_ booste r_ expert. */
        WINS_BOOSTER_EXPERT("2"),
        /** The WIN s_ rankup. */

        WINS_RANKUP("DIFFICULTY_INDEX_REQD"), /** The WIN s_ ranku p_ easy. */
        WINS_RANKUP_EASY("3"),
        /** The WIN s_ ranku p_ medium. */
        WINS_RANKUP_MEDIUM("4"),
        /** The WIN s_ ranku p_ hard. */
        WINS_RANKUP_HARD("5"),
        /** The WIN s_ ranku p_ expert. */
        WINS_RANKUP_EXPERT("6"),
        /** The WIN s_ mediumai. */

        WINS_MEDIUMAI("DIFFICULTY_INDEX_REQD"), /** The WIN s_ mediuma i_ easy. */
        WINS_MEDIUMAI_EASY("10"),
        /** The WIN s_ mediuma i_ medium. */
        WINS_MEDIUMAI_MEDIUM("9"),
        /** The WIN s_ mediuma i_ hard. */
        WINS_MEDIUMAI_HARD("8"),
        /** The WIN s_ mediuma i_ expert. */
        WINS_MEDIUMAI_EXPERT("7"),
        /** The WIN s_ hardai. */

        WINS_HARDAI("DIFFICULTY_INDEX_REQD"), /** The WIN s_ harda i_ easy. */
        WINS_HARDAI_EASY("20"),
        /** The WIN s_ harda i_ medium. */
        WINS_HARDAI_MEDIUM("18"),
        /** The WIN s_ harda i_ hard. */
        WINS_HARDAI_HARD("16"),
        /** The WIN s_ harda i_ expert. */
        WINS_HARDAI_EXPERT("14"),
        /** The WIN s_ expertai. */

        WINS_EXPERTAI("DIFFICULTY_INDEX_REQD"), /** The WIN s_ experta i_ easy. */
        WINS_EXPERTAI_EASY("40"),
        /** The WIN s_ experta i_ medium. */
        WINS_EXPERTAI_MEDIUM("36"),
        /** The WIN s_ experta i_ hard. */
        WINS_EXPERTAI_HARD("32"),
        /** The WIN s_ experta i_ expert. */
        WINS_EXPERTAI_EXPERT("28"),
        /** The SHO p_ ma x_ packs. */

        SHOP_MAX_PACKS("6"), /** The SHO p_ single s_ common. */
        SHOP_SINGLES_COMMON("7"),
        /** The SHO p_ single s_ uncommon. */
        SHOP_SINGLES_UNCOMMON("3"),
        /** The SHO p_ single s_ rare. */
        SHOP_SINGLES_RARE("1"),
        /** The SHO p_ win s_ fo r_ additiona l_ pack. */
        SHOP_WINS_FOR_ADDITIONAL_PACK("10"),
        /** The SHO p_ startin g_ packs. */
        SHOP_STARTING_PACKS("4");
        /** */

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
