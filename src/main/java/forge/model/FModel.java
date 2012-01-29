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

import arcane.util.MultiplexOutputStream;
import forge.AllZone;
import forge.ComputerAIGeneral;
import forge.ComputerAIInput;
import forge.Constant;
import forge.ConstantStringArrayList;
import forge.game.GameSummary;
import forge.gui.input.InputControl;
import forge.properties.ForgePreferences;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.properties.ForgePreferences.FPref;
import forge.quest.data.QuestPreferences;
import forge.util.FileUtil;
import forge.util.HttpUtil;

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
        // Unfortunately, they're tied up in legacy code in the Display interface,
        // currently in GuiTopLevel.  When that code is updated, this TODO should be resolved.
        // Doublestrike 24-01-12
        Constant.Runtime.DEV_MODE[0] = preferences.getPrefBoolean(FPref.DEV_MODE_ENABLED);

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

    /** @return {@link forge.model.BuildInfo} */
    public final BuildInfo getBuildInfo() {
        return this.buildInfo;
    }

    /** @param bi0 &emsp; {@link forge.model.BuildInfo} */
    protected final void setBuildInfo(final BuildInfo bi0) {
        this.buildInfo = bi0;
    }

    /** @return {@link forge.properties.ForgePreferences} */
    public final ForgePreferences getPreferences() {
        return this.preferences;
    }

    /** @return {@link forge.quest.data.QuestPreferences} */
    public final QuestPreferences getQuestPreferences() {
        return this.questPreferences;
    }

    /** @return {@link forge.model.FGameState} */
    public final FGameState getGameState() {
        return this.gameState;
    }

    /** @return {@link forge.game.GameSummary} */
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

}
