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
package forge.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;

import forge.AllZone;
import forge.Constant;
import forge.GameAction;
import forge.Singletons;
import forge.card.BoosterData;
import forge.card.CardBlock;
import forge.card.EditionCollection;
import forge.card.FatPackData;
import forge.card.FormatCollection;
import forge.control.input.InputControl;
import forge.deck.CardCollections;
import forge.game.GameState;
import forge.game.GameSummary;
import forge.game.player.ComputerAIGeneral;
import forge.game.player.ComputerAIInput;
import forge.gui.match.VMatchUI;
import forge.gui.match.nonsingleton.VField;
import forge.gui.match.views.VDev;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestPreferences;
import forge.util.FileUtil;
import forge.util.HttpUtil;
import forge.util.IStorageView;
import forge.util.MultiplexOutputStream;
import forge.util.StorageView;

/**
 * The default Model implementation for Forge.
 * 
 * This used to be an interface, but it seems unlikely that we will ever use a
 * different model.
 * 
 * In case we need to convert it into an interface in the future, all fields of
 * this class must be either private or public static final.
 */
public enum FModel {

    /** The SINGLETO n_ instance. */
    SINGLETON_INSTANCE;
    // private static final int NUM_INIT_PHASES = 1;

    private final PrintStream oldSystemOut;
    private final PrintStream oldSystemErr;
    private BuildInfo buildInfo;
    private OutputStream logFileStream;

    private final GameAction gameAction;
    private final QuestPreferences questPreferences;
    private final ForgePreferences preferences;
    private final GameState gameState;
    private final FMatchState matchState;

    private final EditionCollection editions;
    private final FormatCollection formats;
    private final IStorageView<BoosterData> boosters;
    private final IStorageView<BoosterData> tournaments;
    private final IStorageView<FatPackData> fatPacks;
    private final IStorageView<CardBlock> blocks;

    // have to implement lazy initialization - at the moment of FModel.ctor()
    // CardDb is not ready yet.
    private CardCollections decks;


    /**
     * Constructor.
     * 
     * @throws FileNotFoundException
     *             if we could not find or write to the log file.
     */
    private FModel() {
        // Fire up log file
        final File logFile = new File("forge.log");
        final boolean deleteSucceeded = logFile.delete();

        if (logFile.exists() && !deleteSucceeded && (logFile.length() != 0)) {
            throw new IllegalStateException("Could not delete existing logFile:" + logFile.getAbsolutePath());
        }

        try {
            this.logFileStream = new FileOutputStream(logFile);
        } catch (final FileNotFoundException e) {
            e.printStackTrace();
        }

        this.oldSystemOut = System.out;
        System.setOut(new PrintStream(new MultiplexOutputStream(System.out, this.logFileStream), true));
        this.oldSystemErr = System.err;
        System.setErr(new PrintStream(new MultiplexOutputStream(System.err, this.logFileStream), true));

        // Instantiate preferences: quest and regular
        try {
            this.preferences = new ForgePreferences();
        } catch (final Exception exn) {
            throw new RuntimeException(exn);
        }

        this.gameAction = new GameAction();
        this.gameState = new GameState();
        this.matchState = new FMatchState();
        this.questPreferences = new QuestPreferences();

        this.editions = new EditionCollection();
        this.formats = new FormatCollection("res/blockdata/formats.txt");
        this.boosters = new StorageView<BoosterData>(new BoosterData.Reader("res/blockdata/boosters.txt"));
        this.tournaments = new StorageView<BoosterData>(new BoosterData.Reader("res/blockdata/starters.txt"));
        this.fatPacks = new StorageView<FatPackData>(new FatPackData.Reader("res/blockdata/fatpacks.txt"));
        this.blocks = new StorageView<CardBlock>(new CardBlock.Reader("res/blockdata/blocks.txt", editions));

        // TODO this single setting from preferences should not be here, or,
        // it should be here with all the other settings at the same time.
        // Unfortunately, they're tied up in legacy code in the Display
        // interface,
        // currently in GuiTopLevel. When that code is updated, this TODO should
        // be resolved.
        // Doublestrike 24-01-12
        // ==
        // It's looking like all the settings at the same time, here only.
        // Doublestrike 06-02-12
        Constant.Runtime.DEV_MODE[0] = this.preferences.getPrefBoolean(FPref.DEV_MODE_ENABLED);
        Constant.Runtime.setSkinName(this.preferences.getPref(FPref.UI_SKIN));

        // Load splash image and preloader swatches for skin
        FSkin.loadLight(Constant.Runtime.getSkinName());

        // Instantiate AI
        AllZone.setInputControl(new InputControl(FModel.this));
        AllZone.getInputControl().setComputer(new ComputerAIInput(new ComputerAIGeneral()));

        // Set gameplay preferences and constants
        final HttpUtil pinger = new HttpUtil();
        final String url = ForgeProps.getProperty(NewConstants.CARDFORGE_URL) + "/draftAI/ping.php";
        Constant.Runtime.NET_CONN[0] = (pinger.getURL(url).equals("pong") ? true : false);

        this.setBuildInfo(new BuildInfo());
        FModel.loadDynamicGamedata();
    }

