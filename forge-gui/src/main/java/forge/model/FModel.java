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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import forge.CardStorageReader;
import forge.CardStorageReader.ProgressObserver;
import forge.ImageKeys;
import forge.MulliganDefs;
import forge.StaticData;
import forge.ai.AiProfileUtil;
import forge.card.CardRulesPredicates;
import forge.card.CardType;
import forge.deck.CardArchetypeLDAGenerator;
import forge.deck.CardRelationMatrixGenerator;
import forge.deck.io.DeckPreferences;
import forge.download.AutoUpdater;
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
import forge.itemmanager.ItemManagerConfig;
import forge.localinstance.achievements.*;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.player.GamePlayerUtil;
import forge.util.*;
import forge.util.storage.IStorage;
import forge.util.storage.StorageBase;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static ItemPool<PaperCard> uniqueCardsNoAlt, allCardsNoAlt, planechaseCards, archenemyCards, brawlCommander, oathbreakerCommander, tinyLeadersCommander, commanderPool, avatarPool, conspiracyPool;

    public static void initialize(final IProgressBar progressBar, Function<ForgePreferences, Void> adjustPrefs) {
        //init version to log
        System.out.println("Forge v." + GuiBase.getInterface().getCurrentVersion() + " (" + GuiBase.getInterface() + ")");
        //Device
        if (GuiBase.isAndroid()) //todo get device on other mobile platforms
            System.out.println(GuiBase.getDeviceName() + " (RAM: " + GuiBase.getDeviceRAM() + "MB, Android " + GuiBase.getAndroidRelease() + " API Level " + GuiBase.getAndroidAPILevel() + ")");
        else
            System.out.println(System.getProperty("os.name") + " (" + System.getProperty("os.version") + " " + System.getProperty("os.arch") + ")");

        ImageKeys.initializeDirs(
                ForgeConstants.CACHE_CARD_PICS_DIR, ForgeConstants.CACHE_CARD_PICS_SUBDIR,
                ForgeConstants.CACHE_TOKEN_PICS_DIR, ForgeConstants.CACHE_ICON_PICS_DIR,
                ForgeConstants.CACHE_BOOSTER_PICS_DIR, ForgeConstants.CACHE_FATPACK_PICS_DIR,
                ForgeConstants.CACHE_BOOSTERBOX_PICS_DIR, ForgeConstants.CACHE_PRECON_PICS_DIR,
                ForgeConstants.CACHE_TOURNAMENTPACK_PICS_DIR);

        // Instantiate preferences: quest and regular
        // Preferences are initialized first so that the splash screen can be translated.
        try {
            preferences = new ForgePreferences();
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
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setDescription(name);
                        progressBar.setPercentMode(usePercents);
                    }
                });
            }

            @Override
            public void report(final int current, final int total) {
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setMaximum(total);
                        progressBar.setValue(current);
                    }
                });
            }
        };

        if (new AutoUpdater(true).attemptToUpdate()) {
            //
        }

        //load card database
        final CardStorageReader reader = new CardStorageReader(ForgeConstants.CARD_DATA_DIR, progressBarBridge,
                FModel.getPreferences().getPrefBoolean(FPref.LOAD_CARD_SCRIPTS_LAZILY));
        final CardStorageReader tokenReader = new CardStorageReader(ForgeConstants.TOKEN_DATA_DIR, progressBarBridge,
                FModel.getPreferences().getPrefBoolean(FPref.LOAD_CARD_SCRIPTS_LAZILY));
        CardStorageReader customReader;
        try {
           customReader  = new CardStorageReader(ForgeConstants.USER_CUSTOM_CARDS_DIR, progressBarBridge,
                    FModel.getPreferences().getPrefBoolean(FPref.LOAD_CARD_SCRIPTS_LAZILY));
        } catch (Exception e) {
            customReader = null;
        }
        magicDb = new StaticData(reader, tokenReader, customReader, ForgeConstants.EDITIONS_DIR,
                                 ForgeConstants.USER_CUSTOM_EDITIONS_DIR, ForgeConstants.BLOCK_DATA_DIR, ForgeConstants.SETLOOKUP_DIR,
                                 FModel.getPreferences().getPref(FPref.UI_PREFERRED_ART),
                                 FModel.getPreferences().getPrefBoolean(FPref.UI_LOAD_UNKNOWN_CARDS),
                                 FModel.getPreferences().getPrefBoolean(FPref.UI_LOAD_NONLEGAL_CARDS),
                                 FModel.getPreferences().getPrefBoolean(FPref.ALLOW_CUSTOM_CARDS_IN_DECKS_CONFORMANCE),
                                 FModel.getPreferences().getPrefBoolean(FPref.UI_SMART_CARD_ART)
                );
        CardTranslation.preloadTranslation(preferences.getPref(FPref.UI_LANGUAGE), ForgeConstants.LANG_DIR);

        //create profile dirs if they don't already exist
        for (final String dname : ForgeConstants.PROFILE_DIRS) {
            final File path = new File(dname);
            if (path.isDirectory()) {
                // already exists
                continue;
            }
            if (!path.mkdirs()) {
                throw new RuntimeException("cannot create profile directory: " + dname);
            }
        }

        ForgePreferences.DEV_MODE = preferences.getPrefBoolean(FPref.DEV_MODE_ENABLED);
        ForgePreferences.UPLOAD_DRAFT = ForgePreferences.NET_CONN;

        formats = new GameFormat.Collection(new GameFormat.Reader( new File(ForgeConstants.FORMATS_DATA_DIR),
                new File(ForgeConstants.USER_FORMATS_DIR), preferences.getPrefBoolean(FPref.LOAD_HISTORIC_FORMATS)));

        magicDb.setStandardPredicate(formats.getStandard().getFilterRules());
        magicDb.setPioneerPredicate(formats.getPioneer().getFilterRules());
        magicDb.setModernPredicate(formats.getModern().getFilterRules());
        magicDb.setCommanderPredicate(formats.get("Commander").getFilterRules());
        magicDb.setOathbreakerPredicate(formats.get("Oathbreaker").getFilterRules());
        magicDb.setBrawlPredicate(formats.get("Brawl").getFilterRules());

        magicDb.setFilteredHandsEnabled(preferences.getPrefBoolean(FPref.FILTERED_HANDS));
        magicDb.setMulliganRule(MulliganDefs.MulliganRule.valueOf(preferences.getPref(FPref.MULLIGAN_RULE)));

        blocks = new StorageBase<>("Block definitions", new CardBlock.Reader(ForgeConstants.BLOCK_DATA_DIR + "blocks.txt", magicDb.getEditions()));
        questPreferences = new QuestPreferences();
        conquestPreferences = new ConquestPreferences();
        fantasyBlocks = new StorageBase<>("Custom blocks", new CardBlock.Reader(ForgeConstants.BLOCK_DATA_DIR + "fantasyblocks.txt", magicDb.getEditions()));
        themedChaosDrafts = new StorageBase<>("Themed Chaos Drafts", new ThemedChaosDraft.Reader(ForgeConstants.BLOCK_DATA_DIR + "chaosdraftthemes.txt"));
        planes = new StorageBase<>("Conquest planes", new ConquestPlane.Reader(ForgeConstants.CONQUEST_PLANES_DIR + "planes.txt"));
        Map<String, QuestWorld> standardWorlds = new QuestWorld.Reader(ForgeConstants.QUEST_WORLD_DIR + "worlds.txt").readAll();
        Map<String, QuestWorld> customWorlds = new QuestWorld.Reader(ForgeConstants.USER_QUEST_WORLD_DIR + "customworlds.txt").readAll();
        for (QuestWorld world:customWorlds.values()){
            world.setCustom(true);
        }
        standardWorlds.putAll(customWorlds);
        worlds = new StorageBase<>("Quest worlds", null, standardWorlds);

        Spell.setPerformanceMode(preferences.getPrefBoolean(FPref.PERFORMANCE_MODE));

        loadDynamicGamedata();

        if (progressBar != null) {
            FThreads.invokeInEdtLater(new Runnable() {
                @Override
                public void run() {
                    progressBar.setDescription(Localizer.getInstance().getMessage("splash.loading.decks"));
                }
            });
        }

        decks = new CardCollections();
        quest = new QuestController();
        conquest = new ConquestController();

        CardPreferences.load();
        DeckPreferences.load();
        ItemManagerConfig.load();
        ConquestUtil.updateRarityFilterOdds();

        achievements = Maps.newHashMap();
        achievements.put(GameType.Constructed, new ConstructedAchievements());
        achievements.put(GameType.Draft, new DraftAchievements());
        achievements.put(GameType.Sealed, new SealedAchievements());
        achievements.put(GameType.Quest, new QuestAchievements());
        achievements.put(GameType.PlanarConquest, new PlanarConquestAchievements());
        achievements.put(GameType.Puzzle, new PuzzleAchievements());

        //preload AI profiles
        AiProfileUtil.loadAllProfiles(ForgeConstants.AI_PROFILE_DIR);

        //generate Deck Gen matrix
        if(!FModel.getPreferences().getPrefBoolean(FPref.LOAD_CARD_SCRIPTS_LAZILY)
                &&FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_CARDBASED)) {
            boolean commanderDeckGenMatrixLoaded=CardRelationMatrixGenerator.initialize();
            deckGenMatrixLoaded=CardArchetypeLDAGenerator.initialize();
            if(!commanderDeckGenMatrixLoaded){
                deckGenMatrixLoaded=false;
            }
        }

        if (GuiBase.getInterface().isLibgdxPort() && GuiBase.getDeviceRAM() < 5000)
            return; // don't preload ItemPool on mobile port with less than 5GB RAM

        //common ItemPool to preload
        allCardsNoAlt = getAllCardsNoAlt();
        archenemyCards = getArchenemyCards();
        planechaseCards = getPlanechaseCards();
        if (GuiBase.getInterface().isLibgdxPort()) {
            //preload mobile Itempool
            uniqueCardsNoAlt = getUniqueCardsNoAlt();
        } else {
            //preload Desktop Itempool
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
        return quest;
    }

    public static ConquestController getConquest() {
        return conquest;
    }

    public static ItemPool<PaperCard> getUniqueCardsNoAlt() {
        if (uniqueCardsNoAlt == null)
            return ItemPool.createFrom(Iterables.concat(getMagicDb().getCommonCards().getUniqueCardsNoAlt(), getMagicDb().getCustomCards().getUniqueCardsNoAlt()), PaperCard.class);
        return uniqueCardsNoAlt;
    }

    public static ItemPool<PaperCard> getAllCardsNoAlt() {
        if (allCardsNoAlt == null)
            return ItemPool.createFrom(Iterables.concat(getMagicDb().getCommonCards().getAllCardsNoAlt(), getMagicDb().getCustomCards().getAllCardsNoAlt()), PaperCard.class);
        return allCardsNoAlt;
    }

    public static ItemPool<PaperCard> getArchenemyCards() {
        if (archenemyCards == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_SCHEME, PaperCard.FN_GET_RULES)), PaperCard.class);
        return archenemyCards;
    }

    public static ItemPool<PaperCard> getPlanechaseCards() {
        if (planechaseCards == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_PLANE_OR_PHENOMENON, PaperCard.FN_GET_RULES)), PaperCard.class);
        return planechaseCards;
    }

    public static ItemPool<PaperCard> getBrawlCommander() {
        if (brawlCommander == null)
            return ItemPool.createFrom(Iterables.concat(getMagicDb().getCommonCards().getAllCardsNoAlt(Predicates.and(
                    FModel.getFormats().get("Brawl").getFilterPrinted(), Predicates.compose(CardRulesPredicates.Presets.CAN_BE_BRAWL_COMMANDER, PaperCard.FN_GET_RULES))), getMagicDb().getCustomCards().getAllCardsNoAlt(Predicates.and(
                    FModel.getFormats().get("Brawl").getFilterPrinted(), Predicates.compose(CardRulesPredicates.Presets.CAN_BE_BRAWL_COMMANDER, PaperCard.FN_GET_RULES)))), PaperCard.class);
        return brawlCommander;
    }

    public static ItemPool<PaperCard> getOathbreakerCommander() {
        if (oathbreakerCommander == null)
            return ItemPool.createFrom(Iterables.concat(
                    getMagicDb().getCommonCards().getAllCardsNoAlt(Predicates.compose(Predicates.or(CardRulesPredicates.Presets.CAN_BE_OATHBREAKER, CardRulesPredicates.Presets.CAN_BE_SIGNATURE_SPELL), PaperCard.FN_GET_RULES)),
                    getMagicDb().getCustomCards().getAllCardsNoAlt(Predicates.compose(Predicates.or(CardRulesPredicates.Presets.CAN_BE_OATHBREAKER, CardRulesPredicates.Presets.CAN_BE_SIGNATURE_SPELL), PaperCard.FN_GET_RULES))), PaperCard.class);
        return oathbreakerCommander;
    }

    public static ItemPool<PaperCard> getTinyLeadersCommander() {
        if (tinyLeadersCommander == null)
            return ItemPool.createFrom(Iterables.concat(
                    getMagicDb().getCommonCards().getAllCardsNoAlt(Predicates.compose(CardRulesPredicates.Presets.CAN_BE_TINY_LEADERS_COMMANDER, PaperCard.FN_GET_RULES)),
                    getMagicDb().getCustomCards().getAllCardsNoAlt(Predicates.compose(CardRulesPredicates.Presets.CAN_BE_TINY_LEADERS_COMMANDER, PaperCard.FN_GET_RULES))), PaperCard.class);
        return tinyLeadersCommander;
    }

    public static ItemPool<PaperCard> getCommanderPool() {
        if (commanderPool == null)
            return ItemPool.createFrom(Iterables.concat(
                    getMagicDb().getCommonCards().getAllCardsNoAlt(Predicates.compose(CardRulesPredicates.Presets.CAN_BE_COMMANDER, PaperCard.FN_GET_RULES)),
                    getMagicDb().getCustomCards().getAllCardsNoAlt(Predicates.compose(CardRulesPredicates.Presets.CAN_BE_COMMANDER, PaperCard.FN_GET_RULES))), PaperCard.class);
        return commanderPool;
    }

    public static ItemPool<PaperCard> getAvatarPool() {
        if (avatarPool == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_VANGUARD, PaperCard.FN_GET_RULES)), PaperCard.class);
        return avatarPool;
    }

    public static ItemPool<PaperCard> getConspiracyPool() {
        if (conspiracyPool == null)
            return ItemPool.createFrom(getMagicDb().getVariantCards().getAllCards(Predicates.compose(CardRulesPredicates.Presets.IS_CONSPIRACY, PaperCard.FN_GET_RULES)), PaperCard.class);
        return conspiracyPool;
    }

    private static boolean keywordsLoaded = false;

    /**
     * Load dynamic gamedata.
     */
    public static void loadDynamicGamedata() {
        if (!CardType.Constant.LOADED.isSet()) {
            final List<String> typeListFile = FileUtil.readFile(ForgeConstants.TYPE_LIST_FILE);

            Set<String> addTo = null;

            for (final String s : typeListFile) {
                if (s.equals("[BasicTypes]")) {
                    addTo = CardType.Constant.BASIC_TYPES;
                } else if (s.equals("[LandTypes]")) {
                    addTo = CardType.Constant.LAND_TYPES;
                } else if (s.equals("[CreatureTypes]")) {
                    addTo = CardType.Constant.CREATURE_TYPES;
                } else if (s.equals("[SpellTypes]")) {
                    addTo = CardType.Constant.SPELL_TYPES;
                } else if (s.equals("[EnchantmentTypes]")) {
                    addTo = CardType.Constant.ENCHANTMENT_TYPES;
                } else if (s.equals("[ArtifactTypes]")) {
                    addTo = CardType.Constant.ARTIFACT_TYPES;
                } else if (s.equals("[WalkerTypes]")) {
                    addTo = CardType.Constant.WALKER_TYPES;
                } else if (s.length() > 1) {
                    if (addTo != null) {
                        if (s.contains(":")) {
                            String[] k = s.split(":");
                            addTo.add(k[0]);
                            CardType.Constant.pluralTypes.put(k[0], k[1]);
                        } else {
                            addTo.add(s);
                        }
                    }
                }
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

    public static AchievementCollection getAchievements(GameType gameType) {
        switch (gameType) { //translate gameType to appropriate type if needed
        case Constructed:
        case Draft:
        case Sealed:
        case Quest:
        case PlanarConquest:
        case Puzzle:
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
        return questPreferences;
    }

    public static ConquestPreferences getConquestPreferences() {
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
