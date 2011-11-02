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
    private boolean newGui;

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
     * @return the stackAiLand
     */
    public boolean isStackAiLand() {
        return stackAiLand;
    }

    /**
     * @param stackAiLand the stackAiLand to set
     */
    public void setStackAiLand(boolean stackAiLand) {
        this.stackAiLand = stackAiLand; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the millingLossCondition
     */
    public boolean isMillingLossCondition() {
        return millingLossCondition;
    }

    /**
     * @param millingLossCondition the millingLossCondition to set
     */
    public void setMillingLossCondition(boolean millingLossCondition) {
        this.millingLossCondition = millingLossCondition; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bAIBeginCombat
     */
    public boolean isbAIBeginCombat() {
        return bAIBeginCombat;
    }

    /**
     * @param bAIBeginCombat the bAIBeginCombat to set
     */
    public void setbAIBeginCombat(boolean bAIBeginCombat) {
        this.bAIBeginCombat = bAIBeginCombat; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bAIEndCombat
     */
    public boolean isbAIEndCombat() {
        return bAIEndCombat;
    }

    /**
     * @param bAIEndCombat the bAIEndCombat to set
     */
    public void setbAIEndCombat(boolean bAIEndCombat) {
        this.bAIEndCombat = bAIEndCombat; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bAIUpkeep
     */
    public boolean isbAIUpkeep() {
        return bAIUpkeep;
    }

    /**
     * @param bAIUpkeep the bAIUpkeep to set
     */
    public void setbAIUpkeep(boolean bAIUpkeep) {
        this.bAIUpkeep = bAIUpkeep; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bAIDraw
     */
    public boolean isbAIDraw() {
        return bAIDraw;
    }

    /**
     * @param bAIDraw the bAIDraw to set
     */
    public void setbAIDraw(boolean bAIDraw) {
        this.bAIDraw = bAIDraw; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bAIEOT
     */
    public boolean isbAIEOT() {
        return bAIEOT;
    }

    /**
     * @param bAIEOT the bAIEOT to set
     */
    public void setbAIEOT(boolean bAIEOT) {
        this.bAIEOT = bAIEOT; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bHumanBeginCombat
     */
    public boolean isbHumanBeginCombat() {
        return bHumanBeginCombat;
    }

    /**
     * @param bHumanBeginCombat the bHumanBeginCombat to set
     */
    public void setbHumanBeginCombat(boolean bHumanBeginCombat) {
        this.bHumanBeginCombat = bHumanBeginCombat; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bHumanDraw
     */
    public boolean isbHumanDraw() {
        return bHumanDraw;
    }

    /**
     * @param bHumanDraw the bHumanDraw to set
     */
    public void setbHumanDraw(boolean bHumanDraw) {
        this.bHumanDraw = bHumanDraw; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the uploadDraftAI
     */
    public boolean isUploadDraftAI() {
        return uploadDraftAI;
    }

    /**
     * @param uploadDraftAI the uploadDraftAI to set
     */
    public void setUploadDraftAI(boolean uploadDraftAI) {
        this.uploadDraftAI = uploadDraftAI; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bHumanEndCombat
     */
    public boolean isbHumanEndCombat() {
        return bHumanEndCombat;
    }

    /**
     * @param bHumanEndCombat the bHumanEndCombat to set
     */
    public void setbHumanEndCombat(boolean bHumanEndCombat) {
        this.bHumanEndCombat = bHumanEndCombat; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bHumanEOT
     */
    public boolean isbHumanEOT() {
        return bHumanEOT;
    }

    /**
     * @param bHumanEOT the bHumanEOT to set
     */
    public void setbHumanEOT(boolean bHumanEOT) {
        this.bHumanEOT = bHumanEOT; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bHumanUpkeep
     */
    public boolean isbHumanUpkeep() {
        return bHumanUpkeep;
    }

    /**
     * @param bHumanUpkeep the bHumanUpkeep to set
     */
    public void setbHumanUpkeep(boolean bHumanUpkeep) {
        this.bHumanUpkeep = bHumanUpkeep; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bugzName
     */
    public String getBugzName() {
        return bugzName;
    }

    /**
     * @param bugzName the bugzName to set
     */
    public void setBugzName(String bugzName) {
        this.bugzName = bugzName; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bugzPwd
     */
    public String getBugzPwd() {
        return bugzPwd;
    }

    /**
     * @param bugzPwd the bugzPwd to set
     */
    public void setBugzPwd(String bugzPwd) {
        this.bugzPwd = bugzPwd; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the randCFoil
     */
    public boolean isRandCFoil() {
        return randCFoil;
    }

    /**
     * @param randCFoil the randCFoil to set
     */
    public void setRandCFoil(boolean randCFoil) {
        this.randCFoil = randCFoil; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the developerMode
     */
    public boolean isDeveloperMode() {
        return developerMode;
    }

    /**
     * @param developerMode the developerMode to set
     */
    public void setDeveloperMode(boolean developerMode) {
        this.developerMode = developerMode; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the laf
     */
    public String getLaf() {
        return laf;
    }

    /**
     * @param laf the laf to set
     */
    public void setLaf(String laf) {
        this.laf = laf; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the skin
     */
    public String getSkin() {
        return skin;
    }

    /**
     * @param skin the skin to set
     */
    public void setSkin(String skin) {
        this.skin = skin; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the lafFonts
     */
    public boolean isLafFonts() {
        return lafFonts;
    }

    /**
     * @param lafFonts the lafFonts to set
     */
    public void setLafFonts(boolean lafFonts) {
        this.lafFonts = lafFonts; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the scaleLargerThanOriginal
     */
    public boolean isScaleLargerThanOriginal() {
        return scaleLargerThanOriginal;
    }

    /**
     * @param scaleLargerThanOriginal the scaleLargerThanOriginal to set
     */
    public void setScaleLargerThanOriginal(boolean scaleLargerThanOriginal) {
        this.scaleLargerThanOriginal = scaleLargerThanOriginal; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the cardOverlay
     */
    public boolean isCardOverlay() {
        return cardOverlay;
    }

    /**
     * @param cardOverlay the cardOverlay to set
     */
    public void setCardOverlay(boolean cardOverlay) {
        this.cardOverlay = cardOverlay; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the deckGenRmvArtifacts
     */
    public boolean isDeckGenRmvArtifacts() {
        return deckGenRmvArtifacts;
    }

    /**
     * @param deckGenRmvArtifacts the deckGenRmvArtifacts to set
     */
    public void setDeckGenRmvArtifacts(boolean deckGenRmvArtifacts) {
        this.deckGenRmvArtifacts = deckGenRmvArtifacts; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the deckGenRmvSmall
     */
    public boolean isDeckGenRmvSmall() {
        return deckGenRmvSmall;
    }

    /**
     * @param deckGenRmvSmall the deckGenRmvSmall to set
     */
    public void setDeckGenRmvSmall(boolean deckGenRmvSmall) {
        this.deckGenRmvSmall = deckGenRmvSmall; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the cardSize
     */
    public CardSizeType getCardSize() {
        return cardSize;
    }

    /**
     * @param cardSize the cardSize to set
     */
    public void setCardSize(CardSizeType cardSize) {
        this.cardSize = cardSize; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the stackOffset
     */
    public StackOffsetType getStackOffset() {
        return stackOffset;
    }

    /**
     * @param stackOffset the stackOffset to set
     */
    public void setStackOffset(StackOffsetType stackOffset) {
        this.stackOffset = stackOffset; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the maxStackSize
     */
    public int getMaxStackSize() {
        return maxStackSize;
    }

    /**
     * @param maxStackSize the maxStackSize to set
     */
    public void setMaxStackSize(int maxStackSize) {
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
