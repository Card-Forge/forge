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
    public boolean newGui;

    /** The stack ai land. */
    public boolean stackAiLand;

    /** The milling loss condition. */
    public boolean millingLossCondition;

    /** The developer mode. */
    public boolean developerMode;

    /** The upload draft ai. */
    public boolean uploadDraftAI;

    /** The rand c foil. */
    public boolean randCFoil;

    /** The skin. */
    public String skin;

    /** The laf. */
    public String laf;

    /** The laf fonts. */
    public boolean lafFonts;

    /** The stack offset. */
    public StackOffsetType stackOffset;

    /** The max stack size. */
    public int maxStackSize;

    /** The card size. */
    public CardSizeType cardSize;

    /** The card overlay. */
    public boolean cardOverlay;

    /** The scale larger than original. */
    public boolean scaleLargerThanOriginal;

    /** The deck gen rmv artifacts. */
    public boolean deckGenRmvArtifacts;

    /** The deck gen rmv small. */
    public boolean deckGenRmvSmall;

    /** The Bugz name. */
    public String bugzName;

    /** The Bugz pwd. */
    public String bugzPwd;

    // Phases
    /** The b ai upkeep. */
    public boolean bAIUpkeep;

    /** The b ai draw. */
    public boolean bAIDraw;

    /** The b aieot. */
    public boolean bAIEOT;

    /** The b ai begin combat. */
    public boolean bAIBeginCombat;

    /** The b ai end combat. */
    public boolean bAIEndCombat;

    /** The b human upkeep. */
    public boolean bHumanUpkeep;

    /** The b human draw. */
    public boolean bHumanDraw;

    /** The b human eot. */
    public boolean bHumanEOT;

    /** The b human begin combat. */
    public boolean bHumanBeginCombat;

    /** The b human end combat. */
    public boolean bHumanEndCombat;

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
        this.stackAiLand = this.getBoolean("AI.stack.land", false);
        this.millingLossCondition = this.getBoolean("loss.condition.milling", true);
        this.developerMode = this.getBoolean("developer.mode", false);

        this.uploadDraftAI = this.getBoolean("upload.Draft.AI", true);

        this.randCFoil = this.getBoolean("rand.C.Foil", true);

        this.laf = this.get("gui.laf", "");
        this.lafFonts = this.getBoolean("gui.laf.fonts", false);
        this.skin = this.get("gui.skin", "default");

        this.cardOverlay = this.getBoolean("card.overlay", true);
        this.cardSize = CardSizeType.valueOf(this.get("card.images.size", "medium"));
        this.stackOffset = StackOffsetType.valueOf(this.get("stack.offset", "tiny"));
        this.maxStackSize = this.getInt("stack.max.size", 3);
        this.scaleLargerThanOriginal = this.getBoolean("card.scale.larger.than.original", true);

        this.deckGenRmvArtifacts = this.getBoolean("deck.gen.rmv.artifacts", false);
        this.deckGenRmvSmall = this.getBoolean("deck.gen.rmv.small", false);

        this.bugzName = this.get("bugz.user.name", "");
        this.bugzPwd = this.get("bugz.user.pwd", "");

        // Stop at Phases
        this.bAIUpkeep = this.getBoolean("phase.ai.upkeep", true);
        this.bAIDraw = this.getBoolean("phase.ai.draw", true);
        this.bAIEOT = this.getBoolean("phase.ai.eot", true);
        this.bAIBeginCombat = this.getBoolean("phase.ai.beginCombat", true);
        this.bAIEndCombat = this.getBoolean("phase.ai.endCombat", true);
        this.bHumanUpkeep = this.getBoolean("phase.human.upkeep", true);
        this.bHumanDraw = this.getBoolean("phase.human.draw", true);
        this.bHumanEOT = this.getBoolean("phase.human.eot", true);
        this.bHumanBeginCombat = this.getBoolean("phase.human.beginCombat", true);
        this.bHumanEndCombat = this.getBoolean("phase.human.endCombat", true);
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

        this.set("AI.stack.land", this.stackAiLand);
        this.set("loss.condition.milling", this.millingLossCondition);
        this.set("developer.mode", this.developerMode);
        this.set("upload.Draft.AI", this.uploadDraftAI);

        this.set("rand.C.Foil", this.randCFoil);

        this.set("gui.skin", this.skin);
        this.set("gui.laf", this.laf);
        this.set("gui.laf.fonts", this.lafFonts);

        this.set("card.overlay", this.cardOverlay);
        this.set("card.images.size", this.cardSize);
        this.set("stack.offset", this.stackOffset);
        this.set("stack.max.size", this.maxStackSize);
        this.set("card.scale.larger.than.original", this.scaleLargerThanOriginal);
        for (final SavePreferencesListener listeners : this.saveListeners) {
            listeners.savePreferences();
        }

        this.set("deck.gen.rmv.artifacts", this.deckGenRmvArtifacts);
        this.set("deck.gen.rmv.small", this.deckGenRmvSmall);

        this.set("bugz.user.name", this.bugzName);
        this.set("bugz.user.pwd", this.bugzPwd);

        this.set("phase.ai.upkeep", this.bAIUpkeep);
        this.set("phase.ai.draw", this.bAIDraw);
        this.set("phase.ai.eot", this.bAIEOT);
        this.set("phase.ai.beginCombat", this.bAIBeginCombat);
        this.set("phase.ai.endCombat", this.bAIEndCombat);
        this.set("phase.human.upkeep", this.bHumanUpkeep);
        this.set("phase.human.draw", this.bHumanDraw);
        this.set("phase.human.eot", this.bHumanEOT);
        this.set("phase.human.beginCombat", this.bHumanBeginCombat);
        this.set("phase.human.endCombat", this.bHumanEndCombat);

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
