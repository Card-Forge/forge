package forge.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * ForgePreferences class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class ForgePreferences extends Preferences {

    /** The new gui. */
    private final boolean newGui;

    /** The stack ai land. */
    private boolean stackAiLand;

    /** The milling loss condition. */
    private boolean millingLossCondition;

    /** The developer mode. */
    private boolean developerMode;

    /** The upload draft ai. */
    private boolean uploadDraftAI;

    /** The rand c foil. */
    private boolean randCFoil;

    /** The skin. */
    private String skin;

    /** The laf. */
    private String laf;

    /** The laf fonts. */
    private boolean lafFonts;

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

    // Phases
    /** The b ai upkeep. */
    private boolean bAIUpkeep;

    /** The b ai draw. */
    private boolean bAIDraw;

    /** The b aieot. */
    private boolean bAIEOT;

    /** The b ai begin combat. */
    private boolean bAIBeginCombat;

    /** The b ai end combat. */
    private boolean bAIEndCombat;

    /** The b human upkeep. */
    private boolean bHumanUpkeep;

    /** The b human draw. */
    private boolean bHumanDraw;

    /** The b human eot. */
    private boolean bHumanEOT;

    /** The b human begin combat. */
    private boolean bHumanBeginCombat;

    /** The b human end combat. */
    private boolean bHumanEndCombat;

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

        this.newGui = this.getBoolean("gui.new", true);
        this.setStackAiLand(this.getBoolean("AI.stack.land", false));
        this.setMillingLossCondition(this.getBoolean("loss.condition.milling", true));
        this.setDeveloperMode(this.getBoolean("developer.mode", false));

        this.setUploadDraftAI(this.getBoolean("upload.Draft.AI", true));

        this.setRandCFoil(this.getBoolean("rand.C.Foil", true));

        this.setLaf(this.get("gui.laf", ""));
        this.setLafFonts(this.getBoolean("gui.laf.fonts", false));
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

        // Stop at Phases
        this.setbAIUpkeep(this.getBoolean("phase.ai.upkeep", true));
        this.setbAIDraw(this.getBoolean("phase.ai.draw", true));
        this.setbAIEOT(this.getBoolean("phase.ai.eot", true));
        this.setbAIBeginCombat(this.getBoolean("phase.ai.beginCombat", true));
        this.setbAIEndCombat(this.getBoolean("phase.ai.endCombat", true));
        this.setbHumanUpkeep(this.getBoolean("phase.human.upkeep", true));
        this.setbHumanDraw(this.getBoolean("phase.human.draw", true));
        this.setbHumanEOT(this.getBoolean("phase.human.eot", true));
        this.setbHumanBeginCombat(this.getBoolean("phase.human.beginCombat", true));
        this.setbHumanEndCombat(this.getBoolean("phase.human.endCombat", true));
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

        this.set("gui.new", this.newGui);

        this.set("AI.stack.land", this.isStackAiLand());
        this.set("loss.condition.milling", this.isMillingLossCondition());
        this.set("developer.mode", this.isDeveloperMode());
        this.set("upload.Draft.AI", this.isUploadDraftAI());

        this.set("rand.C.Foil", this.isRandCFoil());

        this.set("gui.skin", this.getSkin());
        this.set("gui.laf", this.getLaf());
        this.set("gui.laf.fonts", this.isLafFonts());

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

        this.set("phase.ai.upkeep", this.isbAIUpkeep());
        this.set("phase.ai.draw", this.isbAIDraw());
        this.set("phase.ai.eot", this.isbAIEOT());
        this.set("phase.ai.beginCombat", this.isbAIBeginCombat());
        this.set("phase.ai.endCombat", this.isbAIEndCombat());
        this.set("phase.human.upkeep", this.isbHumanUpkeep());
        this.set("phase.human.draw", this.isbHumanDraw());
        this.set("phase.human.eot", this.isbHumanEOT());
        this.set("phase.human.beginCombat", this.isbHumanBeginCombat());
        this.set("phase.human.endCombat", this.isbHumanEndCombat());

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
     * @return the stackAiLand
     */
    public boolean isStackAiLand() {
        return this.stackAiLand;
    }

    /**
     * Sets the stack ai land.
     *
     * @param stackAiLand the stackAiLand to set
     */
    public void setStackAiLand(final boolean stackAiLand) {
        this.stackAiLand = stackAiLand; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is milling loss condition.
     *
     * @return the millingLossCondition
     */
    public boolean isMillingLossCondition() {
        return this.millingLossCondition;
    }

    /**
     * Sets the milling loss condition.
     *
     * @param millingLossCondition the millingLossCondition to set
     */
    public void setMillingLossCondition(final boolean millingLossCondition) {
        this.millingLossCondition = millingLossCondition; // TODO: Add 0 to
                                                          // parameter's name.
    }

    /**
     * Checks if is b ai begin combat.
     *
     * @return the bAIBeginCombat
     */
    public boolean isbAIBeginCombat() {
        return this.bAIBeginCombat;
    }

    /**
     * Sets the b ai begin combat.
     *
     * @param bAIBeginCombat the bAIBeginCombat to set
     */
    public void setbAIBeginCombat(final boolean bAIBeginCombat) {
        this.bAIBeginCombat = bAIBeginCombat; // TODO: Add 0 to parameter's
                                              // name.
    }

    /**
     * Checks if is b ai end combat.
     *
     * @return the bAIEndCombat
     */
    public boolean isbAIEndCombat() {
        return this.bAIEndCombat;
    }

    /**
     * Sets the b ai end combat.
     *
     * @param bAIEndCombat the bAIEndCombat to set
     */
    public void setbAIEndCombat(final boolean bAIEndCombat) {
        this.bAIEndCombat = bAIEndCombat; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is b ai upkeep.
     *
     * @return the bAIUpkeep
     */
    public boolean isbAIUpkeep() {
        return this.bAIUpkeep;
    }

    /**
     * Sets the b ai upkeep.
     *
     * @param bAIUpkeep the bAIUpkeep to set
     */
    public void setbAIUpkeep(final boolean bAIUpkeep) {
        this.bAIUpkeep = bAIUpkeep; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is b ai draw.
     *
     * @return the bAIDraw
     */
    public boolean isbAIDraw() {
        return this.bAIDraw;
    }

    /**
     * Sets the b ai draw.
     *
     * @param bAIDraw the bAIDraw to set
     */
    public void setbAIDraw(final boolean bAIDraw) {
        this.bAIDraw = bAIDraw; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is b aieot.
     *
     * @return the bAIEOT
     */
    public boolean isbAIEOT() {
        return this.bAIEOT;
    }

    /**
     * Sets the b aieot.
     *
     * @param bAIEOT the bAIEOT to set
     */
    public void setbAIEOT(final boolean bAIEOT) {
        this.bAIEOT = bAIEOT; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is b human begin combat.
     *
     * @return the bHumanBeginCombat
     */
    public boolean isbHumanBeginCombat() {
        return this.bHumanBeginCombat;
    }

    /**
     * Sets the b human begin combat.
     *
     * @param bHumanBeginCombat the bHumanBeginCombat to set
     */
    public void setbHumanBeginCombat(final boolean bHumanBeginCombat) {
        this.bHumanBeginCombat = bHumanBeginCombat; // TODO: Add 0 to
                                                    // parameter's name.
    }

    /**
     * Checks if is b human draw.
     *
     * @return the bHumanDraw
     */
    public boolean isbHumanDraw() {
        return this.bHumanDraw;
    }

    /**
     * Sets the b human draw.
     *
     * @param bHumanDraw the bHumanDraw to set
     */
    public void setbHumanDraw(final boolean bHumanDraw) {
        this.bHumanDraw = bHumanDraw; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is upload draft ai.
     *
     * @return the uploadDraftAI
     */
    public boolean isUploadDraftAI() {
        return this.uploadDraftAI;
    }

    /**
     * Sets the upload draft ai.
     *
     * @param uploadDraftAI the uploadDraftAI to set
     */
    public void setUploadDraftAI(final boolean uploadDraftAI) {
        this.uploadDraftAI = uploadDraftAI; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is b human end combat.
     *
     * @return the bHumanEndCombat
     */
    public boolean isbHumanEndCombat() {
        return this.bHumanEndCombat;
    }

    /**
     * Sets the b human end combat.
     *
     * @param bHumanEndCombat the bHumanEndCombat to set
     */
    public void setbHumanEndCombat(final boolean bHumanEndCombat) {
        this.bHumanEndCombat = bHumanEndCombat; // TODO: Add 0 to parameter's
                                                // name.
    }

    /**
     * Checks if is b human eot.
     *
     * @return the bHumanEOT
     */
    public boolean isbHumanEOT() {
        return this.bHumanEOT;
    }

    /**
     * Sets the b human eot.
     *
     * @param bHumanEOT the bHumanEOT to set
     */
    public void setbHumanEOT(final boolean bHumanEOT) {
        this.bHumanEOT = bHumanEOT; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is b human upkeep.
     *
     * @return the bHumanUpkeep
     */
    public boolean isbHumanUpkeep() {
        return this.bHumanUpkeep;
    }

    /**
     * Sets the b human upkeep.
     *
     * @param bHumanUpkeep the bHumanUpkeep to set
     */
    public void setbHumanUpkeep(final boolean bHumanUpkeep) {
        this.bHumanUpkeep = bHumanUpkeep; // TODO: Add 0 to parameter's name.
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
     * @param bugzName the bugzName to set
     */
    public void setBugzName(final String bugzName) {
        this.bugzName = bugzName; // TODO: Add 0 to parameter's name.
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
     * @param bugzPwd the bugzPwd to set
     */
    public void setBugzPwd(final String bugzPwd) {
        this.bugzPwd = bugzPwd; // TODO: Add 0 to parameter's name.
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
     * @param randCFoil the randCFoil to set
     */
    public void setRandCFoil(final boolean randCFoil) {
        this.randCFoil = randCFoil; // TODO: Add 0 to parameter's name.
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
     * @param developerMode the developerMode to set
     */
    public void setDeveloperMode(final boolean developerMode) {
        this.developerMode = developerMode; // TODO: Add 0 to parameter's name.
    }

    /**
     * Gets the laf.
     *
     * @return the laf
     */
    public String getLaf() {
        return this.laf;
    }

    /**
     * Sets the laf.
     *
     * @param laf the laf to set
     */
    public void setLaf(final String laf) {
        this.laf = laf; // TODO: Add 0 to parameter's name.
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
     * @param skin the skin to set
     */
    public void setSkin(final String skin) {
        this.skin = skin; // TODO: Add 0 to parameter's name.
    }

    /**
     * Checks if is laf fonts.
     *
     * @return the lafFonts
     */
    public boolean isLafFonts() {
        return this.lafFonts;
    }

    /**
     * Sets the laf fonts.
     *
     * @param lafFonts the lafFonts to set
     */
    public void setLafFonts(final boolean lafFonts) {
        this.lafFonts = lafFonts; // TODO: Add 0 to parameter's name.
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
     * @param scaleLargerThanOriginal the scaleLargerThanOriginal to set
     */
    public void setScaleLargerThanOriginal(final boolean scaleLargerThanOriginal) {
        this.scaleLargerThanOriginal = scaleLargerThanOriginal; // TODO: Add 0
                                                                // to
                                                                // parameter's
                                                                // name.
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
     * @param cardOverlay the cardOverlay to set
     */
    public void setCardOverlay(final boolean cardOverlay) {
        this.cardOverlay = cardOverlay; // TODO: Add 0 to parameter's name.
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
     * @param deckSingletons the new deck gen singletons
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
     * @param deckGenRmvArtifacts the deckGenRmvArtifacts to set
     */
    public void setDeckGenRmvArtifacts(final boolean deckGenRmvArtifacts) {
        this.deckGenRmvArtifacts = deckGenRmvArtifacts; // TODO: Add 0 to
                                                        // parameter's name.
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
     * @param deckGenRmvSmall the deckGenRmvSmall to set
     */
    public void setDeckGenRmvSmall(final boolean deckGenRmvSmall) {
        this.deckGenRmvSmall = deckGenRmvSmall; // TODO: Add 0 to parameter's
                                                // name.
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
     * @param cardSize the cardSize to set
     */
    public void setCardSize(final CardSizeType cardSize) {
        this.cardSize = cardSize; // TODO: Add 0 to parameter's name.
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
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(final StackOffsetType stackOffset) {
        this.stackOffset = stackOffset; // TODO: Add 0 to parameter's name.
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
     * @param maxStackSize the maxStackSize to set
     */
    public void setMaxStackSize(final int maxStackSize) {
        this.maxStackSize = maxStackSize; // TODO: Add 0 to parameter's name.
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
}
