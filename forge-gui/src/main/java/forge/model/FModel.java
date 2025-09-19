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

import com.google.common.collect.Maps;
import forge.*;
import forge.CardStorageReader.ProgressObserver;
import forge.ai.AiProfileUtil;
import forge.card.CardRulesPredicates;
import forge.card.CardType;
import forge.deck.CardArchetypeLDAGenerator;
import forge.deck.CardRelationMatrixGenerator;
import forge.deck.io.DeckPreferences;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.game.card.CardUtil;
import forge.game.spellability.Spell;
import forge.gamemodes.gauntlet.GauntletData;
import forge.gamemodes.limited.GauntletMini;
import forge.gamemodes.limited.ThemedChaosDraft;
import forge.gamemodes.planarconquest.ConquestController;
import forge.gamemodes.planarconquest.ConquestPlane;
import forge.gamemodes.planarconquest.ConquestPreferences;
import forge.gamemodes.planarconquest.ConquestUtil;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestWorld;
import forge.gamemodes.quest.data.QuestPreferences;
import forge.gamemodes.tournament.TournamentData;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.card.CardPreferences;
import forge.gui.interfaces.IProgressBar;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.itemmanager.ItemManagerConfig;
import forge.localinstance.achievements.*;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgeNetPreferences;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.player.GamePlayerUtil;
import forge.util.*;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The default Model implementation for Forge.
 *
 * This used to be an interface, but it seems unlikely that we will ever use a
 * different model.
 *
 * In case we need to convert it into an interface in the future, all fields of
 * this class must be either private or public static final.
 */
public final class FModel {
    private FModel() { } //don't allow creating instance

    private static StaticData magicDb;

    private static QuestPreferences questPreferences;
    private static ConquestPreferences conquestPreferences;
    private static ForgePreferences preferences;
    private static ForgeNetPreferences netPreferences;
    private static Map<GameType, AchievementCollection> achievements;

    // Someone should take care of 2 gauntlets here
    private static TournamentData tournamentData;
    private static GauntletData gauntletData;
    private static GauntletMini gauntlet;

    private static QuestController quest;
    private static ConquestController conquest;
    private static CardCollections decks;

    private static IStorage<CardBlock> blocks;
    private static IStorage<CardBlock> fantasyBlocks;
    private static IStorage<ThemedChaosDraft> themedChaosDrafts;
    private static IStorage<ConquestPlane> planes;
    private static IStorage<QuestWorld> worlds;
    private static GameFormat.Collection formats;
    private static ItemPool<PaperCard> uniqueCardsNoAlt, allCardsNoAlt, planechaseCards, archenemyCards,
            brawlCommander, oathbreakerCommander, tinyLeadersCommander, commanderPool,
            avatarPool, conspiracyPool, dungeonPool, attractionPool, contraptionPool;

