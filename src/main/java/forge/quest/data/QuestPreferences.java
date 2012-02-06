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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import forge.properties.ForgeProps;
import forge.properties.NewConstants.Quest;

/**
 * Holds default preference values in an enum.
 * Loads preferred values when instantiated.
 * If a requested value is not present, default is returned.
 * 
 * @author Forge
 * @version $Id$
 */
@SuppressWarnings("serial")
public class QuestPreferences implements Serializable {
    private Map<QPref, String> preferenceValues;

    /** 
     * Preference identifiers, and their default values.
     * When this class is instantiated, these enum values are used
     * in a map that is populated with the current preferences
     * from the text file.
     */
    public enum QPref { /** */
        BOOSTER_COMMONS ("11"), /** */
        BOOSTER_UNCOMMONS ("3"), /** */
        BOOSTER_RARES ("1"), /** */
        BOOSTER_FORMAT ("Standard"), /** */

        PENALTY_LOSS ("15"), /** */
        CURRENT_QUEST ("DEFAULT"), /** */
        CURRENT_DECK ("DEFAULT"), /** */

        REWARDS_BASE ("25"), /** */
        REWARDS_UNDEFEATED ("25"), /** */
        REWARDS_WINS_MULTIPLIER ("0.3"), /** */
        REWARDS_POISON ("50"), /** */
        REWARDS_MILLED ("40"), /** */
        REWARDS_MULLIGAN0 ("500"), /** */
        REWARDS_ALTERNATIVE ("100"), /** */
        REWARDS_TURN15 ("5"), /** */
        REWARDS_TURN10 ("50"), /** */
        REWARDS_TURN5 ("250"), /** */
        REWARDS_TURN1 ("1500"), /** */

        STARTING_BASIC_LANDS ("20"), /** */
        STARTING_SNOW_LANDS ("5"), /** */

        STARTING_COMMONS ("DIFFICULTY_INDEX_REQD"), /** */
        STARTING_COMMONS_EASY ("82"), /** */
        STARTING_COMMONS_MEDIUM ("80"), /** */
        STARTING_COMMONS_HARD ("78"), /** */
        STARTING_COMMONS_EXPERT ("76"), /** */

        STARTING_UNCOMMONS ("DIFFICULTY_INDEX_REQD"), /** */
        STARTING_UNCOMMONS_EASY ("40"), /** */
        STARTING_UNCOMMONS_MEDIUM ("36"), /** */
        STARTING_UNCOMMONS_HARD ("32"), /** */
        STARTING_UNCOMMONS_EXPERT ("28"), /** */

        STARTING_RARES ("DIFFICULTY_INDEX_REQD"), /** */
        STARTING_RARES_EASY ("20"), /** */
        STARTING_RARES_MEDIUM ("18"), /** */
        STARTING_RARES_HARD ("16"), /** */
        STARTING_RARES_EXPERT ("15"), /** */

        STARTING_CREDITS ("DIFFICULTY_INDEX_REQD"), /** */
        STARTING_CREDITS_EASY ("250"), /** */
        STARTING_CREDITS_MEDIUM ("200"), /** */
        STARTING_CREDITS_HARD ("150"), /** */
        STARTING_CREDITS_EXPERT ("100"), /** */

        WINS_BOOSTER ("DIFFICULTY_INDEX_REQD"), /** */
        WINS_BOOSTER_EASY ("1"), /** */
        WINS_BOOSTER_MEDIUM ("1"), /** */
        WINS_BOOSTER_HARD ("2"), /** */
        WINS_BOOSTER_EXPERT ("2"), /** */

        WINS_RANKUP ("DIFFICULTY_INDEX_REQD"), /** */
        WINS_RANKUP_EASY ("3"), /** */
        WINS_RANKUP_MEDIUM ("4"), /** */
        WINS_RANKUP_HARD ("5"), /** */
        WINS_RANKUP_EXPERT ("6"), /** */

        WINS_MEDIUMAI ("DIFFICULTY_INDEX_REQD"), /** */
        WINS_MEDIUMAI_EASY ("10"), /** */
        WINS_MEDIUMAI_MEDIUM ("9"), /** */
        WINS_MEDIUMAI_HARD ("8"), /** */
        WINS_MEDIUMAI_EXPERT ("7"), /** */

        WINS_HARDAI ("DIFFICULTY_INDEX_REQD"), /** */
        WINS_HARDAI_EASY ("20"), /** */
        WINS_HARDAI_MEDIUM ("18"), /** */
        WINS_HARDAI_HARD ("16"), /** */
        WINS_HARDAI_EXPERT ("14"), /** */

        WINS_EXPERTAI ("DIFFICULTY_INDEX_REQD"), /** */
        WINS_EXPERTAI_EASY ("40"), /** */
        WINS_EXPERTAI_MEDIUM ("36"), /** */
        WINS_EXPERTAI_HARD ("32"), /** */
        WINS_EXPERTAI_EXPERT ("28"), /** */

