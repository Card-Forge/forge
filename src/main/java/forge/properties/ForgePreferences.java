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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * <p>
 * ForgePreferences class.
 * </p>
 * 
 * All preferences saved in a hash map.  To make a new preference,
 * simply define a default, a setter and a getter.
 * 
 * @author Forge
 * @version $Id$
 */
public class ForgePreferences {
    private String fileName = "forge.preferences";
    private Map<String, String> prefs;

    /**
     * Initializes default values for preferences and sets a file address.
     * 
     * Note: preferences must be loaded separately.
     * 
     * @param s0 &emsp; File address string
     */
    public ForgePreferences(String s0) {
        this.fileName = s0;
        File f = new File(fileName);

        if (!f.exists()) { makeNew(); }
        else { load(); }

        // Test for existence of old file
        if (prefs.get("ui.use.skin") == null) {
            makeNew();
        }
    }

    private void makeNew() {
        prefs = new HashMap<String, String>();
        //========== Default values for all preferences:
        // UI preferences
        prefs.put("ui.use.old", "false");
        prefs.put("ui.random.foil", "false");
        prefs.put("ui.layout.params", "");
        prefs.put("ui.smooth.land", "false");
        prefs.put("ui.use.skin", "default");
        prefs.put("ui.card.overlay", "true");
        prefs.put("ui.upload.draft", "false");
        prefs.put("ui.scale.larger", "false");
        prefs.put("ui.max.stack", "3");
        prefs.put("ui.stack.offset", "tiny");
        prefs.put("ui.card.size", "small");
        prefs.put("ui.bugz.name", "");
        prefs.put("ui.bugz.pwd", "");
        prefs.put("ui.ante", "false");

        // Devmode preferences
        prefs.put("dev.mode", "false");
        prefs.put("dev.hand.view", "false");
        prefs.put("dev.library.view", "false");
        prefs.put("dev.milling.loss", "true");
        prefs.put("dev.unlimited.land", "false");

        //Deck generation preferences
        prefs.put("deckgen.singletons", "false");
        prefs.put("deckgen.artifacts", "false");
        prefs.put("deckgen.nosmall", "false");

        // Phase toggle preferences
        prefs.put("phase.ai.upkeep", "true");
        prefs.put("phase.ai.draw", "true");
        prefs.put("phase.ai.main1", "true");
        prefs.put("phase.ai.beginCombat", "true");
        prefs.put("phase.ai.declareAttackers", "true");
        prefs.put("phase.ai.declareBlockers", "true");
        prefs.put("phase.ai.firstStrike", "true");
        prefs.put("phase.ai.combatDamage", "true");
        prefs.put("phase.ai.endCombat", "true");
        prefs.put("phase.ai.main2", "true");
        prefs.put("phase.ai.eot", "true");
        prefs.put("phase.ai.cleanup", "true");

        prefs.put("phase.human.upkeep", "true");
        prefs.put("phase.human.draw", "true");
        prefs.put("phase.human.main1", "true");
        prefs.put("phase.human.beginCombat", "true");
        prefs.put("phase.human.declareAttackers", "true");
        prefs.put("phase.human.declareBlockers", "true");
        prefs.put("phase.human.firstStrike", "true");
        prefs.put("phase.human.combatDamage", "true");
        prefs.put("phase.human.endCombat", "true");
        prefs.put("phase.human.main2", "true");
        prefs.put("phase.human.eot", "true");
        prefs.put("phase.human.cleanup", "true");

        // Keyboard shortcuts
        prefs.put("shortcut.showstack", "83");
        prefs.put("shortcut.showcombat", "67");
        prefs.put("shortcut.showconsole", "76");
        prefs.put("shortcut.showplayers", "80");
        prefs.put("shortcut.showdev", "68");
        prefs.put("shortcut.concede", "27");
        prefs.put("shortcut.showpicture", "17 80");
        prefs.put("shortcut.showdetail", "17 68");

        save();
    }
    //========== File handling