    /**
     * Load dynamic gamedata.
     */
    public static void loadDynamicGamedata() {
        if (!Constant.CardTypes.LOADED[0]) {
            final List<String> typeListFile = FileUtil.readFile("res/gamedata/TypeLists.txt");

            List<String> tList = null;

            if (typeListFile.size() > 0) {
                for (int i = 0; i < typeListFile.size(); i++) {
                    final String s = typeListFile.get(i);

                    if (s.equals("[CardTypes]")) {
                        tList = Constant.CardTypes.CARD_TYPES;
                    }

                    else if (s.equals("[SuperTypes]")) {
                        tList = Constant.CardTypes.SUPER_TYPES;
                    }

                    else if (s.equals("[BasicTypes]")) {
                        tList = Constant.CardTypes.BASIC_TYPES;
                    }

                    else if (s.equals("[LandTypes]")) {
                        tList = Constant.CardTypes.LAND_TYPES;
                    }

                    else if (s.equals("[CreatureTypes]")) {
                        tList = Constant.CardTypes.CREATURE_TYPES;
                    }

                    else if (s.equals("[InstantTypes]")) {
                        tList = Constant.CardTypes.INSTANT_TYPES;
                    }

                    else if (s.equals("[SorceryTypes]")) {
                        tList = Constant.CardTypes.SORCERY_TYPES;
                    }

                    else if (s.equals("[EnchantmentTypes]")) {
                        tList = Constant.CardTypes.ENCHANTMENT_TYPES;
                    }

                    else if (s.equals("[ArtifactTypes]")) {
                        tList = Constant.CardTypes.ARTIFACT_TYPES;
                    }

                    else if (s.equals("[WalkerTypes]")) {
                        tList = Constant.CardTypes.WALKER_TYPES;
                    }

                    else if (s.length() > 1) {
                        tList.add(s);
                    }
                }
            }
            Constant.CardTypes.LOADED[0] = true;
            /*
             * if (Constant.Runtime.DevMode[0]) {
             * System.out.println(Constant.CardTypes.cardTypes[0].list);
             * System.out.println(Constant.CardTypes.superTypes[0].list);
             * System.out.println(Constant.CardTypes.basicTypes[0].list);
             * System.out.println(Constant.CardTypes.landTypes[0].list);
             * System.out.println(Constant.CardTypes.creatureTypes[0].list);
             * System.out.println(Constant.CardTypes.instantTypes[0].list);
             * System.out.println(Constant.CardTypes.sorceryTypes[0].list);
             * System.out.println(Constant.CardTypes.enchantmentTypes[0].list);
             * System.out.println(Constant.CardTypes.artifactTypes[0].list);
             * System.out.println(Constant.CardTypes.walkerTypes[0].list); }
             */
        }

        if (!Constant.Keywords.LOADED[0]) {
            final List<String> nskwListFile = FileUtil.readFile("res/gamedata/NonStackingKWList.txt");


            if (nskwListFile.size() > 1) {
                for (int i = 0; i < nskwListFile.size(); i++) {
                    final String s = nskwListFile.get(i);
                    if (s.length() > 1) {
                        Constant.Keywords.NON_STACKING_LIST.add(s);
                    }
                }
            }
            Constant.Keywords.LOADED[0] = true;
            /*
             * if (Constant.Runtime.DevMode[0]) {
             * System.out.println(Constant.Keywords.NonStackingList[0].list); }
             */
        }

        /*
         * if (!Constant.Color.loaded[0]) { ArrayList<String> lcListFile =
         * FileUtil.readFile("res/gamedata/LandColorList");
         * 
         * if (lcListFile.size() > 1) { for (int i=0; i<lcListFile.size(); i++)
         * { String s = lcListFile.get(i); if (s.length() > 1)
         * Constant.Color.LandColor[0].map.add(s); } }
         * Constant.Keywords.loaded[0] = true; if (Constant.Runtime.DevMode[0])
         * { System.out.println(Constant.Keywords.NonStackingList[0].list); } }
         */
    }

