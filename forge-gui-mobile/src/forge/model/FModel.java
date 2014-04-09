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

import forge.CardStorageReader;
import forge.StaticData;
import forge.ai.AiProfileUtil;
import forge.card.CardType;
import forge.game.GameFormat;
import forge.game.card.CardUtil;
import forge.guantlet.GauntletData;
import forge.limited.GauntletMini;
import forge.quest.QuestController;
import forge.quest.QuestWorld;
import forge.quest.data.QuestPreferences;
import forge.toolbox.FProgressBar;
import forge.util.FileUtil;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;
import forge.utils.Constants;
import forge.utils.ForgePreferences;

import java.io.*;
import java.util.List;

import com.badlogic.gdx.Gdx;

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
    private FModel() { } //don't allow creating instance

    private static StaticData magicDb;

    private static PrintStream oldSystemOut;
    private static PrintStream oldSystemErr;
    private static OutputStream logFileStream;

    private static QuestPreferences questPreferences;
    private static ForgePreferences preferences;

    // Someone should take care of 2 gauntlets here
    private static GauntletData gauntletData;
    private static GauntletMini gauntlet;

    private static QuestController quest;
    private static CardCollections decks;

    private static IStorage<CardBlock> blocks;
    private static IStorage<CardBlock> fantasyBlocks;
    private static IStorage<QuestWorld> worlds;
    private static GameFormat.Collection formats;

    public static void initialize(final FProgressBar progressBar) {
        // install our error reporter
        //ExceptionHandler.registerErrorHandling();

        //load card database
        final CardStorageReader.ProgressObserver progressBarBridge = new CardStorageReader.ProgressObserver() {
            @Override
            public void setOperationName(final String name, final boolean usePercents) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setDescription(name);
                        progressBar.setPercentMode(usePercents);
                    }
                });
            }

            @Override
            public void report(final int current, final int total) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setMaximum(total);
                        progressBar.setValue(current);
                    }
                });
            }
        };

        final CardStorageReader reader = new CardStorageReader(Constants.CARD_DATA_DIR, progressBarBridge, null);
        magicDb = new StaticData(reader, Constants.EDITIONS_DIR, Constants.BLOCK_DATA_DIR);

        //create profile dirs if they don't already exist
        for (String dname : Constants.PROFILE_DIRS) {
            File path = new File(dname);
            if (path.isDirectory()) {
                // already exists
                continue;
            }
            if (!path.mkdirs()) {
                throw new RuntimeException("cannot create profile directory: " + dname);
            }
        }
        
        //initialize log file
        File logFile = new File(Constants.LOG_FILE);

        int i = 0;
        while (logFile.exists() && !logFile.delete()) {
            String pathname = logFile.getPath().replaceAll("[0-9]{0,2}.log$", String.valueOf(i++) + ".log");
            logFile = new File(pathname);
        }

        try {
            logFileStream = new FileOutputStream(logFile);
        }
        catch (final FileNotFoundException e) {
            e.printStackTrace();
        }

        /*oldSystemOut = System.out;
        System.setOut(new PrintStream(new MultiplexOutputStream(System.out, logFileStream), true));
        oldSystemErr = System.err;
        System.setErr(new PrintStream(new MultiplexOutputStream(System.err, logFileStream), true));*/

        // Instantiate preferences: quest and regular
        try {
            preferences = new ForgePreferences();
        }
        catch (final Exception exn) {
            throw new RuntimeException(exn);
        }

        formats = new GameFormat.Collection(new GameFormat.Reader(new File(Constants.BLOCK_DATA_DIR + "formats.txt")));
        blocks = new StorageBase<CardBlock>("Block definitions", new CardBlock.Reader(Constants.BLOCK_DATA_DIR + "blocks.txt", magicDb.getEditions()));
        questPreferences = new QuestPreferences();
        gauntletData = new GauntletData();
        fantasyBlocks = new StorageBase<CardBlock>("Custom blocks", new CardBlock.Reader(Constants.BLOCK_DATA_DIR + "fantasyblocks.txt", magicDb.getEditions()));
        worlds = new StorageBase<QuestWorld>("Quest worlds", new QuestWorld.Reader(Constants.QUEST_WORLD_DIR + "worlds.txt"));

        loadDynamicGamedata();

        progressBar.setDescription("Loading decks");

        decks = new CardCollections();
        quest = new QuestController();
        
        //preload AI profiles
        AiProfileUtil.loadAllProfiles(Constants.AI_PROFILE_DIR);
    }

    public static QuestController getQuest() {
        return quest;
    }
    
    private static boolean keywordsLoaded = false;
    
    /**
     * Load dynamic gamedata.
     */
    public static void loadDynamicGamedata() {
        if (!CardType.Constant.LOADED[0]) {
            final List<String> typeListFile = FileUtil.readFile(Constants.TYPE_LIST_FILE);

            List<String> tList = null;

            if (typeListFile.size() > 0) {
                for (int i = 0; i < typeListFile.size(); i++) {
                    final String s = typeListFile.get(i);

                    if (s.equals("[CardTypes]")) {
                        tList = CardType.Constant.CARD_TYPES;
                    }

                    else if (s.equals("[SuperTypes]")) {
                        tList = CardType.Constant.SUPER_TYPES;
                    }

                    else if (s.equals("[BasicTypes]")) {
                        tList = CardType.Constant.BASIC_TYPES;
                    }

                    else if (s.equals("[LandTypes]")) {
                        tList = CardType.Constant.LAND_TYPES;
                    }

                    else if (s.equals("[CreatureTypes]")) {
                        tList = CardType.Constant.CREATURE_TYPES;
                    }

                    else if (s.equals("[InstantTypes]")) {
                        tList = CardType.Constant.INSTANT_TYPES;
                    }

                    else if (s.equals("[SorceryTypes]")) {
                        tList = CardType.Constant.SORCERY_TYPES;
                    }

                    else if (s.equals("[EnchantmentTypes]")) {
                        tList = CardType.Constant.ENCHANTMENT_TYPES;
                    }

                    else if (s.equals("[ArtifactTypes]")) {
                        tList = CardType.Constant.ARTIFACT_TYPES;
                    }

                    else if (s.equals("[WalkerTypes]")) {
                        tList = CardType.Constant.WALKER_TYPES;
                    }

                    else if (s.length() > 1) {
                        tList.add(s);
                    }
                }
            }
            CardType.Constant.LOADED[0] = true;
            /*
             * if (Constant.Runtime.DevMode[0]) {
             * System.out.println(CardType.Constant.cardTypes[0].list);
             * System.out.println(CardType.Constant.superTypes[0].list);
             * System.out.println(CardType.Constant.basicTypes[0].list);
             * System.out.println(CardType.Constant.landTypes[0].list);
             * System.out.println(CardType.Constant.creatureTypes[0].list);
             * System.out.println(CardType.Constant.instantTypes[0].list);
             * System.out.println(CardType.Constant.sorceryTypes[0].list);
             * System.out.println(CardType.Constant.enchantmentTypes[0].list);
             * System.out.println(CardType.Constant.artifactTypes[0].list);
             * System.out.println(CardType.Constant.walkerTypes[0].list); }
             */
        }

        if (!keywordsLoaded) {
            final List<String> nskwListFile = FileUtil.readFile(Constants.KEYWORD_LIST_FILE);

            if (nskwListFile.size() > 1) {
                for (String s : nskwListFile) {
                    if (s.length() > 1) {
                        CardUtil.NON_STACKING_LIST.add(s);
                    }
                }
            }
            keywordsLoaded = true;
            /*
             * if (Constant.Runtime.DevMode[0]) {
             * System.out.println(Constant.Keywords.NonStackingList[0].list); }
             */
        }

        /*
         * if (!MagicColor.Constant.loaded[0]) { ArrayList<String> lcListFile =
         * FileUtil.readFile("res/gamedata/LandColorList");
         * 
         * if (lcListFile.size() > 1) { for (int i=0; i<lcListFile.size(); i++)
         * { String s = lcListFile.get(i); if (s.length() > 1)
         * MagicColor.Constant.LandColor[0].map.add(s); } }
         * Constant.Keywords.loaded[0] = true; if (Constant.Runtime.DevMode[0])
         * { System.out.println(Constant.Keywords.NonStackingList[0].list); } }
         */
    }

    public static StaticData getMagicDb() {
        return magicDb;
    }

    public static ForgePreferences getPreferences() {
        return preferences;
    }

    public static IStorage<CardBlock> getBlocks() {
        return blocks;
    }    

    public static QuestPreferences getQuestPreferences() {
        return questPreferences;
    }

    public static GauntletData getGauntletData() {
        return gauntletData;
    }

    public static void setGauntletData(GauntletData data0) {
        gauntletData = data0;
    }

    public static GauntletMini getGauntletMini() {
        if (gauntlet == null) {
            gauntlet = new GauntletMini();
        }
        return gauntlet;
    }

    public static CardCollections getDecks() {
        return decks;
    }

    public static IStorage<QuestWorld> getWorlds() {
        return worlds;
    }
 
    public static GameFormat.Collection getFormats() {
        return formats;
    }

    public static IStorage<CardBlock> getFantasyBlocks() {
        return fantasyBlocks;
    }

    /**
     * Finalizer, generally should be avoided, but here closes the log file
     * stream and resets the system output streams.
     */
    public static void close() throws IOException {
        System.setOut(oldSystemOut);
        System.setErr(oldSystemErr);
        logFileStream.close();
    }
}