    /** Loads preferences from file into prefs map. */
    public void load() {
        BufferedReader reader = null;

        try {
            String line = null;
            reader = new BufferedReader(new FileReader(fileName));
            prefs = new HashMap<String, String>();
            while ((line = reader.readLine()) != null) {
                String[] pair = line.split("=");
                if (pair.length != 2) {
                    continue;
                }
                prefs.put(pair[0], pair[1]);
            }
            reader.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** Saves prefs map to file. */
    public void save() {
        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(fileName));
            TreeSet<String> keys = new TreeSet<String>(prefs.keySet());
            for (String key : keys) {
                writer.write(key + "=" + prefs.get(key));
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

    //========== Enums
    /** */
    public static enum CardSizeType {
        /** */
        tiny, smaller, small, medium, large, huge
    }

    /** */
    public static enum StackOffsetType {
        /** */
        tiny, small, medium, large
    }

    //========== Setters / getters: UI prefs

    /** @return boolean */
    public boolean isStackAiLand() {
        return Boolean.parseBoolean(prefs.get("ui.smooth.land"));
    }

    /** @param b0 &emsp; boolean */
    public void setStackAiLand(boolean b0) {
        prefs.put("ui.smooth.land", Boolean.toString(b0));
    }

    /** @return boolean */
    public boolean isOldGui() {
        return Boolean.parseBoolean(prefs.get("ui.use.old"));
    }

    /** @param b0 &emsp; boolean */
    public void setOldGui(boolean b0) {
        prefs.put("ui.use.old", Boolean.toString(b0));
    }

    /** @return String of six values, comma delimited */
    public String getUILayout() {
        return prefs.get("ui.layout.params");
    }

    /** @param s0 &emsp; String of six values, comma delimited */
    public void setUILayout(String s0) {
        prefs.put("ui.layout.params", s0);
    }

    /** @return boolean */
    public boolean isUploadDraftAI() {
        return Boolean.parseBoolean(prefs.get("ui.upload.draft"));
    }

    /** @param b0 &emsp; boolean */
    public void setUploadDraftAI(boolean b0) {
        prefs.put("ui.upload.draft", Boolean.toString(b0));
    }

    /** @return the randCFoil */
    public boolean isRandCFoil() {
        return Boolean.parseBoolean(prefs.get("ui.random.foil"));
    }

    /** @param b0 &emsp; boolean */
    public void setRandCFoil(boolean b0) {
        prefs.put("ui.random.foil", Boolean.toString(b0));
    }

    /** @param skin0 &emsp; String skin name*/
    public void setSkin(String skin0) {
        prefs.put("ui.use.skin", skin0);
    }

    /** @return boolean*/
    public boolean isCardOverlay() {
        return Boolean.parseBoolean(prefs.get("ui.card.overlay"));
    }

    /** @param b0 &emsp; boolean */
    public void setCardOverlay(boolean b0) {
        prefs.put("ui.card.overlay", Boolean.toString(b0));
    }

    /** @return int */
    public int getMaxStackSize() {
        return Integer.parseInt(prefs.get("ui.max.stack"));
    }

    /** @param i0 &emsp; int, max stack size */
    public void setMaxStackSize(int i0) {
        prefs.put("ui.max.stack", Integer.toString(i0));
    }

    /** @return boolean */
    public boolean isScaleLargerThanOriginal() {
        return Boolean.parseBoolean(prefs.get("ui.scale.larger"));
    }

    /** @param b0 boolean */
    public void setScaleLargerThanOriginal(boolean b0) {
        prefs.put("ui.scale.larger", Boolean.toString(b0));
    }

    /** @return boolean */
    public boolean isPlayForAnte() {
        return Boolean.parseBoolean(prefs.get("ui.ante"));
    }

    /** @param b0 boolean */
    public void setPlayForAnte(boolean b0) {
        prefs.put("ui.ante", Boolean.toString(b0));
    }

    //========== Setters / getters: Dev mode prefs

    /** @return boolean */
    public boolean isMillingLossCondition() {
        return Boolean.parseBoolean(prefs.get("dev.milling.loss"));
    }

    /** @param b0 &emsp; boolean */
    public void setMillingLossCondition(boolean b0) {
        prefs.put("dev.milling.loss", Boolean.toString(b0));
    }

    /** @return boolean */
    public boolean getHandView() {
        return Boolean.parseBoolean(prefs.get("dev.hand.view"));
    }

    /** @param b0 &emsp; boolean */
    public void setHandView(boolean b0) {
        prefs.put("dev.hand.view", Boolean.toString(b0));
    }

    /** @return boolean */
    public boolean getLibraryView() {
        return Boolean.parseBoolean(prefs.get("dev.library.view"));
    }

    /**  @param b0 &emsp; boolean */
    public void setLibraryView(boolean b0) {
        prefs.put("dev.library.view", Boolean.toString(b0));
    }

    /** @return boolean */
    public boolean getUnlimitedLand() {
        return Boolean.parseBoolean(prefs.get("dev.unlimited.land"));
    }

    /** @param b0 &emsp; boolean */
    public void setUnlimitedLand(boolean b0) {
        prefs.put("dev.unlimited.land", Boolean.toString(b0));
    }

    /** @return boolean */
    public boolean isDeveloperMode() {
        return Boolean.parseBoolean(prefs.get("dev.mode"));
    }

    /** @param b0 &emsp; boolean */
    public void setDeveloperMode(boolean b0) {
        prefs.put("dev.mode", Boolean.toString(b0));
    }

    /** @return CardSizeType */
    public CardSizeType getCardSize() {
        return CardSizeType.valueOf(prefs.get("ui.card.size"));
    }

    /** @param cst0 &emsp; CardSizeType */
    public void setCardSize(CardSizeType cst0) {
        prefs.put("ui.card.size", cst0.toString());
    }

    /** @return StackOffsetType */
    public StackOffsetType getStackOffset() {
        return StackOffsetType.valueOf(prefs.get("ui.stack.offset"));
    }

    /** @param sot0 &emsp; StackOffsetType */
    public void setStackOffset(StackOffsetType sot0) {
        prefs.put("ui.stack.offset", sot0.toString());
    }

    /** @return String skin name */
    public String getSkin() {
        return prefs.get("ui.use.skin");
    }

    /** @return String */
    public String getBugzName() {
        return prefs.get("ui.bugz.name");
    }

    /** @param s0 &emsp; String bugzName */
    public void setBugzName(String s0) {
        prefs.put("ui.bugz.name", s0);
    }

    /** @return String */
    public String getBugzPwd() {
        return prefs.get("ui.bugz.pwd");
    }

    /**  @param s0 &emsp; String password */
    public void setBugzPwd(String s0) {
        prefs.put("ui.bugz.pwd", s0);
    }

    //========== Setters / getters: Deck generation prefs

    /** @return boolean */
    public boolean isDeckGenSingletons() {
        return Boolean.parseBoolean(prefs.get("deckgen.singletons"));
    }

    /** @param b0 &emsp; boolean */
    public void setDeckGenSingletons(boolean b0) {
        prefs.put("deckgen.singletons", Boolean.toString(b0));
    }

    /** @return boolean */
    public boolean isDeckGenRmvArtifacts() {
        return Boolean.parseBoolean(prefs.get("deckgen.artifacts"));
    }

    /** @param b0 &emsp; boolean */
    public void setDeckGenRmvArtifacts(boolean b0) {
        prefs.put("deckgen.artifacts", Boolean.toString(b0));
    }

    /** @return boolean */
    public boolean isDeckGenRmvSmall() {
        return Boolean.parseBoolean(prefs.get("deckgen.nosmall"));
    }

    /** @param b0 &emsp; boolean */
    public void setDeckGenRmvSmall(boolean b0) {
        prefs.put("deckgen.nosmall", Boolean.toString(b0));
    }

    //========== Phase and shortcut setter/getter

    /** 
     * Prints exception stack trace if phase name is not in the list.
     * @param s0 &emsp; String phase name
     * @return boolean
     */
    public boolean isShowPhase(String s0) {
        if (prefs.get(s0) != null) {
            return Boolean.parseBoolean(prefs.get(s0));
        }
        else {
            Exception ex = new Exception();
            ex.printStackTrace();
            return true;
        }
    }

    /** 
     * Prints exception stack trace if phase name is not in the list.
     * @param s0 &emsp; String phase name
     * @param b0 &emsp; boolean
     */
    public void setShowPhase(String s0, boolean b0) {
        if (prefs.get(s0) != null) {
            prefs.put(s0, Boolean.toString(b0));
        }
        else {
            Exception ex = new Exception();
            ex.printStackTrace();
        }
    }

    /** 
     * Prints exception stack trace if shortcut is not in the list.
     * @param s0 &emsp; String shortcut key code(s)
     * @return boolean
     */
    public String getKeyboardShortcut(String s0) {
        if (prefs.get(s0) != null) {
            return prefs.get(s0);
        }
        else {
            Exception ex = new Exception();
            ex.printStackTrace();
            return null;
        }
    }

    /** 
     * Prints exception stack trace if shortcut is not in the list.
     * @param s0 &emsp; String shortcut name
     * @param s1 &emsp; String shortcut key code(s)
     */
    public void setKeyboardShortcut(String s0, String s1) {
        if (prefs.get(s0) != null) {
            prefs.put(s0, s1);
        }
        else {
            Exception ex = new Exception();
            ex.printStackTrace();
        }
    }
}