    /**
     * Gets the builds the info.
     * 
     * @return {@link forge.model.BuildInfo}
     */
    public final BuildInfo getBuildInfo() {
        return this.buildInfo;
    }

    /**
     * Sets the builds the info.
     * 
     * @param bi0
     *            &emsp; {@link forge.model.BuildInfo}
     */
    protected final void setBuildInfo(final BuildInfo bi0) {
        this.buildInfo = bi0;
    }

    /**
     * Gets the preferences.
     * 
     * @return {@link forge.properties.ForgePreferences}
     */
    public final ForgePreferences getPreferences() {
        return this.preferences;
    }

    /**
     * Gets the quest preferences.
     * 
     * @return {@link forge.quest.data.QuestPreferences}
     */
    public final QuestPreferences getQuestPreferences() {
        return this.questPreferences;
    }

    /**
     * Returns all player's decks for constructed, sealed and whatever.
     * 
     * @return {@link forge.decks.CardCollections}
     */
    public final CardCollections getDecks() {
        if (this.decks == null) {
            this.decks = new CardCollections(ForgeProps.getFile(NewConstants.NEW_DECKS));
        }
        return this.decks;
    }

    /**
     * Gets the game action model.
     * 
     * @return {@link forge.GameAction}
     */
    public final GameAction getGameAction() {
        return this.gameAction;
    }

    /**
     * Gets the game state model - that is, the data stored for a single game.
     * 
     * @return {@link forge.game.GameState}
     */
    public final GameState getGameState() {
        return this.gameState;
    }

    /**
     * Gets the match state model - that is, the data stored over multiple
     * games.
     * 
     * @return {@link forge.model.FMatchState}
     */
    public final FMatchState getMatchState() {
        return this.matchState;
    }

