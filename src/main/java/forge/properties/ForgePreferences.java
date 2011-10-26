package forge.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// TODO: Auto-generated Javadoc
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
    public String BugzName;

    /** The Bugz pwd. */
    public String BugzPwd;

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

    private List<SavePreferencesListener> saveListeners = new ArrayList<SavePreferencesListener>();
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
        File f = new File(fileName);
        if (!f.exists()) {
            f.createNewFile();
        }
        try {
            FileInputStream stream = new FileInputStream(fileName);
            load(stream);
            stream.close();
        } catch (FileNotFoundException ex) {
            throw new Exception("File not found: \"" + fileName + "\".", ex);
        } catch (IOException ex) {
            throw new Exception("Error reading \"" + fileName + "\".", ex);
        }

        newGui = getBoolean("gui.new", true);
        stackAiLand = getBoolean("AI.stack.land", false);
        millingLossCondition = getBoolean("loss.condition.milling", true);
        developerMode = getBoolean("developer.mode", false);

        uploadDraftAI = getBoolean("upload.Draft.AI", true);

        randCFoil = getBoolean("rand.C.Foil", true);

        laf = get("gui.laf", "");
        lafFonts = getBoolean("gui.laf.fonts", false);
        skin = get("gui.skin", "default");

        cardOverlay = getBoolean("card.overlay", true);
        cardSize = CardSizeType.valueOf(get("card.images.size", "medium"));
        stackOffset = StackOffsetType.valueOf(get("stack.offset", "tiny"));
        maxStackSize = getInt("stack.max.size", 3);
        scaleLargerThanOriginal = getBoolean("card.scale.larger.than.original", true);

        deckGenRmvArtifacts = getBoolean("deck.gen.rmv.artifacts", false);
        deckGenRmvSmall = getBoolean("deck.gen.rmv.small", false);

        BugzName = get("bugz.user.name", "");
        BugzPwd = get("bugz.user.pwd", "");

        // Stop at Phases
        bAIUpkeep = getBoolean("phase.ai.upkeep", true);
        bAIDraw = getBoolean("phase.ai.draw", true);
        bAIEOT = getBoolean("phase.ai.eot", true);
        bAIBeginCombat = getBoolean("phase.ai.beginCombat", true);
        bAIEndCombat = getBoolean("phase.ai.endCombat", true);
        bHumanUpkeep = getBoolean("phase.human.upkeep", true);
        bHumanDraw = getBoolean("phase.human.draw", true);
        bHumanEOT = getBoolean("phase.human.eot", true);
        bHumanBeginCombat = getBoolean("phase.human.beginCombat", true);
        bHumanEndCombat = getBoolean("phase.human.endCombat", true);
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

        set("gui.new", newGui);

        set("AI.stack.land", stackAiLand);
        set("loss.condition.milling", millingLossCondition);
        set("developer.mode", developerMode);
        set("upload.Draft.AI", uploadDraftAI);

        set("rand.C.Foil", randCFoil);

        set("gui.skin", skin);
        set("gui.laf", laf);
        set("gui.laf.fonts", lafFonts);

        set("card.overlay", cardOverlay);
        set("card.images.size", cardSize);
        set("stack.offset", stackOffset);
        set("stack.max.size", maxStackSize);
        set("card.scale.larger.than.original", scaleLargerThanOriginal);
        for (SavePreferencesListener listeners : saveListeners) {
            listeners.savePreferences();
        }

        set("deck.gen.rmv.artifacts", deckGenRmvArtifacts);
        set("deck.gen.rmv.small", deckGenRmvSmall);

        set("bugz.user.name", BugzName);
        set("bugz.user.pwd", BugzPwd);

        set("phase.ai.upkeep", bAIUpkeep);
        set("phase.ai.draw", bAIDraw);
        set("phase.ai.eot", bAIEOT);
        set("phase.ai.beginCombat", bAIBeginCombat);
        set("phase.ai.endCombat", bAIEndCombat);
        set("phase.human.upkeep", bHumanUpkeep);
        set("phase.human.draw", bHumanDraw);
        set("phase.human.eot", bHumanEOT);
        set("phase.human.beginCombat", bHumanBeginCombat);
        set("phase.human.endCombat", bHumanEndCombat);

        try {
            FileOutputStream stream = new FileOutputStream(fileName);
            store(stream, "Forge");
            stream.close();
        } catch (IOException ex) {
            throw new Exception("Error saving \"" + fileName + "\".", ex);
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
        saveListeners.add(listener);
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
    static public enum StackOffsetType {

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