    public static void initialize(final IProgressBar progressBar, Function<ForgePreferences, Void> adjustPrefs) {
        initialize(progressBar, adjustPrefs, false);
    }
    public static void initialize(final IProgressBar progressBar, Function<ForgePreferences, Void> adjustPrefs, boolean isSimTest) {
        ImageKeys.initializeDirs(
                ForgeConstants.CACHE_CARD_PICS_DIR, ForgeConstants.CACHE_CARD_PICS_SUBDIR,
                ForgeConstants.CACHE_TOKEN_PICS_DIR, ForgeConstants.CACHE_ICON_PICS_DIR,
                ForgeConstants.CACHE_BOOSTER_PICS_DIR, ForgeConstants.CACHE_FATPACK_PICS_DIR,
                ForgeConstants.CACHE_BOOSTERBOX_PICS_DIR, ForgeConstants.CACHE_PRECON_PICS_DIR,
                ForgeConstants.CACHE_TOURNAMENTPACK_PICS_DIR);

        // Instantiate preferences: quest and regular
        // Preferences are initialized first so that the splash screen can be translated.
        try {
            preferences = GuiBase.getForgePrefs();
            if (adjustPrefs != null) {
                adjustPrefs.apply(preferences);
            }
            GamePlayerUtil.getGuiPlayer().setName(preferences.getPref(FPref.PLAYER_NAME));
        }
        catch (final Exception exn) {
            throw new RuntimeException(exn);
        }

        Lang.createInstance(FModel.getPreferences().getPref(FPref.UI_LANGUAGE));
        Localizer.getInstance().initialize(FModel.getPreferences().getPref(FPref.UI_LANGUAGE), ForgeConstants.LANG_DIR);

        final ProgressObserver progressBarBridge = (progressBar == null) ?
                ProgressObserver.emptyObserver : new ProgressObserver() {
            @Override
            public void setOperationName(final String name, final boolean usePercents) {
                FThreads.invokeInEdtLater(() -> {
                    progressBar.setDescription(name);
                    progressBar.setPercentMode(usePercents);
                });
            }

            @Override
            public void report(final int current, final int total) {
                FThreads.invokeInEdtLater(() -> {
                    progressBar.setMaximum(total);
                    progressBar.setValue(current);
                });
            }
        };

        // if (new AutoUpdater(true).attemptToUpdate()) {}
        // Load types before loading cards
        loadDynamicGamedata();

        // Load card database
        // Lazy loading currently disabled
        final CardStorageReader reader = new CardStorageReader(ForgeConstants.CARD_DATA_DIR, progressBarBridge,
                false);
        final CardStorageReader tokenReader = new CardStorageReader(ForgeConstants.TOKEN_DATA_DIR, progressBarBridge,
                false);
        CardStorageReader customReader;
        try {
           customReader  = new CardStorageReader(ForgeConstants.USER_CUSTOM_CARDS_DIR, progressBarBridge, false);
        } catch (Exception e) {
            customReader = null;
        }
        CardStorageReader customTokenReader;
        try {
            customTokenReader  = new CardStorageReader(ForgeConstants.USER_CUSTOM_TOKENS_DIR, progressBarBridge, false);
        } catch (Exception e) {
            customTokenReader = null;
        }

        // Do this first so PaperCards see the real preference
        CardTranslation.preloadTranslation(preferences.getPref(FPref.UI_LANGUAGE), ForgeConstants.LANG_DIR);

        magicDb = new StaticData(reader, tokenReader, customReader, customTokenReader, ForgeConstants.EDITIONS_DIR,
                                 ForgeConstants.USER_CUSTOM_EDITIONS_DIR, ForgeConstants.BLOCK_DATA_DIR, ForgeConstants.SETLOOKUP_DIR,
                                 FModel.getPreferences().getPref(FPref.UI_PREFERRED_ART),
                                 FModel.getPreferences().getPrefBoolean(FPref.UI_LOAD_UNKNOWN_CARDS),
                                 FModel.getPreferences().getPrefBoolean(FPref.UI_LOAD_NONLEGAL_CARDS),
                                 FModel.getPreferences().getPrefBoolean(FPref.ALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE),
                                 FModel.getPreferences().getPrefBoolean(FPref.UI_SMART_CARD_ART)
                );

        // Create profile dirs if they don't already exist
        for (final String dname : ForgeConstants.PROFILE_DIRS) {
            final File path = new File(dname);
            if (path.isDirectory()) {
                // Already exists
                continue;
            }
            if (!path.mkdirs()) {
                throw new RuntimeException("cannot create profile directory: " + dname);
            }
        }

        ForgePreferences.DEV_MODE = preferences.getPrefBoolean(FPref.DEV_MODE_ENABLED);
        ForgePreferences.UPLOAD_DRAFT = ForgePreferences.NET_CONN;

        formats = new GameFormat.Collection(new GameFormat.Reader( new File(ForgeConstants.FORMATS_DATA_DIR),
                new File(ForgeConstants.USER_FORMATS_DIR), preferences.getPrefBoolean(FPref.LOAD_ARCHIVED_FORMATS)));

        magicDb.setStandardPredicate(formats.getStandard().getFilterRules());
        magicDb.setPioneerPredicate(formats.getPioneer().getFilterRules());
        magicDb.setModernPredicate(formats.getModern().getFilterRules());
        magicDb.setCommanderPredicate(formats.get("Commander").getFilterRules());
        magicDb.setOathbreakerPredicate(formats.get("Oathbreaker").getFilterRules());
        magicDb.setBrawlPredicate(formats.get("Brawl").getFilterRules());

        magicDb.setFilteredHandsEnabled(preferences.getPrefBoolean(FPref.FILTERED_HANDS));
        try {
            magicDb.setMulliganRule(MulliganDefs.MulliganRule.valueOf(preferences.getPref(FPref.MULLIGAN_RULE)));
        } catch(Exception e) {
            magicDb.setMulliganRule(MulliganDefs.MulliganRule.London);
        }

        blocks = new StorageBase<>("Block definitions", new CardBlock.Reader(ForgeConstants.BLOCK_DATA_DIR + "blocks.txt", magicDb.getEditions()));
        // SetblockLands
        for (final CardBlock b : blocks) {
            magicDb.getBlockLands().add(b.getLandSet().getCode());
        }
        fantasyBlocks = new StorageBase<>("Custom blocks", new CardBlock.Reader(ForgeConstants.BLOCK_DATA_DIR + "fantasyblocks.txt", magicDb.getEditions()));
        themedChaosDrafts = new StorageBase<>("Themed Chaos Drafts", new ThemedChaosDraft.Reader(ForgeConstants.BLOCK_DATA_DIR + "chaosdraftthemes.txt"));
        planes = new StorageBase<>("Conquest planes", new ConquestPlane.Reader(ForgeConstants.CONQUEST_PLANES_DIR + "planes.txt"));
        Map<String, QuestWorld> standardWorlds = new QuestWorld.Reader(ForgeConstants.QUEST_WORLD_DIR + "worlds.txt").readAll();
        Map<String, QuestWorld> customWorlds = new QuestWorld.Reader(ForgeConstants.USER_QUEST_WORLD_DIR + "customworlds.txt").readAll();
        customWorlds.values().forEach(world -> world.setCustom(true));
        standardWorlds.putAll(customWorlds);
        worlds = new StorageBase<>("Quest worlds", null, standardWorlds);

        Spell.setPerformanceMode(preferences.getPrefBoolean(FPref.PERFORMANCE_MODE));

        if (progressBar != null) {
            FThreads.invokeInEdtLater(() -> progressBar.setDescription(Localizer.getInstance().getMessage("splash.loading.decks")));
        }

        CardPreferences.load();
        DeckPreferences.load();
        ItemManagerConfig.load();

        achievements = Maps.newHashMap();
        achievements.put(GameType.Constructed, new ConstructedAchievements());
        achievements.put(GameType.Draft, new DraftAchievements());
        achievements.put(GameType.Sealed, new SealedAchievements());
        achievements.put(GameType.Quest, new QuestAchievements());
        achievements.put(GameType.PlanarConquest, new PlanarConquestAchievements());
        achievements.put(GameType.Puzzle, new PuzzleAchievements());
        achievements.put(GameType.Adventure, new AdventureAchievements());

        // Preload AI profiles
        AiProfileUtil.loadAllProfiles(ForgeConstants.AI_PROFILE_DIR);
        AiProfileUtil.setAiSideboardingMode(AiProfileUtil.AISideboardingMode.normalizedValueOf(FModel.getPreferences().getPref(FPref.MATCH_AI_SIDEBOARDING_MODE)));

        // Generate Deck Gen matrix
        if(FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_CARDBASED)) {
            boolean commanderDeckGenMatrixLoaded=CardRelationMatrixGenerator.initialize();
            deckGenMatrixLoaded=CardArchetypeLDAGenerator.initialize();
            if(!commanderDeckGenMatrixLoaded){
                deckGenMatrixLoaded=false;
            }
        }

