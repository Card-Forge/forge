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

import forge.Constant;
import forge.Constant.Preferences;
import forge.card.BoosterData;
import forge.card.CardBlock;
import forge.card.EditionCollection;
import forge.card.FatPackData;
import forge.card.FormatCollection;
import forge.card.cardfactory.CardStorageReader;
import forge.deck.CardCollections;
import forge.error.BugReporter;
import forge.error.ExceptionHandler;
import forge.game.GameState;
import forge.game.GameType;
import forge.game.MatchController;
import forge.game.limited.GauntletMini;
import forge.game.player.LobbyPlayer;
import forge.gauntlet.GauntletData;
import forge.gui.GuiUtils;
import forge.item.CardDb;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.properties.NewConstants;
import forge.quest.QuestController;
import forge.quest.QuestWorld;
import forge.quest.data.QuestPreferences;
import forge.util.FileUtil;
import forge.util.MultiplexOutputStream;
import forge.util.storage.IStorageView;
import forge.util.storage.StorageView;

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
    SINGLETON_INSTANCE;

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

    private final MatchController match;
    private GameState gameState;

    private final EditionCollection editions;
    private final FormatCollection formats;
    private final IStorageView<BoosterData> boosters;
    private final IStorageView<BoosterData> tournaments;
    private final IStorageView<FatPackData> fatPacks;
    private final IStorageView<CardBlock> blocks;
    private final IStorageView<CardBlock> fantasyBlocks;
    private final IStorageView<QuestWorld> worlds;

    /**
     * Constructor.
     * 
     * @throws FileNotFoundException
     *             if we could not find or write to the log file.
     */
    private FModel() {
        // Fire up log file and exception handling
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
        
        final File logFile = new File(NewConstants.LOG_FILE);
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

        this.questPreferences = new QuestPreferences();
        this.gauntletData = new GauntletData();

        this.editions = new EditionCollection();
        this.formats = new FormatCollection("res/blockdata/formats.txt");
        this.boosters = new StorageView<BoosterData>(new BoosterData.Reader("res/blockdata/boosters.txt"));
        this.tournaments = new StorageView<BoosterData>(new BoosterData.Reader("res/blockdata/starters.txt"));
        this.fatPacks = new StorageView<FatPackData>(new FatPackData.Reader("res/blockdata/fatpacks.txt"));
        this.blocks = new StorageView<CardBlock>(new CardBlock.Reader("res/blockdata/blocks.txt", editions));
        this.fantasyBlocks = new StorageView<CardBlock>(new CardBlock.Reader("res/blockdata/fantasyblocks.txt", editions));
        this.worlds = new StorageView<QuestWorld>(new QuestWorld.Reader("res/quest/world/worlds.txt"));

        this.match = new MatchController();
        // TODO - there's got to be a better place for this...oblivion?
        Preferences.DEV_MODE = this.preferences.getPrefBoolean(FPref.DEV_MODE_ENABLED);

        this.loadDynamicGamedata();

        // Loads all cards (using progress bar).
        GuiUtils.checkEDT("CardFactory$constructor", false);
        final CardStorageReader reader = new CardStorageReader(NewConstants.CARD_DATA_DIR, true);
        try {
            // this fills in our map of card names to Card instances.
            CardDb.setup(reader.loadCards());
            
        } catch (final Exception ex) {
            BugReporter.reportException(ex);
        }

        this.decks = new CardCollections();
        this.quest = new QuestController();
    }

    public final QuestController getQuest() {
        return quest;
    }

    /**
     * Load dynamic gamedata.
     */
    public void loadDynamicGamedata() {
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
                for (String s : nskwListFile) {
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

    /** @return {@link forge.gui.home.gauntlet} */
    public GauntletData getGauntletData() {
        return this.gauntletData;
    }
    /**
     * Returns all player's decks for constructed, sealed and whatever.
     * 
     * @return {@link forge.decks.CardCollections}
     */
    public final CardCollections getDecks() {
        return this.decks;
    }

    /**
     * Gets the game state model - that is, the data stored for a single game.
     * 
     * @return {@link forge.game.GameState}
     */
    public final GameState getGame() {
        return this.gameState;
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
     * Gets the game worlds.
     *
     * @return the worlds
     */
    public final IStorageView<QuestWorld> getWorlds() {
        return this.worlds;
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

    /** @return {@link forge.util.storage.IStorageView}<{@link forge.card.CardBlock}> */
    public IStorageView<CardBlock> getBlocks() {
        return blocks;
    }

    /** @return {@link forge.util.storage.IStorageView}<{@link forge.card.CardBlock}> */
    public IStorageView<CardBlock> getFantasyBlocks() {
        return fantasyBlocks;
    }

    /** @return {@link forge.util.storage.IStorageView}<{@link forge.card.FatPackData}> */
    public IStorageView<FatPackData> getFatPacks() {
        return fatPacks;
    }

    /** @return {@link forge.util.storage.IStorageView}<{@link forge.card.BoosterData}> */
    public final IStorageView<BoosterData> getTournamentPacks() {
        return tournaments;
    }

    /** @return {@link forge.util.storage.IStorageView}<{@link forge.card.BoosterData}> */
    public final IStorageView<BoosterData> getBoosters() {
        return boosters;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param data0 {@link forge.gauntlet.GauntletData}
     */
    public void setGauntletData(GauntletData data0) {
        this.gauntletData = data0;
    }

    public MatchController getMatch() {
        return match;
    }

    /**
     * TODO: Write javadoc for this method.
     * @param players
     */
    public GameState newGame(Iterable<LobbyPlayer> players, GameType type, final MatchController match0) {
        gameState = new GameState(players,type, match0);
        return gameState;
    }

    public GauntletMini getGauntletMini() {

        if (gauntlet == null) {
            gauntlet = new GauntletMini();
        }
        return gauntlet;
    }
}