    /**
     * Gets the game summary.
     * 
     * @return {@link forge.game.GameSummary}
     */
    public final GameSummary getGameSummary() {
        return this.gameState.getGameSummary();
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @return the editions
     */

    public final EditionCollection getEditions() {
        return this.editions;
    }

    /**
     * Gets the formats.
     *
     * @return the formats
     */
    public final FormatCollection getFormats() {
        return this.formats;
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in
     * codebase.
     * 
     * @return true, if successful
     */
    public final boolean savePrefs() {
        final ForgePreferences fp = this.preferences;
        final List<VField> fieldViews = VMatchUI.SINGLETON_INSTANCE.getFieldViews();

        // AI field is at index [0]
        fp.setPref(FPref.PHASE_AI_UPKEEP, String.valueOf(fieldViews.get(0).getLblUpkeep().getEnabled()));
        fp.setPref(FPref.PHASE_AI_DRAW, String.valueOf(fieldViews.get(0).getLblDraw().getEnabled()));
        fp.setPref(FPref.PHASE_AI_MAIN1, String.valueOf(fieldViews.get(0).getLblMain1().getEnabled()));
        fp.setPref(FPref.PHASE_AI_BEGINCOMBAT, String.valueOf(fieldViews.get(0).getLblBeginCombat().getEnabled()));
        fp.setPref(FPref.PHASE_AI_DECLAREATTACKERS,
                String.valueOf(fieldViews.get(0).getLblDeclareAttackers().getEnabled()));
        fp.setPref(FPref.PHASE_AI_DECLAREBLOCKERS,
                String.valueOf(fieldViews.get(0).getLblDeclareBlockers().getEnabled()));
        fp.setPref(FPref.PHASE_AI_FIRSTSTRIKE, String.valueOf(fieldViews.get(0).getLblFirstStrike().getEnabled()));
        fp.setPref(FPref.PHASE_AI_COMBATDAMAGE, String.valueOf(fieldViews.get(0).getLblCombatDamage().getEnabled()));
        fp.setPref(FPref.PHASE_AI_ENDCOMBAT, String.valueOf(fieldViews.get(0).getLblEndCombat().getEnabled()));
        fp.setPref(FPref.PHASE_AI_MAIN2, String.valueOf(fieldViews.get(0).getLblMain2().getEnabled()));
        fp.setPref(FPref.PHASE_AI_EOT, String.valueOf(fieldViews.get(0).getLblEndTurn().getEnabled()));
        fp.setPref(FPref.PHASE_AI_CLEANUP, String.valueOf(fieldViews.get(0).getLblCleanup().getEnabled()));

        // Human field is at index [1]
        fp.setPref(FPref.PHASE_HUMAN_UPKEEP, String.valueOf(fieldViews.get(1).getLblUpkeep().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_DRAW, String.valueOf(fieldViews.get(1).getLblDraw().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_MAIN1, String.valueOf(fieldViews.get(1).getLblMain1().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_BEGINCOMBAT, String.valueOf(fieldViews.get(1).getLblBeginCombat().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_DECLAREATTACKERS,
                String.valueOf(fieldViews.get(1).getLblDeclareAttackers().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_DECLAREBLOCKERS,
                String.valueOf(fieldViews.get(1).getLblDeclareBlockers().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_FIRSTSTRIKE, String.valueOf(fieldViews.get(1).getLblFirstStrike().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_COMBATDAMAGE, String.valueOf(fieldViews.get(1).getLblCombatDamage().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_ENDCOMBAT, String.valueOf(fieldViews.get(1).getLblEndCombat().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_MAIN2, String.valueOf(fieldViews.get(1).getLblMain2().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_EOT, String.valueOf(fieldViews.get(1).getLblEndTurn().getEnabled()));
        fp.setPref(FPref.PHASE_HUMAN_CLEANUP, String.valueOf(fieldViews.get(1).getLblCleanup().getEnabled()));

        final VDev v = VMatchUI.SINGLETON_INSTANCE.getViewDevMode();
        Constant.Runtime.MILL[0] = v.getLblMilling().getEnabled();

        fp.setPref(FPref.DEV_MILLING_LOSS, String.valueOf(Constant.Runtime.MILL[0]));
        fp.setPref(FPref.DEV_UNLIMITED_LAND, String.valueOf(v.getLblUnlimitedLands().getEnabled()));

        fp.save();
        return true;
    }

    /**
     * TODO: Needs to be reworked for efficiency with rest of prefs loads in
     * codebase.
     * 
     * @return true, if successful
     */
    public final boolean loadPrefs() {
        final ForgePreferences fp = Singletons.getModel().getPreferences();
        final List<VField> fieldViews = VMatchUI.SINGLETON_INSTANCE.getFieldViews();

        Constant.Runtime.MILL[0] = fp.getPrefBoolean(FPref.DEV_MILLING_LOSS);
        Constant.Runtime.DEV_MODE[0] = fp.getPrefBoolean(FPref.DEV_MODE_ENABLED);
        Constant.Runtime.UPLOAD_DRAFT[0] = fp.getPrefBoolean(FPref.UI_UPLOAD_DRAFT);
        Constant.Runtime.RANDOM_FOIL[0] = fp.getPrefBoolean(FPref.UI_RANDOM_FOIL);
        Constant.Runtime.UPLOAD_DRAFT[0] = (Constant.Runtime.NET_CONN[0] ? fp.getPrefBoolean(FPref.UI_UPLOAD_DRAFT)
                : false);

        // AI field is at index [0]
        fieldViews.get(0).getLblUpkeep().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_UPKEEP));
        fieldViews.get(0).getLblDraw().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_DRAW));
        fieldViews.get(0).getLblMain1().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_MAIN1));
        fieldViews.get(0).getLblBeginCombat().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_BEGINCOMBAT));
        fieldViews.get(0).getLblDeclareAttackers().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_DECLAREATTACKERS));
        fieldViews.get(0).getLblDeclareBlockers().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_DECLAREBLOCKERS));
        fieldViews.get(0).getLblFirstStrike().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_FIRSTSTRIKE));
        fieldViews.get(0).getLblCombatDamage().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_COMBATDAMAGE));
        fieldViews.get(0).getLblEndCombat().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_ENDCOMBAT));
        fieldViews.get(0).getLblMain2().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_MAIN2));
        fieldViews.get(0).getLblEndTurn().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_EOT));
        fieldViews.get(0).getLblCleanup().setEnabled(fp.getPrefBoolean(FPref.PHASE_AI_CLEANUP));

        // Human field is at index [1]
        fieldViews.get(1).getLblUpkeep().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_UPKEEP));
        fieldViews.get(1).getLblDraw().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_DRAW));
        fieldViews.get(1).getLblMain1().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_MAIN1));
        fieldViews.get(1).getLblBeginCombat().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_BEGINCOMBAT));
        fieldViews.get(1).getLblDeclareAttackers().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREATTACKERS));
        fieldViews.get(1).getLblDeclareBlockers().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREBLOCKERS));
        fieldViews.get(1).getLblFirstStrike().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_FIRSTSTRIKE));
        fieldViews.get(1).getLblCombatDamage().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_COMBATDAMAGE));
        fieldViews.get(1).getLblEndCombat().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_ENDCOMBAT));
        fieldViews.get(1).getLblMain2().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_MAIN2));
        fieldViews.get(1).getLblEndTurn().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_EOT));
        fieldViews.get(1).getLblCleanup().setEnabled(fp.getPrefBoolean(FPref.PHASE_HUMAN_CLEANUP));

        //Singletons.getView().getViewMatch().setLayoutParams(fp.getPref(FPref.UI_LAYOUT_PARAMS));
        return true;
    }

    /**
     * Finalizer, generally should be avoided, but here closes the log file
     * stream and resets the system output streams.
     */
    public final void close() {
        System.setOut(this.oldSystemOut);
        System.setErr(this.oldSystemErr);
        try {
            this.logFileStream.close();
        } catch (final IOException e) {
            // ignored
        }
    }

    /** @return {@link forge.util.IStorageView}<{@link forge.card.CardBlock}> */
    public IStorageView<CardBlock> getBlocks() {
        return blocks;
    }

    /** @return {@link forge.util.IStorageView}<{@link forge.card.FatPackData}> */
    public IStorageView<FatPackData> getFatPacks() {
        return fatPacks;
    }

    /** @return {@link forge.util.IStorageView}<{@link forge.card.BoosterData}> */
    public final IStorageView<BoosterData> getTournamentPacks() {
        return tournaments;
    }

    /** @return {@link forge.util.IStorageView}<{@link forge.card.BoosterData}> */
    public final IStorageView<BoosterData> getBoosters() {
        return boosters;
    }
}