        SHOP_MAX_PACKS ("6"), /** */
        SHOP_SINGLES_COMMON ("7"), /** */
        SHOP_SINGLES_UNCOMMON ("3"), /** */
        SHOP_SINGLES_RARE ("1"), /** */
        SHOP_WINS_FOR_ADDITIONAL_PACK ("10"), /** */
        SHOP_STARTING_PACKS ("4"); /** */

        private final String strDefaultVal;

        /** @param s0 &emsp; {@link java.lang.String} */
        QPref(String s0) {
            this.strDefaultVal = s0;
        }

        /** @return {@link java.lang.String} */
        public String getDefault() {
            return strDefaultVal;
        }
    }

    /** Instantiates a QuestPreferences object. */
    public QuestPreferences() {
        preferenceValues = new HashMap<QPref, String>();
        try {
            final BufferedReader input = new BufferedReader(new FileReader(ForgeProps.getFile(Quest.PREFS)));
            String line = null;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("#") || (line.length() == 0)) {
                    continue;
                }

                final String[] split = line.split("=");

                if (split.length == 2) {
                    this.setPreference(split[0], split[1]);
                }
            }
        } catch (FileNotFoundException ex) {
            //ex.printStackTrace();
        } catch (IOException ex) {
            //ex.printStackTrace();
        }
    }

    /** Saves prefs map to file. */
    public void save() {
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(ForgeProps.getFile(Quest.PREFS)));
            for (QPref key : QPref.values()) {
                if (key.getDefault().equals("DIFFICULTY_INDEX_REQD")) {
                    writer.newLine();
                    continue;
                }
                writer.write(key + "=" + getPreference(key));
                writer.newLine();
            }

            writer.flush();
            writer.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** */
    public void reset() {
        this.preferenceValues.clear();
    }

    /**
     * DUE TO BE DEPRECATED:
     * Transition code between preference manager for v1.2.2 and v1.2.3.
     * (string-based vs. enum-based)
     * 
     * @param s0 &emsp; {@link java.lang.String} identifier of preference
     * @param s1 &emsp; {@link java.lang.String} value
     */
    public void setPreference(String s0, String s1) {
        try {
            preferenceValues.put(QPref.valueOf(s0), s1);
        }
        catch (Exception e) {
        }
    }

    /**
     * @param q0 &emsp; {@link forge.quest.data.QuestPreferences.QPref}
     * @param s0 &emsp; {@link java.lang.String} value
     */
    public void setPreference(QPref q0, String s0) {
        preferenceValues.put(q0, s0);
    }

    /**
     * Returns a non-difficulty-indexed preference value.
     * 
     * @param qp0 &emsp; {@link forge.quest.data.QuestPreferences.QPref}
     * @return String
     */
    public String getPreference(QPref qp0) {
        String val;

        if (qp0.getDefault().equals("DIFFICULTY_INDEX_REQD")) {
            // This error indicates that this is a preference
            // value which is different based on difficulty.
            // A difficulty index must be passed to determine
            // which value is appropriate for this setting.
            // To do this, use getPreference(QPref, int).
            try { throw new Exception(); }
            catch (Exception e1) { e1.printStackTrace(); }
        }

        val = preferenceValues.get(qp0);
        if (val == null) { val = qp0.getDefault(); }

        return val;
    }

    /**
     * Returns a preference value according to a difficulty index.
     * 
     * @param qp0 &emsp; {@link forge.quest.data.QuestPreferences.QPref}
     * @param i0 &emsp; int difficulty index
     * @return String
     */
    public String getPreference(QPref qp0, int i0) {
        String val;
        String newQPref = qp0.toString();
        QPref q;

        switch(i0) {
            case 0: newQPref += "_EASY"; break;
            case 1: newQPref += "_MEDIUM"; break;
            case 2: newQPref += "_HARD"; break;
            case 3: newQPref += "_EXPERT"; break;
            default:
                try { throw new Exception(); }
                catch (Exception e1) {
                    System.err.println("Difficulty index (" + i0 + ") out of bounds! ");
                    e1.printStackTrace();
                }
        }

        q = QPref.valueOf(newQPref);
        val = preferenceValues.get(q);
        if (val == null) { val = q.getDefault(); }

        return val;
    }

    /**
     * Returns a non-difficulty-indexed preference value, as an int.
     * 
     * @param qp0 &emsp; {@link forge.quest.data.QuestPreferences.QPref}
     * @return int
     */
    public int getPreferenceInt(QPref qp0) {
        return Integer.parseInt(getPreference(qp0));
    }

    /**
     * Returns a difficulty-indexed preference value, as an int.
     * 
     * @param qp0 &emsp; {@link forge.quest.data.QuestPreferences.QPref}
     * @param i0 &emsp; int difficulty index
     * @return int
     */
    public int getPreferenceInt(QPref qp0, int i0) {
        return Integer.parseInt(getPreference(qp0, i0));
    }

    /**
     * @param i &emsp; int
     * @return String
     */
    public static String getDifficulty(int i) {
        String s;
        switch(i) {
            case 1: s = "EASY"; break;
            case 2: s = "MEDIUM"; break;
            case 3: s = "HARD"; break;
            case 4: s = "EXPERT"; break;
            default: s = "UNKNOWN";
        }
        return s;
    }
}
