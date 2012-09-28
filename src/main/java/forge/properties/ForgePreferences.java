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
package forge.properties;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import forge.gui.home.EMenuItem;

/**
 * Holds default preference values in an enum.
 * Loads preferred values when instantiated.
 * If a requested value is not present, default is returned.
 * 
 * @author Forge
 * @version $Id$
 */
public class ForgePreferences {
    private Map<FPref, String> preferenceValues;

    /** 
     * Preference identifiers, and their default values.
     * When this class is instantiated, these enum values are used
     * in a map that is populated with the current preferences
     * from the text file.
     */
    public enum FPref { /** */
        UI_USE_OLD ("false"), /** */
        UI_RANDOM_FOIL ("false"), /** */
        UI_SMOOTH_LAND ("false"), /** */
        UI_AVATARS ("0,1"), /** */
        UI_CARD_OVERLAY ("true"), /** */
        UI_UPLOAD_DRAFT ("false"), /** */
        UI_SCALE_LARGER ("true"), /** */
        UI_MAX_STACK ("3"), /** */
        UI_STACK_OFFSET ("tiny"), /** */
        UI_CARD_SIZE ("small"), /** */
        UI_BUGZ_NAME (""), /** */
        UI_BUGZ_PWD (""), /** */
        UI_ANTE ("false"), /** */
        UI_MANABURN("false"), /** */
        UI_SKIN ("default"), /** */
        UI_PREFERRED_AVATARS_ONLY ("false"), /** */
        UI_TARGETING_OVERLAY ("false"), /** */

        SUBMENU_CURRENTMENU (EMenuItem.CONSTRUCTED.toString()), /** */
        SUBMENU_SANCTIONED ("false"), /** */
        SUBMENU_QUEST ("false"), /** */
        SUBMENU_SETTINGS ("false"), /** */
        SUBMENU_UTILITIES ("false"), /** */

        DEV_MODE_ENABLED ("false"), /** */
        DEV_MILLING_LOSS ("true"), /** */
        DEV_UNLIMITED_LAND ("false"), /** */

        DECKGEN_SINGLETONS ("false"), /** */
        DECKGEN_ARTIFACTS ("false"), /** */
        DECKGEN_NOSMALL ("false"), /** */

        PHASE_AI_UPKEEP ("true"), /** */
        PHASE_AI_DRAW ("true"), /** */
        PHASE_AI_MAIN1 ("true"), /** */
        PHASE_AI_BEGINCOMBAT ("true"), /** */
        PHASE_AI_DECLAREATTACKERS ("true"), /** */
        PHASE_AI_DECLAREBLOCKERS ("true"), /** */
        PHASE_AI_FIRSTSTRIKE ("true"), /** */
        PHASE_AI_COMBATDAMAGE ("true"), /** */
        PHASE_AI_ENDCOMBAT ("true"), /** */
        PHASE_AI_MAIN2 ("true"), /** */
        PHASE_AI_EOT ("true"), /** */
        PHASE_AI_CLEANUP ("true"), /** */

        PHASE_HUMAN_UPKEEP ("true"), /** */
        PHASE_HUMAN_DRAW ("true"), /** */
        PHASE_HUMAN_MAIN1 ("true"), /** */
        PHASE_HUMAN_BEGINCOMBAT ("true"), /** */
        PHASE_HUMAN_DECLAREATTACKERS ("true"), /** */
        PHASE_HUMAN_DECLAREBLOCKERS ("true"), /** */
        PHASE_HUMAN_FIRSTSTRIKE ("true"), /** */
        PHASE_HUMAN_COMBATDAMAGE ("true"), /** */
        PHASE_HUMAN_ENDCOMBAT ("true"), /** */
        PHASE_HUMAN_MAIN2 ("true"), /** */
        PHASE_HUMAN_EOT ("true"), /** */
        PHASE_HUMAN_CLEANUP ("true"), /** */

        SHORTCUT_SHOWSTACK ("83"), /** */
        SHORTCUT_SHOWCOMBAT ("67"), /** */
        SHORTCUT_SHOWCONSOLE ("76"), /** */
        SHORTCUT_SHOWPLAYERS ("80"), /** */
        SHORTCUT_SHOWDEV ("68"), /** */
        SHORTCUT_CONCEDE ("17"), /** */
        SHORTCUT_ENDTURN ("69");

        private final String strDefaultVal;

        /** @param s0 &emsp; {@link java.lang.String} */
        FPref(String s0) {
            this.strDefaultVal = s0;
        }

        /** @return {@link java.lang.String} */
        public String getDefault() {
            return strDefaultVal;
        }
    }

    /** */
    public enum CardSizeType {
        /** */
        tiny, smaller, small, medium, large, huge
    }

    /** */
    public enum StackOffsetType {
        /** */
        tiny, small, medium, large
    }

    /** */
    public enum HomeMenus {
        /** */
        constructed, draft, sealed, quest, settings, utilities
    }

    /** Instantiates a ForgePreferences object. */
    public ForgePreferences() {
        preferenceValues = new HashMap<FPref, String>();
        try {
            // Preferences files have been consolidated into res/prefs/.
            // This code is here temporarily to facilitate this transfer.
            // After a while, this can be deleted.  Doublestrike 21-5-12
            final File oldFile = new File("forge.preferences");
            if (oldFile.exists()) {
                final File newFile = new File(NewConstants.PREFS_GLOBAL_FILE);
                final InputStream in = new FileInputStream(oldFile);
                final OutputStream out = new FileOutputStream(newFile);

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                oldFile.delete();
            }  // END TEMPORARY CONSOLIDATION FACILITATION

            final BufferedReader input = new BufferedReader(new FileReader(NewConstants.PREFS_GLOBAL_FILE));
            String line = null;
            while ((line = input.readLine()) != null) {
                if (line.startsWith("#") || (line.length() == 0)) {
                    continue;
                }

                final String[] split = line.split("=");

                if (split.length == 2) {
                    this.setPref(split[0], split[1]);
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
            writer = new BufferedWriter(new FileWriter(NewConstants.PREFS_GLOBAL_FILE));
            for (FPref key : FPref.values()) {
                writer.write(key + "=" + getPref(key));
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
    public void setPref(String s0, String s1) {
        try {
            preferenceValues.put(FPref.valueOf(s0), s1);
        }
        catch (Exception e) {
        }
    }

    /**
     * @param q0 &emsp; {@link forge.properties.ForgePreferences.FPref}
     * @param s0 &emsp; {@link java.lang.String} value
     */
    public void setPref(FPref q0, String s0) {
        preferenceValues.put(q0, s0);
    }

    /**
     * Returns a non-difficulty-indexed preference value.
     * 
     * @param fp0 &emsp; {@link forge.quest.data.ForgePreferences.FPref}
     * @return String
     */
    public String getPref(FPref fp0) {
        String val;

        val = preferenceValues.get(fp0);
        if (val == null) { val = fp0.getDefault(); }

        return val;
    }

    /**
     * Returns a non-difficulty-indexed preference value, as an int.
     * 
     * @param fp0 &emsp; {@link forge.quest.data.ForgePreferences.FPref}
     * @return int
     */
    public int getPrefInt(FPref fp0) {
        return Integer.parseInt(getPref(fp0));
    }

    /**
     * Returns a non-difficulty-indexed preference value, as a boolean.
     * 
     * @param fp0 &emsp; {@link forge.quest.data.ForgePreferences.FPref}
     * @return boolean
     */
    public boolean getPrefBoolean(FPref fp0) {
        return Boolean.parseBoolean(getPref(fp0));
    }
}
