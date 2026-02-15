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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
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

    private static CardStorageReader reader, tokenReader, customReader, customTokenReader;
    private static final Supplier<StaticData> magicDb = Suppliers.memoize(() ->
        new StaticData(reader, tokenReader, customReader, customTokenReader, ForgeConstants.EDITIONS_DIR,
            ForgeConstants.USER_CUSTOM_EDITIONS_DIR, ForgeConstants.BLOCK_DATA_DIR, ForgeConstants.SETLOOKUP_DIR,
            getPreferences().getPref(FPref.UI_PREFERRED_ART),
            getPreferences().getPrefBoolean(FPref.UI_LOAD_UNKNOWN_CARDS),
            getPreferences().getPrefBoolean(FPref.UI_LOAD_NONLEGAL_CARDS),
            getPreferences().getPrefBoolean(FPref.ALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE),
            getPreferences().getPrefBoolean(FPref.UI_SMART_CARD_ART),
                       getPreferences().getPrefBoolean(FPref.UI_ENABLE_SMART_TOKEN_ART)));
    private static final Supplier<QuestPreferences> questPreferences = Suppliers.memoize(QuestPreferences::new);
    private static final Supplier<ConquestPreferences> conquestPreferences = Suppliers.memoize(() -> {
       final ConquestPreferences cp = new ConquestPreferences();
       ConquestUtil.updateRarityFilterOdds(cp);
       return cp;
    });
    private static ForgePreferences preferences;
    private static final Supplier<ForgeNetPreferences> netPreferences = Suppliers.memoize(ForgeNetPreferences::new);
    private static final Supplier<Map<GameType, AchievementCollection>> achievements = Suppliers.memoize(() -> {
        final Map<GameType, AchievementCollection> a = Maps.newHashMap();
        a.put(GameType.Constructed, new ConstructedAchievements());
        a.put(GameType.Draft, new DraftAchievements());
        a.put(GameType.Sealed, new SealedAchievements());
        a.put(GameType.Quest, new QuestAchievements());
        a.put(GameType.PlanarConquest, new PlanarConquestAchievements());
        a.put(GameType.Puzzle, new PuzzleAchievements());
        a.put(GameType.Adventure, new AdventureAchievements());
        return a;
    });

    // Someone should take care of 2 gauntlets here
    private static TournamentData tournamentData;
    private static GauntletData gauntletData;
    private static final Supplier<GauntletMini> gauntletMini = Suppliers.memoize(GauntletMini::new);

    private static final Supplier<QuestController> quest = Suppliers.memoize(QuestController::new);
    private static final Supplier<ConquestController> conquest = Suppliers.memoize(ConquestController::new);
    private static final Supplier<CardCollections> decks = Suppliers.memoize(CardCollections::new);

    private static final Supplier<IStorage<CardBlock>> blocks = Suppliers.memoize(() -> {
        final IStorage<CardBlock> cb = new StorageBase<>("Block definitions", new CardBlock.Reader(ForgeConstants.BLOCK_DATA_DIR + "blocks.txt", getMagicDb().getEditions()));
        // SetblockLands
        for (final CardBlock b : cb) {
            try {
                getMagicDb().getBlockLands().add(b.getLandSet().getCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return cb;
    });
    private static final Supplier<IStorage<CardBlock>> fantasyBlocks = Suppliers.memoize(() -> new StorageBase<>("Custom blocks", new CardBlock.Reader(ForgeConstants.BLOCK_DATA_DIR + "fantasyblocks.txt", getMagicDb().getEditions())));
    private static final Supplier<IStorage<ThemedChaosDraft>> themedChaosDrafts = Suppliers.memoize(() -> new StorageBase<>("Themed Chaos Drafts", new ThemedChaosDraft.Reader(ForgeConstants.BLOCK_DATA_DIR + "chaosdraftthemes.txt")));
    private static final Supplier<IStorage<ConquestPlane>> planes = Suppliers.memoize(() -> new StorageBase<>("Conquest planes", new ConquestPlane.Reader(ForgeConstants.CONQUEST_PLANES_DIR + "planes.txt")));
    private static final Supplier<IStorage<QuestWorld>> worlds = Suppliers.memoize(() -> {
        final Map<String, QuestWorld> standardWorlds = new QuestWorld.Reader(ForgeConstants.QUEST_WORLD_DIR + "worlds.txt").readAll();
        final Map<String, QuestWorld> customWorlds = new QuestWorld.Reader(ForgeConstants.USER_QUEST_WORLD_DIR + "customworlds.txt").readAll();
        customWorlds.values().forEach(world -> world.setCustom(true));
        standardWorlds.putAll(customWorlds);
        final IStorage<QuestWorld> w = new StorageBase<>("Quest worlds", null, standardWorlds);
        return w;
    });
    private static final Supplier<GameFormat.Collection> formats = Suppliers.memoize(() -> new GameFormat.Collection(new GameFormat.Reader( new File(ForgeConstants.FORMATS_DATA_DIR), new File(ForgeConstants.USER_FORMATS_DIR), preferences.getPrefBoolean(FPref.LOAD_ARCHIVED_FORMATS))));
    private static final Supplier<ItemPool<PaperCard>> uniqueCardsNoAlt = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getCommonCards().getUniqueCardsNoAlt(), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> allCardsNoAlt = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getCommonCards().getAllCardsNoAlt(), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> planechaseCards = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(CardRulesPredicates.IS_PLANE_OR_PHENOMENON)), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> archenemyCards = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(CardRulesPredicates.IS_SCHEME)), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> brawlCommander = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getCommonCards().getAllCardsNoAlt(getFormats().get("Brawl").getFilterPrinted().and(PaperCardPredicates.fromRules(CardRulesPredicates.CAN_BE_BRAWL_COMMANDER))), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> oathbreakerCommander = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getCommonCards().getAllCardsNoAlt(PaperCardPredicates.fromRules(CardRulesPredicates.CAN_BE_OATHBREAKER.or(CardRulesPredicates.CAN_BE_SIGNATURE_SPELL))), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> tinyLeadersCommander = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getCommonCards().getAllCardsNoAlt(PaperCardPredicates.fromRules(CardRulesPredicates.CAN_BE_TINY_LEADERS_COMMANDER)), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> commanderPool = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getCommonCards().getAllCardsNoAlt(PaperCardPredicates.CAN_BE_COMMANDER), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> avatarPool = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(CardRulesPredicates.IS_VANGUARD)), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> conspiracyPool = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(CardRulesPredicates.IS_CONSPIRACY)), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> dungeonPool = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(CardRulesPredicates.IS_DUNGEON)), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> attractionPool = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(CardRulesPredicates.IS_ATTRACTION)), PaperCard.class));
    private static final Supplier<ItemPool<PaperCard>> contraptionPool = Suppliers.memoize(() -> ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(PaperCardPredicates.fromRules(CardRulesPredicates.IS_CONTRAPTION)), PaperCard.class));

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

        Lang.createInstance(getPreferences().getPref(FPref.UI_LANGUAGE));
        Localizer.getInstance().initialize(getPreferences().getPref(FPref.UI_LANGUAGE), ForgeConstants.LANG_DIR);

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
        reader = new CardStorageReader(ForgeConstants.CARD_DATA_DIR, progressBarBridge,
                false);
        tokenReader = new CardStorageReader(ForgeConstants.TOKEN_DATA_DIR, progressBarBridge,
                false);

        try {
           customReader  = new CardStorageReader(ForgeConstants.USER_CUSTOM_CARDS_DIR, progressBarBridge, false);
        } catch (Exception e) {
            customReader = null;
        }

        try {
            customTokenReader  = new CardStorageReader(ForgeConstants.USER_CUSTOM_TOKENS_DIR, progressBarBridge, false);
        } catch (Exception e) {
            customTokenReader = null;
        }

        // Do this first so PaperCards see the real preference
        CardTranslation.preloadTranslation(preferences.getPref(FPref.UI_LANGUAGE), ForgeConstants.LANG_DIR);

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

        getMagicDb().setStandardPredicate(getFormats().getStandard().getFilterRules());
        getMagicDb().setPioneerPredicate(getFormats().getPioneer().getFilterRules());
        getMagicDb().setModernPredicate(getFormats().getModern().getFilterRules());
        getMagicDb().setCommanderPredicate(getFormats().get("Commander").getFilterRules());
        getMagicDb().setOathbreakerPredicate(getFormats().get("Oathbreaker").getFilterRules());
        getMagicDb().setBrawlPredicate(getFormats().get("Brawl").getFilterRules());

        getMagicDb().setFilteredHandsEnabled(preferences.getPrefBoolean(FPref.FILTERED_HANDS));
        try {
            getMagicDb().setMulliganRule(MulliganDefs.MulliganRule.valueOf(preferences.getPref(FPref.MULLIGAN_RULE)));
        } catch(Exception e) {
            getMagicDb().setMulliganRule(MulliganDefs.MulliganRule.London);
        }

        Spell.setPerformanceMode(preferences.getPrefBoolean(FPref.PERFORMANCE_MODE));

        if (progressBar != null) {
            FThreads.invokeInEdtLater(() -> progressBar.setDescription(Localizer.getInstance().getMessage("splash.loading.decks")));
        }

        CardPreferences.load();
        DeckPreferences.load();
        ItemManagerConfig.load();

        // Preload AI profiles
        AiProfileUtil.loadAllProfiles(ForgeConstants.AI_PROFILE_DIR);
        AiProfileUtil.setAiSideboardingMode(AiProfileUtil.AISideboardingMode.normalizedValueOf(getPreferences().getPref(FPref.MATCH_AI_SIDEBOARDING_MODE)));

        // Generate Deck Gen matrix
        if(getPreferences().getPrefBoolean(FPref.DECKGEN_CARDBASED)) {
            boolean commanderDeckGenMatrixLoaded=CardRelationMatrixGenerator.initialize();
            deckGenMatrixLoaded=CardArchetypeLDAGenerator.initialize();
            if(!commanderDeckGenMatrixLoaded){
                deckGenMatrixLoaded=false;
            }
        }
    }

    private static boolean deckGenMatrixLoaded = false;

    public static boolean isdeckGenMatrixLoaded(){
        return deckGenMatrixLoaded;
    }

    public static QuestController getQuest() {
        return quest.get();
    }

    public static ConquestController getConquest() {
        return conquest.get();
    }

    public static ItemPool<PaperCard> getUniqueCardsNoAlt() {
        return uniqueCardsNoAlt.get();
    }

    public static ItemPool<PaperCard> getAllCardsNoAlt() {
        return allCardsNoAlt.get();
    }

    public static ItemPool<PaperCard> getArchenemyCards() {
        return archenemyCards.get();
    }

    public static ItemPool<PaperCard> getPlanechaseCards() {
        return planechaseCards.get();
    }

    public static ItemPool<PaperCard> getBrawlCommander() {
        return brawlCommander.get();
    }

    public static ItemPool<PaperCard> getOathbreakerCommander() {
        return oathbreakerCommander.get();
    }

    public static ItemPool<PaperCard> getTinyLeadersCommander() {
        return tinyLeadersCommander.get();
    }

    public static ItemPool<PaperCard> getCommanderPool() {
        return commanderPool.get();
    }

    public static ItemPool<PaperCard> getAvatarPool() {
        return avatarPool.get();
    }

    public static ItemPool<PaperCard> getConspiracyPool() {
        return conspiracyPool.get();
    }

    public static ItemPool<PaperCard> getDungeonPool() {
        return dungeonPool.get();
    }

    public static ItemPool<PaperCard> getAttractionPool() {
        return attractionPool.get();
    }

    public static ItemPool<PaperCard> getContraptionPool() {
        return contraptionPool.get();
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
        return magicDb.get();
    }

    public static ForgePreferences getPreferences() {
        return preferences;
    }
    public static ForgeNetPreferences getNetPreferences() {
        return netPreferences.get();
    }

    public static AchievementCollection getAchievements(GameType gameType) {
        // Translate gameType to appropriate type if needed
        return switch (gameType) {
            case Constructed, Draft, Sealed, Quest, PlanarConquest, Puzzle, Adventure -> achievements.get().get(gameType);
            case AdventureEvent -> achievements.get().get(GameType.Adventure);
            case QuestDraft -> achievements.get().get(GameType.Quest);
            default -> achievements.get().get(GameType.Constructed);
        };
    }

    public static IStorage<CardBlock> getBlocks() {
        return blocks.get();
    }

    public static QuestPreferences getQuestPreferences() {
        return questPreferences.get();
    }

    public static ConquestPreferences getConquestPreferences() {
        return conquestPreferences.get();
    }

    public static GauntletData getGauntletData() {
        return gauntletData;
    }

    public static void setGauntletData(final GauntletData data0) {
        gauntletData = data0;
    }

    public static GauntletMini getGauntletMini() {
        return gauntletMini.get();
    }

    public static CardCollections getDecks() {
        return decks.get();
    }

    public static IStorage<ConquestPlane> getPlanes() {
        return planes.get();
    }

    public static IStorage<QuestWorld> getWorlds() {
        return worlds.get();
    }

    public static GameFormat.Collection getFormats() {
        return formats.get();
    }

    public static IStorage<CardBlock> getFantasyBlocks() {
        return fantasyBlocks.get();
    }

    public static IStorage<ThemedChaosDraft> getThemedChaosDrafts() {
        return themedChaosDrafts.get();
    }

    public static TournamentData getTournamentData() { return tournamentData; }

    public static void setTournamentData(TournamentData tournamentData0) { tournamentData = tournamentData0;  }
}