        if (GuiBase.getInterface().isLibgdxPort() && GuiBase.getDeviceRAM() < 5000)
            return; // Don't preload ItemPool on mobile port with less than 5GB RAM

        // Common ItemPool to preload
        allCardsNoAlt = getAllCardsNoAlt();
        archenemyCards = getArchenemyCards();
        planechaseCards = getPlanechaseCards();
        attractionPool = getAttractionPool();
        contraptionPool = getContraptionPool();
        if (GuiBase.getInterface().isLibgdxPort()) {
            // Preload mobile Itempool
            uniqueCardsNoAlt = getUniqueCardsNoAlt();
        } else {
            // Preload Desktop Itempool
            commanderPool = getCommanderPool();
            brawlCommander = getBrawlCommander();
            tinyLeadersCommander = getTinyLeadersCommander();
            avatarPool = getAvatarPool();
            conspiracyPool = getConspiracyPool();
        }
    }

    private static boolean deckGenMatrixLoaded=false;

    public static boolean isdeckGenMatrixLoaded(){
        return deckGenMatrixLoaded;
    }

    public static QuestController getQuest() {
        if (quest == null)
            quest = new QuestController();
        return quest;
    }

    public static ConquestController getConquest() {
        if (conquest == null)
            conquest = new ConquestController();
        return conquest;
    }

    public static ItemPool<PaperCard> getUniqueCardsNoAlt() {
        if (uniqueCardsNoAlt == null)
            return ItemPool.createFrom(getMagicDb().getCommonCards().getUniqueCardsNoAlt(), PaperCard.class);
        return uniqueCardsNoAlt;
    }

    public static ItemPool<PaperCard> getAllCardsNoAlt() {
        if (allCardsNoAlt == null)
            return ItemPool.createFrom(getMagicDb().getCommonCards().getAllCardsNoAlt(), PaperCard.class);
        return allCardsNoAlt;
    }

    public static ItemPool<PaperCard> getArchenemyCards() {
        if (archenemyCards == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(CardRulesPredicates.IS_SCHEME)), PaperCard.class);
        return archenemyCards;
    }

    public static ItemPool<PaperCard> getPlanechaseCards() {
        if (planechaseCards == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(CardRulesPredicates.IS_PLANE_OR_PHENOMENON)), PaperCard.class);
        return planechaseCards;
    }

    public static ItemPool<PaperCard> getBrawlCommander() {
        if (brawlCommander == null) {
            return ItemPool.createFrom(getMagicDb().getCommonCards().getAllCardsNoAlt(
                    FModel.getFormats().get("Brawl").getFilterPrinted()
                            .and(PaperCardPredicates.fromRules(CardRulesPredicates.CAN_BE_BRAWL_COMMANDER))
            ), PaperCard.class);
        }
        return brawlCommander;
    }

    public static ItemPool<PaperCard> getOathbreakerCommander() {
        if (oathbreakerCommander == null)
            return ItemPool.createFrom(getMagicDb().getCommonCards().getAllCardsNoAlt(PaperCardPredicates.fromRules(
                    CardRulesPredicates.CAN_BE_OATHBREAKER.or(CardRulesPredicates.CAN_BE_SIGNATURE_SPELL))), PaperCard.class);
        return oathbreakerCommander;
    }

    public static ItemPool<PaperCard> getTinyLeadersCommander() {
        if (tinyLeadersCommander == null)
            return ItemPool.createFrom(getMagicDb().getCommonCards().getAllCardsNoAlt(PaperCardPredicates.fromRules(
                    CardRulesPredicates.CAN_BE_TINY_LEADERS_COMMANDER)), PaperCard.class);
        return tinyLeadersCommander;
    }

    public static ItemPool<PaperCard> getCommanderPool() {
        if (commanderPool == null)
            return ItemPool.createFrom(getMagicDb().getCommonCards().getAllCardsNoAlt(PaperCardPredicates.CAN_BE_COMMANDER), PaperCard.class);
        return commanderPool;
    }

    public static ItemPool<PaperCard> getAvatarPool() {
        if (avatarPool == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(
                    CardRulesPredicates.IS_VANGUARD)), PaperCard.class);
        return avatarPool;
    }

    public static ItemPool<PaperCard> getConspiracyPool() {
        if (conspiracyPool == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(
                    CardRulesPredicates.IS_CONSPIRACY)), PaperCard.class);
        return conspiracyPool;
    }

    public static ItemPool<PaperCard> getDungeonPool() {
        if (dungeonPool == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(
                    CardRulesPredicates.IS_DUNGEON)), PaperCard.class);
        return dungeonPool;
    }

    public static ItemPool<PaperCard> getAttractionPool() {
        if (attractionPool == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(
                    CardRulesPredicates.IS_ATTRACTION)), PaperCard.class);
        return attractionPool;
    }

    public static ItemPool<PaperCard> getContraptionPool() {
        if(contraptionPool == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(
                    CardRulesPredicates.IS_CONTRAPTION)), PaperCard.class);
        return contraptionPool;
    }

    private static boolean keywordsLoaded = false;

    /**
     * Load dynamic gamedata.
     */
    public static void loadDynamicGamedata() {
        if (!CardType.Constant.LOADED.isSet()) {
            
            final Map<String, List<String>> contents = FileSection.parseSections(FileUtil.readFile(ForgeConstants.TYPE_LIST_FILE));
            
            for (String sectionName: contents.keySet()) {
                CardType.Helper.parseTypes(sectionName, contents.get(sectionName));
            }

            CardType.Constant.LOADED.set();
        }

        if (!keywordsLoaded) {
            final List<String> nskwListFile = FileUtil.readFile(ForgeConstants.KEYWORD_LIST_FILE);

            if (nskwListFile.size() > 1) {
                for (final String s : nskwListFile) {
                    if (s.length() > 1) {
                        CardUtil.NON_STACKING_LIST.add(s);
                    }
                }
            }
            keywordsLoaded = true;
        }
    }

    public static StaticData getMagicDb() {
        return magicDb;
    }

    public static ForgePreferences getPreferences() {
        return preferences;
    }
    public static ForgeNetPreferences getNetPreferences() {
        if (netPreferences == null)
            netPreferences = new ForgeNetPreferences();
        return netPreferences;
    }

    public static AchievementCollection getAchievements(GameType gameType) {
        switch (gameType) { // Translate gameType to appropriate type if needed
        case Constructed:
        case Draft:
        case Sealed:
        case Quest:
        case PlanarConquest:
        case Puzzle:
        case Adventure:
            break;
        case AdventureEvent:
            gameType = GameType.Adventure;
            break;
        case QuestDraft:
            gameType = GameType.Quest;
            break;
        default:
            gameType = GameType.Constructed;
            break;
        }
        return achievements.get(gameType);
    }

    public static IStorage<CardBlock> getBlocks() {
        return blocks;
    }

    public static QuestPreferences getQuestPreferences() {
        if (questPreferences == null)
            questPreferences = new QuestPreferences();
        return questPreferences;
    }

    public static ConquestPreferences getConquestPreferences() {
        if (conquestPreferences == null) {
            conquestPreferences = new ConquestPreferences();
            // initialize on first call...
            ConquestUtil.updateRarityFilterOdds(conquestPreferences);
        }
        return conquestPreferences;
    }

    public static GauntletData getGauntletData() {
        return gauntletData;
    }

    public static void setGauntletData(final GauntletData data0) {
        gauntletData = data0;
    }

    public static GauntletMini getGauntletMini() {
        if (gauntlet == null) {
            gauntlet = new GauntletMini();
        }
        return gauntlet;
    }

    public static CardCollections getDecks() {
        if (decks == null)
            decks = new CardCollections();
        return decks;
    }

    public static IStorage<ConquestPlane> getPlanes() {
        return planes;
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

    public static IStorage<ThemedChaosDraft> getThemedChaosDrafts() {
        return themedChaosDrafts;
    }

    public static TournamentData getTournamentData() { return tournamentData; }

    public static void setTournamentData(TournamentData tournamentData) { FModel.tournamentData = tournamentData;  }
}
