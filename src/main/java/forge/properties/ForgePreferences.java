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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * ForgePreferences class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ForgePreferences extends Preferences {

    /** Old gui checkbox toggle. */
    private boolean oldGui;
    
    /** Play for ante checkbox toggle. */
    private boolean playForAnte;

    /** UI layout parameter string. */
    private String uiParams;

    /** The stack ai land. */
   private boolean stackAiLand;

    /** The milling loss condition. */
    private boolean millingLossCondition;

    /** Hand view toggle. */
    private boolean handView;

    /** Library view toggle. */
    private boolean libraryView;

    /** Unlimited land toggle. */
    private boolean unlimitedLand;

    /** Developer mode. */
    private boolean developerMode;

    /** The upload draft ai. */
    private boolean uploadDraftAI;

    /** The rand c foil. */
    private boolean randCFoil;

    /** The skin. */
    private String skin;

    /** The stack offset. */
    private StackOffsetType stackOffset;

    /** The max stack size. */
    private int maxStackSize;

    /** The card size. */
    private CardSizeType cardSize;

    /** The card overlay. */
    private boolean cardOverlay;

    /** The scale larger than original. */
    private boolean scaleLargerThanOriginal;

    /** The deck gen rmv artifacts. */
    private boolean deckGenSingletons;

    /** The deck gen rmv artifacts. */
    private boolean deckGenRmvArtifacts;

    /** The deck gen rmv small. */
    private boolean deckGenRmvSmall;

    /** The Bugz name. */
    private String bugzName;

    /** The Bugz pwd. */
    private String bugzPwd;

    private Map<String, boolean[]> aiPhases = new HashMap<String, boolean[]>();
    private Map<String, boolean[]> humanPhases = new HashMap<String, boolean[]>();

    // Keyboard shortcuts
    private String showStackShortcut;
    private String showCombatShortcut;
    private String showConsoleShortcut;
    private String showPlayersShortcut;
    private String showDevShortcut;
    private String concedeShortcut;
    private String showPictureShortcut;
    private String showDetailShortcut;

    //
    private final List<SavePreferencesListener> saveListeners = new ArrayList<SavePreferencesListener>();
    private final String fileName;

    /**
     * <p>
     * Constructor for ForgePreferences.
     * </p>
     * 
     * @param fileName
     *            a {@link java.lang.String} object.
     * @throws Exception
     *             the exception
     */
    public ForgePreferences(final String fileName) throws Exception {
        this.fileName = fileName;
        final File f = new File(fileName);
        if (!f.exists()) {
            f.createNewFile();
        }
        try {
            final FileInputStream stream = new FileInputStream(fileName);
            this.load(stream);
            stream.close();
        } catch (final FileNotFoundException ex) {
            throw new Exception("File not found: \"" + fileName + "\".", ex);
        } catch (final IOException ex) {
            throw new Exception("Error reading \"" + fileName + "\".", ex);
        }

        this.oldGui = this.getBoolean("gui.old", true);
        this.setUILayout(this.get("gui.layout", ""));
        this.setStackAiLand(this.getBoolean("AI.stack.land", false));
        this.setMillingLossCondition(this.getBoolean("loss.condition.milling", true));
        this.setHandView(this.getBoolean("developer.handview", true));
        this.setLibraryView(this.getBoolean("developer.libraryview", true));
        this.setUnlimitedLand(this.getBoolean("developer.unlimitedland", true));
        this.setDeveloperMode(this.getBoolean("developer.mode", false));

        this.setUploadDraftAI(this.getBoolean("upload.Draft.AI", true));

        this.setRandCFoil(this.getBoolean("rand.C.Foil", true));

        this.setSkin(this.get("gui.skin", "default"));

        this.setCardOverlay(this.getBoolean("card.overlay", true));
        this.setCardSize(CardSizeType.valueOf(this.get("card.images.size", "small")));
        this.setStackOffset(StackOffsetType.valueOf(this.get("stack.offset", "tiny")));
        this.setMaxStackSize(this.getInt("stack.max.size", 3));
        this.setScaleLargerThanOriginal(this.getBoolean("card.scale.larger.than.original", true));

        this.setDeckGenSingletons(this.getBoolean("deck.gen.singletons", false));
        this.setDeckGenRmvArtifacts(this.getBoolean("deck.gen.rmv.artifacts", false));
        this.setDeckGenRmvSmall(this.getBoolean("deck.gen.rmv.small", false));

        this.setBugzName(this.get("bugz.user.name", ""));
        this.setBugzPwd(this.get("bugz.user.pwd", ""));

        // Default values for phase stop
        this.setAIPhase("phase.ai.upkeep", this.getBoolean("phase.ai.upkeep", true));
        this.setAIPhase("phase.ai.draw", this.getBoolean("phase.ai.draw", true));
        this.setAIPhase("phase.ai.main1", this.getBoolean("phase.ai.main1", true));
        this.setAIPhase("phase.ai.beginCombat", this.getBoolean("phase.ai.beginCombat", true));
        this.setAIPhase("phase.ai.declareAttackers", this.getBoolean("phase.ai.declareAttackers", true));
        this.setAIPhase("phase.ai.declareBlockers", this.getBoolean("phase.ai.declareBlockers", true));
        this.setAIPhase("phase.ai.firstStrike", this.getBoolean("phase.ai.firstStrike", true));
        this.setAIPhase("phase.ai.combatDamage", this.getBoolean("phase.ai.combatDamage", true));
        this.setAIPhase("phase.ai.endCombat", this.getBoolean("phase.ai.endCombat", true));
        this.setAIPhase("phase.ai.main2", this.getBoolean("phase.ai.main2", true));
        this.setAIPhase("phase.ai.eot", this.getBoolean("phase.ai.eot", true));
        this.setAIPhase("phase.ai.cleanup", this.getBoolean("phase.ai.cleanup", true));

        this.setHumanPhase("phase.human.upkeep", this.getBoolean("phase.human.upkeep", true));
        this.setHumanPhase("phase.human.draw", this.getBoolean("phase.human.draw", true));
        this.setHumanPhase("phase.human.main1", this.getBoolean("phase.human.main1", true));
        this.setHumanPhase("phase.human.beginCombat", this.getBoolean("phase.human.beginCombat", true));
        this.setHumanPhase("phase.human.declareAttackers", this.getBoolean("phase.human.declareAttackers", true));
        this.setHumanPhase("phase.human.declareBlockers", this.getBoolean("phase.human.declareBlockers", true));
        this.setHumanPhase("phase.human.firstStrike", this.getBoolean("phase.human.firstStrike", true));
        this.setHumanPhase("phase.human.combatDamage", this.getBoolean("phase.human.combatDamage", true));
        this.setHumanPhase("phase.human.endCombat", this.getBoolean("phase.human.endCombat", true));
        this.setHumanPhase("phase.human.main2", this.getBoolean("phase.human.main2", true));
        this.setHumanPhase("phase.human.eot", this.getBoolean("phase.human.eot", true));
        this.setHumanPhase("phase.human.cleanup", this.getBoolean("phase.human.cleanup", true));

        // Keyboard shortcuts
        this.setShowStackShortcut(this.get("shortcut.showstack", "83"));
        this.setShowCombatShortcut(this.get("shortcut.showcombat", "67"));
        this.setShowConsoleShortcut(this.get("shortcut.showconsole", "76"));
        this.setShowPlayersShortcut(this.get("shortcut.showplayers", "80"));
        this.setShowDevShortcut(this.get("shortcut.showdev", "68"));
        this.setConcedeShortcut(this.get("shortcut.concede", "27"));
        this.setShowPictureShortcut(this.get("shortcut.showpicture", "17 80"));
        this.setShowDetailShortcut(this.get("shortcut.showdetail", "17 68"));
    }

    /**
     * <p>
     * save.
     * </p>
     * 
     * @throws Exception
     *             the exception
     */
    public final void save() throws Exception {
        this.set("gui.old", this.oldGui);
        this.set("gui.layout", this.getUILayout());

        this.set("AI.stack.land", this.isStackAiLand());
        this.set("loss.condition.milling", this.isMillingLossCondition());
        this.set("developer.handview", this.getHandView());
        this.set("developer.libraryview", this.getLibraryView());
        this.set("developer.unlimitedland", this.getUnlimitedLand());
        this.set("developer.mode", this.isDeveloperMode());
        this.set("upload.Draft.AI", this.isUploadDraftAI());

        this.set("rand.C.Foil", this.isRandCFoil());

        this.set("gui.skin", this.getSkin());

        this.set("card.overlay", this.isCardOverlay());
        this.set("card.images.size", this.getCardSize());
        this.set("stack.offset", this.getStackOffset());
        this.set("stack.max.size", this.getMaxStackSize());
        this.set("card.scale.larger.than.original", this.isScaleLargerThanOriginal());
        for (final SavePreferencesListener listeners : this.saveListeners) {
            listeners.savePreferences();
        }

        this.set("deck.gen.singletons", this.isDeckGenSingletons());
        this.set("deck.gen.rmv.artifacts", this.isDeckGenRmvArtifacts());
        this.set("deck.gen.rmv.small", this.isDeckGenRmvSmall());

        this.set("bugz.user.name", this.getBugzName());
        this.set("bugz.user.pwd", this.getBugzPwd());

        this.set("phase.ai.upkeep", isAIPhase("phase.ai.upkeep"));
        this.set("phase.ai.draw", isAIPhase("phase.ai.draw"));
        this.set("phase.ai.main1", isAIPhase("phase.ai.main1"));
        this.set("phase.ai.beginCombat", isAIPhase("phase.ai.beginCombat"));
        this.set("phase.ai.declareAttackers", isAIPhase("phase.ai.declareAttackers"));
        this.set("phase.ai.declareBlockers", isAIPhase("phase.ai.declareBlockers"));
        this.set("phase.ai.firstStrike", isAIPhase("phase.ai.firstStrike"));
        this.set("phase.ai.combatDamage", isAIPhase("phase.ai.combatDamage"));
        this.set("phase.ai.endCombat", isAIPhase("phase.ai.endCombat"));
        this.set("phase.ai.main2", isAIPhase("phase.ai.main2"));
        this.set("phase.ai.eot", isAIPhase("phase.ai.eot"));
        this.set("phase.ai.cleanup", isAIPhase("phase.ai.cleanup"));

        this.set("phase.human.upkeep", isHumanPhase("phase.human.upkeep"));
        this.set("phase.human.draw", isHumanPhase("phase.human.draw"));
        this.set("phase.human.main1", isHumanPhase("phase.human.main1"));
        this.set("phase.human.beginCombat", isHumanPhase("phase.human.beginCombat"));
        this.set("phase.human.declareAttackers", isHumanPhase("phase.human.declareAttackers"));
        this.set("phase.human.declareBlockers", isHumanPhase("phase.human.declareBlockers"));
        this.set("phase.human.firstStrike", isHumanPhase("phase.human.firstStrike"));
        this.set("phase.human.combatDamage", isHumanPhase("phase.human.combatDamage"));
        this.set("phase.human.endCombat", isHumanPhase("phase.human.endCombat"));
        this.set("phase.human.main2", isHumanPhase("phase.human.main2"));
        this.set("phase.human.eot", isHumanPhase("phase.human.eot"));
        this.set("phase.human.cleanup", isHumanPhase("phase.human.cleanup"));

        // Keyboard shortcuts
        this.set("shortcut.showstack", this.getShowStackShortcut());
        this.set("shortcut.showcombat", this.getShowCombatShortcut());
        this.set("shortcut.showconsole", this.getShowConsoleShortcut());
        this.set("shortcut.showplayers", this.getShowPlayersShortcut());
        this.set("shortcut.showdev", this.getShowDevShortcut());
        this.set("shortcut.concede", this.getConcedeShortcut());
        this.set("shortcut.showpicture", this.getShowPictureShortcut());
        this.set("shortcut.showdetail", this.getShowDetailShortcut());

        try {
            final FileOutputStream stream = new FileOutputStream(this.fileName);
            this.store(stream, "Forge");
            stream.close();
        } catch (final IOException ex) {
            throw new Exception("Error saving \"" + this.fileName + "\".", ex);
        }
    }

    /**
     * <p>
     * addSaveListener.
     * </p>
     * 
     * @param listener
     *            a {@link forge.properties.SavePreferencesListener} object.
     */
    public final void addSaveListener(final SavePreferencesListener listener) {
        this.saveListeners.add(listener);
    }

    /**
     * Checks if is stack ai land.
     * 
     * @return boolean
     */
    public boolean isStackAiLand() {
        return this.stackAiLand;
    }

    /**
     * Sets the stack ai land.
     * 
     * @param b0
     *            &emsp; boolean
     */
    public void setStackAiLand(final boolean b0) {
        this.stackAiLand = b0;
    }

    /**
     * Checks if old gui is to be used.
     * 
     * @return boolean
     */
    public boolean isOldGui() {
        return this.oldGui;
    }

    /**
     * Sets if old gui is to be used.
     * 
     * @param b0
     *            &emsp; boolean
     */
    public void setOldGui(final boolean b0) {
        this.oldGui = b0;
    }

    /**
     * Checks if we are playing for ante.
     * 
     * @return boolean
     */
    public boolean isPlayForAnte() {
        return this.playForAnte;
    }

    /**
     * Sets if we are playing for ante.
     * 
     * @param b0
     *            &emsp; boolean
     */
    public void setPlayForAnte(final boolean b0) {
        this.playForAnte = b0;
    }

    /**
     * Gets resizing parameters for UI regions.
     * 
     * @return String of six values, comma delimited
     */
    public String getUILayout() {
        return this.uiParams;
    }

    /**
     * Sets resizing parameters for UI regions.
     * 
     * @param s0
     *            &emsp; String of six values, comma delimited
     */
    public void setUILayout(final String s0) {
        this.uiParams = s0;
    }

    /**
     * Checks if loss by milling is enabled.
     * 
     * @return boolean
     */
    public boolean isMillingLossCondition() {
        return this.millingLossCondition;
    }

    /**
     * Sets if loss by milling is enabled.
     * 
     * @param millingLossCondition0
     *            the millingLossCondition to set
     */
    public void setMillingLossCondition(final boolean millingLossCondition0) {
        this.millingLossCondition = millingLossCondition0;
    }

    /**
     * Determines if "view any hand" option in dev mode is enabled or not.
     * 
     * @return boolean
     */
    public boolean getHandView() {
        return this.handView;
    }

    /**
     * Determines if "view any hand" option in dev mode is enabled or not.
     * 
     * @param b0
     *            &emsp; boolean
     */
    public void setHandView(final boolean b0) {
        this.handView = b0;
    }

    /**
     * Determines if "view any library" option in dev mode is enabled or not.
     * 
     * @return boolean
     */
    public boolean getLibraryView() {
        return this.libraryView;
    }

    /**
     * Determines if "view any library" option in dev mode is enabled or not.
     * 
     * @param b0
     *            &emsp; boolean
     */
    public void setLibraryView(final boolean b0) {
        this.libraryView = b0;
    }

    /**
     * Determines if "unlimited land" option in dev mode is enabled or not.
     * 
     * @return boolean
     */
    public boolean getUnlimitedLand() {
        return this.unlimitedLand;
    }

    /**
     * Determines if "unlimited land" option in dev mode is enabled or not.
     * 
     * @param b0
     *            &emsp; boolean
     */
    public void setUnlimitedLand(final boolean b0) {
        this.unlimitedLand = b0;
    }

    /**
     * Sets the upload draft ai.
     *
     * @param b0 &emsp; boolean, update AI draft picks
     */
    public void setUploadDraftAI(final boolean b0) {
        this.uploadDraftAI = b0;
    }

    /**
     * Checks if is upload draft ai.
     *
     * @return boolean
     */
    public boolean isUploadDraftAI() {
        return this.uploadDraftAI;
    }

    /**
     * Gets the bugz name.
     * 
     * @return the bugzName
     */
    public String getBugzName() {
        return this.bugzName;
    }

    /**
     * Sets the bugz name.
     * 
     * @param bugzName0
     *            the bugzName to set
     */
    public void setBugzName(final String bugzName0) {
        this.bugzName = bugzName0;
    }

    /**
     * Gets the bugz pwd.
     * 
     * @return the bugzPwd
     */
    public String getBugzPwd() {
        return this.bugzPwd;
    }

    /**
     * Sets the bugz pwd.
     * 
     * @param bugzPwd0
     *            the bugzPwd to set
     */
    public void setBugzPwd(final String bugzPwd0) {
        this.bugzPwd = bugzPwd0;
    }

    /**
     * Checks if is rand c foil.
     * 
     * @return the randCFoil
     */
    public boolean isRandCFoil() {
        return this.randCFoil;
    }

    /**
     * Sets the rand c foil.
     * 
     * @param randCFoil0
     *            the randCFoil to set
     */
    public void setRandCFoil(final boolean randCFoil0) {
        this.randCFoil = randCFoil0;
    }

    /**
     * Checks if is developer mode.
     * 
     * @return the developerMode
     */
    public boolean isDeveloperMode() {
        return this.developerMode;
    }

    /**
     * Sets the developer mode.
     * 
     * @param developerMode0
     *            the developerMode to set
     */
    public void setDeveloperMode(final boolean developerMode0) {
        this.developerMode = developerMode0;
    }

    /**
     * Gets the skin.
     * 
     * @return the skin
     */
    public String getSkin() {
        return this.skin;
    }

    /**
     * Sets the skin.
     *
     * @param skin0 the new skin
     */
    public void setSkin(final String skin0) {
        this.skin = skin0;
    }

    /**
     * Checks if is scale larger than original.
     * 
     * @return the scaleLargerThanOriginal
     */
    public boolean isScaleLargerThanOriginal() {
        return this.scaleLargerThanOriginal;
    }

    /**
     * Sets the scale larger than original.
     *
     * @param scaleLargerThanOriginal0 the new scale larger than original
     */
    public void setScaleLargerThanOriginal(final boolean scaleLargerThanOriginal0) {
        this.scaleLargerThanOriginal = scaleLargerThanOriginal0;
    }

    /**
     * Checks if is card overlay.
     * 
     * @return the cardOverlay
     */
    public boolean isCardOverlay() {
        return this.cardOverlay;
    }

    /**
     * Sets the card overlay.
     * 
     * @param cardOverlay0
     *            the cardOverlay to set
     */
    public void setCardOverlay(final boolean cardOverlay0) {
        this.cardOverlay = cardOverlay0;
    }

    /**
     * Checks if is deck gen singletons.
     * 
     * @return true, if is deck gen singletons
     */
    public boolean isDeckGenSingletons() {
        return this.deckGenSingletons;
    }

    /**
     * Sets the deck gen singletons.
     * 
     * @param deckSingletons
     *            the new deck gen singletons
     */
    public void setDeckGenSingletons(final boolean deckSingletons) {
        this.deckGenSingletons = deckSingletons;
    }

    /**
     * Checks if is deck gen rmv artifacts.
     * 
     * @return the deckGenRmvArtifacts
     */
    public boolean isDeckGenRmvArtifacts() {
        return this.deckGenRmvArtifacts;
    }

    /**
     * Sets the deck gen rmv artifacts.
     * 
     * @param deckGenRmvArtifacts0
     *            the deckGenRmvArtifacts to set
     */
    public void setDeckGenRmvArtifacts(final boolean deckGenRmvArtifacts0) {
        this.deckGenRmvArtifacts = deckGenRmvArtifacts0;
    }

    /**
     * Checks if is deck gen rmv small.
     * 
     * @return the deckGenRmvSmall
     */
    public boolean isDeckGenRmvSmall() {
        return this.deckGenRmvSmall;
    }

    /**
     * Sets the deck gen rmv small.
     * 
     * @param deckGenRmvSmall0
     *            the deckGenRmvSmall to set
     */
    public void setDeckGenRmvSmall(final boolean deckGenRmvSmall0) {
        this.deckGenRmvSmall = deckGenRmvSmall0;
    }

    /**
     * Gets the card size.
     * 
     * @return the cardSize
     */
    public CardSizeType getCardSize() {
        return this.cardSize;
    }

    /**
     * Sets the card size.
     * 
     * @param cardSize0
     *            the cardSize to set
     */
    public void setCardSize(final CardSizeType cardSize0) {
        this.cardSize = cardSize0;
    }

    /**
     * Gets the stack offset.
     * 
     * @return the stackOffset
     */
    public StackOffsetType getStackOffset() {
        return this.stackOffset;
    }

    /**
     * Sets the stack offset.
     * 
     * @param stackOffset0
     *            the stackOffset to set
     */
    public void setStackOffset(final StackOffsetType stackOffset0) {
        this.stackOffset = stackOffset0;
    }

    /**
     * Gets the max stack size.
     * 
     * @return the maxStackSize
     */
    public int getMaxStackSize() {
        return this.maxStackSize;
    }

    /**
     * Sets the max stack size.
     * 
     * @param maxStackSize0
     *            the maxStackSize to set
     */
    public void setMaxStackSize(final int maxStackSize0) {
        this.maxStackSize = maxStackSize0;
    }

    /**
     * The Enum CardSizeType.
     */
    public static enum CardSizeType {

        /** The tiny. */
        tiny,
        /** The smaller. */
        smaller,
        /** The small. */
        small,
        /** The medium. */
        medium,
        /** The large. */
        large,
        /** The huge. */
        huge
    }

    /**
     * The Enum StackOffsetType.
     */
    public static enum StackOffsetType {

        /** The tiny. */
        tiny,
        /** The small. */
        small,
        /** The medium. */
        medium,
        /** The large. */
        large
    }

    // Keyboard shortcuts
    /**
     * Gets the show stack shortcut.
     * 
     * @return String &emsp; String of keycodes set for this shortcut, delimited
     *         with spaces.
     */
    public String getShowStackShortcut() {
        return this.showStackShortcut;
    }

    /**
     * Sets the show stack shortcut.
     * 
     * @param keycodes
     *            &emsp; String of keycodes to set for this shortcut, delimited
     *            with spaces.
     */
    public void setShowStackShortcut(final String keycodes) {
        this.showStackShortcut = keycodes;
    }

    /**
     * Gets the show combat shortcut.
     * 
     * @return String &emsp; String of keycodes set for this shortcut, delimited
     *         with spaces.
     */
    public String getShowCombatShortcut() {
        return this.showCombatShortcut;
    }

    /**
     * Sets the show combat shortcut.
     * 
     * @param keycodes
     *            &emsp; String of keycodes to set for this shortcut, delimited
     *            with spaces.
     */
    public void setShowCombatShortcut(final String keycodes) {
        this.showCombatShortcut = keycodes;
    }

    /**
     * Gets the show console shortcut.
     * 
     * @return String &emsp; String of keycodes set for this shortcut, delimited
     *         with spaces.
     */
    public String getShowConsoleShortcut() {
        return this.showConsoleShortcut;
    }

    /**
     * Sets the show console shortcut.
     * 
     * @param keycodes
     *            &emsp; String of keycodes to set for this shortcut, delimited
     *            with spaces.
     */
    public void setShowConsoleShortcut(final String keycodes) {
        this.showConsoleShortcut = keycodes;
    }

    /**
     * Gets the show players shortcut.
     * 
     * @return String &emsp; String of keycodes set for this shortcut, delimited
     *         with spaces.
     */
    public String getShowPlayersShortcut() {
        return this.showPlayersShortcut;
    }

    /**
     * Sets the show players shortcut.
     * 
     * @param keycodes
     *            &emsp; String of keycodes to set for this shortcut, delimited
     *            with spaces.
     */
    public void setShowPlayersShortcut(final String keycodes) {
        this.showPlayersShortcut = keycodes;
    }

    /**
     * Gets the show dev shortcut.
     * 
     * @return String &emsp; String of keycodes set for this shortcut, delimited
     *         with spaces.
     */
    public String getShowDevShortcut() {
        return this.showDevShortcut;
    }

    /**
     * Sets the show dev shortcut.
     * 
     * @param keycodes
     *            &emsp; String of keycodes to set for this shortcut, delimited
     *            with spaces.
     */
    public void setShowDevShortcut(final String keycodes) {
        this.showDevShortcut = keycodes;
    }

    /**
     * Gets the concede shortcut.
     * 
     * @return String &emsp; String of keycodes set for this shortcut, delimited
     *         with spaces.
     */
    public String getConcedeShortcut() {
        return this.concedeShortcut;
    }

    /**
     * Sets the concede shortcut.
     * 
     * @param keycodes
     *            &emsp; String of keycodes to set for this shortcut, delimited
     *            with spaces.
     */
    public void setConcedeShortcut(final String keycodes) {
        this.concedeShortcut = keycodes;
    }

    /**
     * Gets the show picture shortcut.
     * 
     * @return String &emsp; String of keycodes set for this shortcut, delimited
     *         with spaces.
     */
    public String getShowPictureShortcut() {
        return this.showPictureShortcut;
    }

    /**
     * Sets the show picture shortcut.
     * 
     * @param keycodes
     *            &emsp; String of keycodes to set for this shortcut, delimited
     *            with spaces.
     */
    public void setShowPictureShortcut(final String keycodes) {
        this.showPictureShortcut = keycodes;
    }

    /**
     * Gets the show detail shortcut.
     * 
     * @return String &emsp; String of keycodes set for this shortcut, delimited
     *         with spaces.
     */
    public String getShowDetailShortcut() {
        return this.showDetailShortcut;
    }

    /**
     * Sets the show detail shortcut.
     * 
     * @param keycodes
     *            &emsp; String of keycodes to set for this shortcut, delimited
     *            with spaces.
     */
    public void setShowDetailShortcut(final String keycodes) {
        this.showDetailShortcut = keycodes;
    }

    //========== Phase setter/getter
    /**
     * Sets the ai phase.
     *
     * @param s0 &emsp; String index of phase in aiPhase map.
     * @param b0 &emsp; boolean, stop at index or not
     */
    public void setAIPhase(String s0, boolean b0) {
        aiPhases.put(s0, new boolean[] {b0});
    }

    /**
     * Checks if is aI phase.
     *
     * @param s0 &emsp; String index of phase in aiPhase map.
     * @return boolean
     */
    public boolean isAIPhase(String s0) {
        return aiPhases.get(s0)[0];
    }

    /**
     * Sets the human phase.
     *
     * @param s0 &emsp; String index of phase in humanPhase map.
     * @param b0 &emsp; boolean, stop at index or not
     */
    public void setHumanPhase(String s0, boolean b0) {
        humanPhases.put(s0, new boolean[] {b0});
    }

    /**
     * Checks if is human phase.
     *
     * @param s0 &emsp; String index of phase in humanPhase map.
     * @return boolean
     */
    public boolean isHumanPhase(String s0) {
        return humanPhases.get(s0)[0];
    }
}
