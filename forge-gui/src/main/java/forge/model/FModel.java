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

import forge.Singletons;
import forge.ai.AiProfileUtil;
import forge.card.CardType;
import forge.error.ExceptionHandler;
import forge.game.GameFormat;
import forge.game.card.CardUtil;
import forge.gauntlet.GauntletData;
import forge.limited.GauntletMini;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.properties.NewConstants;
import forge.quest.QuestController;
import forge.quest.QuestWorld;
import forge.quest.data.QuestPreferences;
import forge.util.FileUtil;
import forge.util.MultiplexOutputStream;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;
import forge.view.FView;

import java.io.*;
import java.util.List;

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

    private final PrintStream oldSystemOut;
    private final PrintStream oldSystemErr;
    private OutputStream logFileStream;

    private final QuestPreferences questPreferences;
    private final ForgePreferences preferences;

    // Someone should take care of 2 gauntlets here
    private GauntletData gauntletData;
    private GauntletMini gauntlet;

    private final QuestController quest;
    private final CardCollections decks;

    private final IStorage<CardBlock> blocks;
    private final IStorage<CardBlock> fantasyBlocks;
    private final IStorage<QuestWorld> worlds;
    private final GameFormat.Collection formats;
    

    
    private static FModel instance = null;
    public synchronized final static FModel getInstance(boolean initWithUi) {
        if (instance == null)
            instance = new FModel(initWithUi);
        return instance;
    }
    
    /**
     * Constructor.
     * 
     * @throws FileNotFoundException
     *             if we could not find or write to the log file.
     */
    private FModel(boolean initWithUi) {
        // install our error reporter
        ExceptionHandler.registerErrorHandling();

        // create profile dirs if they don't already exist
        for (String dname : NewConstants.PROFILE_DIRS) {
            File path = new File(dname);
            if (path.isDirectory()) {
                // already exists
                continue;
            }
            if (!path.mkdirs()) {
                throw new RuntimeException("cannot create profile directory: " + dname);
            }
        }
        
        // initialize log file
        File logFile = new File(NewConstants.LOG_FILE);

        int i = 0;
        while (logFile.exists() && !logFile.delete()) {
            String pathname = logFile.getPath().replaceAll("[0-9]{0,2}.log$", String.valueOf(i++) + ".log");
            logFile = new File(pathname);
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
        
        this.formats = new GameFormat.Collection(new GameFormat.Reader(new File("res/blockdata", "formats.txt")));

        this.blocks = new StorageBase<CardBlock>("Block definitions", new CardBlock.Reader("res/blockdata/blocks.txt", Singletons.getMagicDb().getEditions()));
        this.questPreferences = new QuestPreferences();
        this.gauntletData = new GauntletData();

        
        
        this.fantasyBlocks = new StorageBase<CardBlock>("Custom blocks", new CardBlock.Reader("res/blockdata/fantasyblocks.txt", Singletons.getMagicDb().getEditions()));
        this.worlds = new StorageBase<QuestWorld>("Quest worlds", new QuestWorld.Reader("res/quest/world/worlds.txt"));
        // TODO - there's got to be a better place for this...oblivion?
        ForgePreferences.DEV_MODE = this.preferences.getPrefBoolean(FPref.DEV_MODE_ENABLED);

        this.loadDynamicGamedata();

        if (initWithUi) {
            FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Loading decks");
        }

        this.decks = new CardCollections();
        this.quest = new QuestController();

        // Preload AI profiles
        AiProfileUtil.loadAllProfiles(NewConstants.AI_PROFILE_DIR);
    }

    public final QuestController getQuest() {
        return quest;
    }
    
    private static boolean KeywordsLoaded = false;
    
    /**
     * Load dynamic gamedata.
     */
    public void loadDynamicGamedata() {
        if (!CardType.Constant.LOADED[0]) {
            final List<String> typeListFile = FileUtil.readFile(NewConstants.TYPE_LIST_FILE);

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

        if (!KeywordsLoaded) {
            final List<String> nskwListFile = FileUtil.readFile(NewConstants.KEYWORD_LIST_FILE);

            if (nskwListFile.size() > 1) {
                for (String s : nskwListFile) {
                    if (s.length() > 1) {
                        CardUtil.NON_STACKING_LIST.add(s);
                    }
                }
            }
            KeywordsLoaded = true;
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

    /**
     * Gets the preferences.
     * 
     * @return {@link forge.properties.ForgePreferences}
     */
    public final ForgePreferences getPreferences() {
        return this.preferences;
    }


    /** @return {@link forge.util.storage.IStorage}<{@link forge.model.CardBlock}> */
    public IStorage<CardBlock> getBlocks() {
        return blocks;
    }    
    /**
     * Gets the quest preferences.
     * 
     * @return {@link forge.quest.data.QuestPreferences}
     */
    public final QuestPreferences getQuestPreferences() {
        return this.questPreferences;
    }

    /** @return {@link forge.gui.home.gauntlet} */
    public GauntletData getGauntletData() {
        return this.gauntletData;
    }
    /**
     * Returns all player's decks for constructed, sealed and whatever.
     * 
     * @return {@link forge.model.CardCollections}
     */
    public final CardCollections getDecks() {
        return this.decks;
    }

    /**
     * Gets the game worlds.
     *
     * @return the worlds
     */
    public final IStorage<QuestWorld> getWorlds() {
        return this.worlds;
    }
    public final GameFormat.Collection getFormats() {
        return this.formats;
    }
    /**
     * Finalizer, generally should be avoided, but here closes the log file
     * stream and resets the system output streams.
     */
    public final void close() throws IOException {
        System.setOut(this.oldSystemOut);
        System.setErr(this.oldSystemErr);
        logFileStream.close();
    }


    /**
     * TODO: Write javadoc for this method.
     * @param data0 {@link forge.gauntlet.GauntletData}
     */
    public void setGauntletData(GauntletData data0) {
        this.gauntletData = data0;
    }

    /** @return {@link forge.util.storage.IStorage}<{@link forge.model.CardBlock}> */
    public IStorage<CardBlock> getFantasyBlocks() {
        return fantasyBlocks;
    }

    public GauntletMini getGauntletMini() {
        if (gauntlet == null) {
            gauntlet = new GauntletMini();
        }
        return gauntlet;
    }
}
