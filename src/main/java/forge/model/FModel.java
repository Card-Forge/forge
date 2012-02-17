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
import java.util.ArrayList;
import java.util.List;

import arcane.util.MultiplexOutputStream;
import forge.AllZone;
import forge.ComputerAIGeneral;
import forge.ComputerAIInput;
import forge.Constant;
import forge.ConstantStringArrayList;
import forge.Singletons;
import forge.control.input.InputControl;
import forge.game.GameSummary;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestPreferences;
import forge.util.FileUtil;
import forge.util.HttpUtil;
import forge.view.match.ViewField;
import forge.view.match.ViewTabber;
import forge.view.toolbox.FSkin;

/**
 * The default Model implementation for Forge.
 * 
 * This used to be an interface, but it seems unlikely that we will ever use a
 * different model.
 * 
 * In case we need to convert it into an interface in the future, all fields of
 * this class must be either private or public static final.
 */
public class FModel {
    // private static final int NUM_INIT_PHASES = 1;

    private final transient OutputStream logFileStream;
    private final transient PrintStream oldSystemOut;
    private final transient PrintStream oldSystemErr;
    private BuildInfo buildInfo;

    /** The preferences. */
    private final QuestPreferences questPreferences;
    private final ForgePreferences preferences;
    private FGameState gameState;

    /**
     * Constructor.
     * 
     * @throws FileNotFoundException
     *             if we could not find or write to the log file.
     */
    public FModel() throws FileNotFoundException {
        // Fire up log file
        final File logFile = new File("forge.log");
        final boolean deleteSucceeded = logFile.delete();

        if (logFile.exists() && !deleteSucceeded && (logFile.length() != 0)) {
            throw new IllegalStateException("Could not delete existing logFile:" + logFile.getAbsolutePath());
        }

        this.logFileStream = new FileOutputStream(logFile);

        this.oldSystemOut = System.out;
        System.setOut(new PrintStream(new MultiplexOutputStream(System.out, this.logFileStream), true));
        this.oldSystemErr = System.err;
        System.setErr(new PrintStream(new MultiplexOutputStream(System.err, this.logFileStream), true));

        // Instantiate preferences
        try {
            this.preferences = new ForgePreferences();
        } catch (final Exception exn) {
            throw new RuntimeException(exn);
        }

        // Instantiate quest preferences
        this.questPreferences = new QuestPreferences();

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
            final ArrayList<String> typeListFile = FileUtil.readFile("res/gamedata/TypeLists.txt");

            ArrayList<String> tList = null;

            Constant.CardTypes.CARD_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.SUPER_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.BASIC_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.LAND_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.CREATURE_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.INSTANT_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.SORCERY_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.ENCHANTMENT_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.ARTIFACT_TYPES[0] = new ConstantStringArrayList();
            Constant.CardTypes.WALKER_TYPES[0] = new ConstantStringArrayList();

            if (typeListFile.size() > 0) {
                for (int i = 0; i < typeListFile.size(); i++) {
                    final String s = typeListFile.get(i);

                    if (s.equals("[CardTypes]")) {
                        tList = Constant.CardTypes.CARD_TYPES[0].getList();
                    }

                    else if (s.equals("[SuperTypes]")) {
                        tList = Constant.CardTypes.SUPER_TYPES[0].getList();
                    }

                    else if (s.equals("[BasicTypes]")) {
                        tList = Constant.CardTypes.BASIC_TYPES[0].getList();
                    }

                    else if (s.equals("[LandTypes]")) {
                        tList = Constant.CardTypes.LAND_TYPES[0].getList();
                    }

                    else if (s.equals("[CreatureTypes]")) {
                        tList = Constant.CardTypes.CREATURE_TYPES[0].getList();
                    }

                    else if (s.equals("[InstantTypes]")) {
                        tList = Constant.CardTypes.INSTANT_TYPES[0].getList();
                    }

                    else if (s.equals("[SorceryTypes]")) {
                        tList = Constant.CardTypes.SORCERY_TYPES[0].getList();
                    }

                    else if (s.equals("[EnchantmentTypes]")) {
                        tList = Constant.CardTypes.ENCHANTMENT_TYPES[0].getList();
                    }

                    else if (s.equals("[ArtifactTypes]")) {
                        tList = Constant.CardTypes.ARTIFACT_TYPES[0].getList();
                    }

                    else if (s.equals("[WalkerTypes]")) {
                        tList = Constant.CardTypes.WALKER_TYPES[0].getList();
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
            final ArrayList<String> nskwListFile = FileUtil.readFile("res/gamedata/NonStackingKWList.txt");

            Constant.Keywords.NON_STACKING_LIST[0] = new ConstantStringArrayList();

            if (nskwListFile.size() > 1) {
                for (int i = 0; i < nskwListFile.size(); i++) {
                    final String s = nskwListFile.get(i);
                    if (s.length() > 1) {
                        Constant.Keywords.NON_STACKING_LIST[0].getList().add(s);
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
     * Destructor for FModel.
     * 
     * @throws Throwable
     *             indirectly
     */
    @Override
    protected final void finalize() throws Throwable {
        this.close();
        super.finalize();
    }

    /**
     * Opposite of constructor; resets all system resources and closes the log
     * file.
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
     * @param bi0 &emsp; {@link forge.model.BuildInfo}
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
     * Gets the game state.
     *
     * @return {@link forge.model.FGameState}
     */
    public final FGameState getGameState() {
        return this.gameState;
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
     * Create and return a new game state.
     * 
     * @return a fresh {@link forge.model.FGameState}
     */
    public final FGameState resetGameState() {
        this.gameState = new FGameState();
        return this.gameState;
    }

    /**
<<<<<<< HEAD
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in codebase.
     * 
     * @return a boolean.
=======
     * TODO: Needs to be reworked for efficiency with rest of prefs saves in
     * codebase.
     *
     * @return true, if successful
>>>>>>> Update Maven plugins. Checkstyle
     */
    public final boolean savePrefs() {
        final ForgePreferences fp = this.preferences;
        final List<ViewField> fieldViews = Singletons.getView().getMatchView().getFieldViews();

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

        final ViewTabber v = Singletons.getView().getMatchView().getViewTabber();
        Constant.Runtime.MILL[0] = v.getLblMilling().getEnabled();

        fp.setPref(FPref.DEV_MILLING_LOSS, String.valueOf(Constant.Runtime.MILL[0]));
        fp.setPref(FPref.UI_LAYOUT_PARAMS, String.valueOf(Singletons.getView().getMatchView().getLayoutParams()));
        fp.setPref(FPref.DEV_UNLIMITED_LAND, String.valueOf(v.getLblUnlimitedLands().getEnabled()));

        fp.save();
        return true;
    }

    /**
<<<<<<< HEAD
     * TODO: Needs to be reworked for efficiency with rest of prefs loads in codebase.
     * 
     * @return a boolean.
=======
     * TODO: Needs to be reworked for efficiency with rest of prefs loads in
     * codebase.
     *
     * @return true, if successful
>>>>>>> Update Maven plugins. Checkstyle
     */
    public final boolean loadPrefs() {
        final ForgePreferences fp = Singletons.getModel().getPreferences();
        final List<ViewField> fieldViews = Singletons.getView().getMatchView().getFieldViews();

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

        Singletons.getView().getMatchView().setLayoutParams(fp.getPref(FPref.UI_LAYOUT_PARAMS));
        return true;
    }
}
