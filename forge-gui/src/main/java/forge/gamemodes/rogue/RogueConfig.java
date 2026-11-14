package forge.gamemodes.rogue;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.CardType;
import forge.card.ICardFace;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.deck.io.DeckSerializer;
import forge.gamemodes.rogue.RogueRun.CarryCardType;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.util.FileSection;
import forge.util.FileUtil;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.tinylog.Logger;

/**
 * Configuration for Rogue Commander mode.
 * Decks are loaded from res/rogue/commanders/ directory.
 * Paths are generated for each run.
 */
public class RogueConfig {
    private RogueConfig() {
    }

    private static final Set<String> EXCLUDED_RANDOM_DRAW_SET_CODES = ImmutableSet.of("UNK", "UNH");
    private static final Set<String> DEPRIORITIZED_RANDOM_PRINT_SET_CODES =
        ImmutableSet.of("SLZ", "MAR", "FCA", "TLE", "PF25", "PF26", "SLD", "PSPL", "MBC", "SCH", "HOC",
            "PZA", "OTP", "PIO", "KLR", "EA2", "PSVC", "PRM");
    private static final String FALLBACK_LAND_SET = "MH3";
    private static final String WASTES_FALLBACK_LAND_SET = "M3C";

    private static final StaticData db = StaticData.instance();

    // Cache all plane cards to avoid reloading them repeatedly
    private static CardPool cachedPlanarPool = null;
    private static List<PaperCard> cachedGamechangerCards = null;
    private static List<PaperCard> cachedCommanderBanlistCards = null;

    private static final Supplier<Map<String, PaperCard>> rogueCards =
            Suppliers.memoize(RogueConfig::loadRogueCards);

    public static PaperCard getRogueCard(String name) {
        return name == null ? null : rogueCards.get().get(name);
    }

    private static Map<String, PaperCard> loadRogueCards() {
        Map<String, PaperCard> cards = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        File cardsDir = new File(ForgeConstants.RES_DIR, "rogue/cards");
        File[] cardFiles = cardsDir.listFiles((dir, name) -> name.endsWith(".txt"));
        if (cardFiles == null) {
            return cards;
        }

        CardRules.Reader rulesReader = new CardRules.Reader();
        for (File cardFile : cardFiles) {
            try (FileInputStream fis = new FileInputStream(cardFile);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
                rulesReader.reset();
                List<String> lines = FileUtil.readAllLines(isr, true);
                String name = cardFile.getName();
                String baseName = name.substring(0, name.length() - 4);
                CardRules rules = rulesReader.readCard(lines, baseName);
                rules.setCustom();
                PaperCard card = new PaperCard(rules, CardEdition.UNKNOWN_CODE, CardRarity.Special);
                // Forge's image lookup searches variant cards without adding them to the normal deck catalog.
                db.getVariantCards().addCard(card);
                cards.put(card.getName(), card);
            } catch (Exception e) {
                Logger.error(e, "Error loading Rogue card {}", cardFile.getName());
            }
        }
        return cards;
    }

    /**
     * Load all available Rogue Decks from the commanders directory.
     * Scans for .dck files and their corresponding _rewards.dck and .meta files.
     */
    public static List<RogueDeck> loadRogueDecks() {
        List<RogueDeck> decks = new ArrayList<>();
        File commanderDir = new File(ForgeConstants.RES_DIR, "rogue/commanders");

        if (!commanderDir.exists() || !commanderDir.isDirectory()) {
            Logger.warn("Rogue commanders directory not found: {}", commanderDir.getAbsolutePath());
            return decks;
        }

        File[] files = commanderDir.listFiles((dir, name) ->
                name.endsWith(".dck") && !name.endsWith("_rewards.dck"));

        if (files == null || files.length == 0) {
            Logger.warn("No commander deck files found in {}", commanderDir.getAbsolutePath());
            return decks;
        }

        for (File deckFile : files) {
            try {
                RogueDeck rogueDeck = loadRogueDeckFromFile(deckFile);
                if (rogueDeck != null) {
                    decks.add(rogueDeck);
                }
            } catch (Exception e) {
                Logger.error(e, "Error loading Rogue deck from {}", deckFile.getName());
            }
        }

        return decks;
    }

    /**
     * Load a single Rogue Deck from a file.
     * Expected files: [name].dck, [name]_rewards.dck, [name].meta
     */
    private static RogueDeck loadRogueDeckFromFile(File deckFile) throws IOException {
        // Extract base name (e.g., "anim_pakal" from "anim_pakal.dck")
        String baseName = deckFile.getName().replace(".dck", "");

        // Load start deck
        Deck startDeck = DeckSerializer.fromFile(deckFile);
        if (startDeck == null) {
            Logger.warn("Failed to load deck from {}", deckFile.getName());
            return null;
        }

        // Load rewards deck
        File rewardsFile = new File(deckFile.getParent(), baseName + "_rewards.dck");
        Deck rewardsDeck = null;
        if (rewardsFile.exists()) {
            rewardsDeck = DeckSerializer.fromFile(rewardsFile);
        }

        // Load metadata
        File metaFile = new File(deckFile.getParent(), baseName + ".meta");
        Properties meta = new Properties();
        if (metaFile.exists()) {
            try (FileInputStream fis = new FileInputStream(metaFile)) {
                meta.load(fis);
            }
        }

        // Create RogueDeckData
        RogueDeck rogueDeck = new RogueDeck();
        rogueDeck.setName(startDeck.getName());
        rogueDeck.setStartDeck(startDeck);

        // Extract commander name from Commander section
        if (startDeck.has(DeckSection.Commander)) {
            CardPool commanders = startDeck.get(DeckSection.Commander);
            if (commanders != null && !commanders.isEmpty()) {
                rogueDeck.setCommanderCardName(commanders.toFlatList().get(0).getName());
            }
        }

        // Set reward pool from rewards deck
        if (rewardsDeck != null) {
            CardPool rewardPool = new CardPool();
            rewardPool.addAll(rewardsDeck.getMain());
            rogueDeck.setRewardPool(rewardPool);
        }

        // Set metadata
        rogueDeck.setDescription(meta.getProperty("description", ""));
        rogueDeck.setThemeDescription(meta.getProperty("theme", ""));
        rogueDeck.setAvatarIndex(Integer.parseInt(meta.getProperty("avatarIndex", "1")));
        rogueDeck.setSleeveIndex(Integer.parseInt(meta.getProperty("sleeveIndex", "1")));
        rogueDeck.setLandEdition(meta.getProperty("landEdition", ""));
        rogueDeck.setIncludeColorlessBasics(Boolean.parseBoolean(
            meta.getProperty("includeColorlessBasics", "false")));
        rogueDeck.setExperimental(Boolean.parseBoolean(meta.getProperty("experimental", "false")));

        // Set unlock condition
        String unlockString = meta.getProperty("unlock", null);
        if (unlockString != null && !unlockString.isEmpty()) {
            rogueDeck.setUnlockCondition(new RogueUnlockCondition(unlockString));
        }

        return rogueDeck;
    }

    /**
     * Get all plane cards from the variant cards collection.
     * Returns a cached CardPool to avoid reloading planes repeatedly.
     * This method is thread-safe and lazy-loads the planes on first call.
     *
     * @return CardPool containing all available plane cards
     */
    public static CardPool getAllPlanes() {
        if (cachedPlanarPool == null) {
            cachedPlanarPool = new CardPool();

            // Search variant cards for planes
            for (PaperCard card : db.getVariantCards().getAllCards()) {
                if (card.getRules().getType().isPlane()) {
                    cachedPlanarPool.add(card);
                }
            }
        }
        return cachedPlanarPool;
    }

    /**
     * Load the shared Gamechanger card pool from res/rogue/util/gamechangers.dck.
     * Returns a cached copy to avoid repeated deck file parsing.
     */
    public static List<PaperCard> getGamechangerCards() {
        if (cachedGamechangerCards == null) {
            File deckFile = new File(ForgeConstants.RES_DIR, "rogue/util/gamechangers.dck");
            if (!deckFile.exists()) {
                Logger.warn("Gamechanger deck not found: {}", deckFile.getAbsolutePath());
                cachedGamechangerCards = ImmutableList.of();
            } else {
                Deck deck = DeckSerializer.fromFile(deckFile);
                if (deck == null) {
                    Logger.warn("Failed to load Gamechanger deck from {}", deckFile.getAbsolutePath());
                    cachedGamechangerCards = ImmutableList.of();
                } else {
                    cachedGamechangerCards = new ArrayList<>(deck.getMain().toFlatList());
                }
            }
        }
        return new ArrayList<>(cachedGamechangerCards);
    }

    /**
     * Load the shared Commander banlist card pool from res/rogue/util/commander_banlist.dck.
     * Returns a cached copy to avoid repeated deck file parsing.
     */
    public static List<PaperCard> getCommanderBanlistCards() {
        if (cachedCommanderBanlistCards == null) {
            File deckFile = new File(ForgeConstants.RES_DIR, "rogue/util/commander_banlist.dck");
            if (!deckFile.exists()) {
                Logger.warn("Commander banlist deck not found: {}", deckFile.getAbsolutePath());
                cachedCommanderBanlistCards = ImmutableList.of();
            } else {
                Deck deck = DeckSerializer.fromFile(deckFile);
                if (deck == null) {
                    Logger.warn("Failed to load Commander banlist deck from {}", deckFile.getAbsolutePath());
                    cachedCommanderBanlistCards = ImmutableList.of();
                } else {
                    cachedCommanderBanlistCards = new ArrayList<>(deck.getMain().toFlatList());
                }
            }
        }
        return new ArrayList<>(cachedCommanderBanlistCards);
    }

    /**
     * Load all available Planebound configurations from the planebounds directory.
     * Scans for .dck files and loads their metadata.
     */
    public static List<RoguePlanebound> loadPlanebounds() {
        List<RoguePlanebound> planebounds = new ArrayList<>();
        File planeboundsDir = new File(ForgeConstants.RES_DIR, "rogue/planebounds");

        if (!planeboundsDir.exists() || !planeboundsDir.isDirectory()) {
            Logger.warn("Rogue planebounds directory not found: {}", planeboundsDir.getAbsolutePath());
            return planebounds;
        }

        File[] files = planeboundsDir.listFiles((dir, name) -> name.endsWith(".dck"));

        if (files == null || files.length == 0) {
            Logger.warn("No planebound deck files found in {}", planeboundsDir.getAbsolutePath());
            return planebounds;
        }

        for (File deckFile : files) {
            try {
                RoguePlanebound planebound = loadPlaneboundFromFile(deckFile);
                if (planebound != null) {
                    planebounds.add(planebound);
                }
            } catch (Exception e) {
                Logger.error(e, "Error loading Planebound from {}", deckFile.getName());
            }
        }

        return planebounds;
    }

    /**
     * Load a single Planebound configuration from a deck file.
     * Reads metadata including name, planeName, and avatarIndex.
     */
    private static RoguePlanebound loadPlaneboundFromFile(File deckFile) {
        // Parse deck file sections
        Map<String, List<String>> sections = FileSection.parseSections(FileUtil.readFile(deckFile));
        if (sections.isEmpty()) {
            Logger.warn("Failed to parse deck file {}", deckFile.getName());
            return null;
        }

        // Extract metadata section
        List<String> metadataLines = sections.get("metadata");
        if (metadataLines == null || metadataLines.isEmpty()) {
            Logger.warn("No metadata section found in {}", deckFile.getName());
            return null;
        }

        // Parse metadata as key-value pairs
        FileSection metadata = FileSection.parse(metadataLines, FileSection.EQUALS_KV_SEPARATOR);

        // Extract required metadata
        String planeboundName = metadata.get("name", "Unknown Planebound");
        String planeName = metadata.get("planeName", "Unknown Plane");
        int avatarIndex = metadata.getInt("avatarIndex", 1);
        int typeIndex = metadata.getInt("type", 0); // Default to NORMAL (0)
        RoguePlaneboundType type = RoguePlaneboundType.fromIndex(typeIndex);

        // Build relative deck path
        String deckPath = "rogue/planebounds/" + deckFile.getName();

        return new RoguePlanebound(planeName, planeboundName, deckPath, avatarIndex, type);
    }

    public static Deck loadPlaneboundDeck(RoguePlanebound planebound) {
        if (planebound == null || StringUtils.isBlank(planebound.deckPath())) {
            return null;
        }

        File deckFile = new File(ForgeConstants.RES_DIR, planebound.deckPath());
        if (!deckFile.exists()) {
            Logger.warn("Planebound deck not found: {}", deckFile.getAbsolutePath());
            return null;
        }
        return DeckSerializer.fromFile(deckFile);
    }

    // Helper method to get all unique cards from the database
    public static List<PaperCard> getAllCards() {
        return getAllCards(null);
    }

    public static List<PaperCard> getAllCards(Predicate<PaperCard> filter) {
        Predicate<PaperCard> effectiveFilter = getAllFilters(filter);

        return FModel.getMagicDb().getCommonCards().getUniqueCards().stream()
                .filter(effectiveFilter)
                .collect(Collectors.toList());
    }

    /** Returns the Bazaar Carry Card type, or null if the card is ineligible. */
    public static CarryCardType getBazaarCarryCardType(PaperCard card) {
        if (card == null || !PaperCardPredicates.IS_RARE_OR_MYTHIC.test(card)
            || card.hasFlavorName() || card.getRules().getSplitType() != CardSplitType.None
            || !DeckFormat.RogueCommander.isLegalCard(card)) {
            return null;
        }

        CardType type = card.getRules().getType();
        if (type.isCreature()) {
            return type.isLegendary() ? CarryCardType.FELLOW : null;
        }
        if (type.isArtifact()) {
            return type.isLegendary() ? CarryCardType.ITEM : null;
        }
        return type.isInstant() || type.isSorcery() ? CarryCardType.SCROLL : null;
    }

    /** Database cards for random Rogue selections, excluding multiplayer-dependent text. */
    public static List<PaperCard> getRandomCards(Predicate<PaperCard> filter) {
        return getRandomCardsForEdition(filter, null);
    }

    /** Random Rogue cards preferring the Commander's edition when one is supplied. */
    public static List<PaperCard> getRandomCardsForEdition(Predicate<PaperCard> filter,
            String preferredEdition) {
        Predicate<PaperCard> eligible = RogueConfig::isEligibleForSingleOpponentDraw;
        Predicate<PaperCard> randomFilter = filter == null ? eligible : filter.and(eligible);
        Predicate<PaperCard> effectiveFilter = getAllFilters(randomFilter);
        return getAllCards(randomFilter).stream()
                .map(card -> findPreferredPrint(card, effectiveFilter, preferredEdition).orElse(card))
                .collect(Collectors.toList());
    }

    /** Finds a non-mythic printing for a generated starting deck, if one qualifies. */
    public static PaperCard getNonMythicRandomPrint(PaperCard card, Predicate<PaperCard> filter,
            String preferredEdition) {
        Predicate<PaperCard> eligible = candidate -> candidate.getRarity() != CardRarity.MythicRare
                && isEligibleForSingleOpponentDraw(candidate);
        Predicate<PaperCard> randomFilter = filter == null ? eligible : filter.and(eligible);
        return findPreferredPrint(card, getAllFilters(randomFilter), preferredEdition).orElse(null);
    }

    private static boolean isEligibleForSingleOpponentDraw(PaperCard card) {
        for (ICardFace face : card.getRules().getAllFaces()) {
            String oracleText = face.getOracleText();
            if (oracleText == null) {
                continue;
            }
            String text = oracleText.toLowerCase(Locale.ROOT);
            if (face.getName() != null) {
                text = text.replace(face.getName().toLowerCase(Locale.ROOT), "");
            }
            if (text.contains("two or more opponents") || text.contains("myriad")
                    || text.contains("melee") || text.contains("undaunted")
                    || text.contains("goad") || text.contains("assist")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a rules-name print for flavor-name cards while leaving ordinary prints unchanged.
     */
    public static PaperCard getRulesNamePrint(PaperCard card) {
        return card != null && card.hasFlavorName() ? getCard(card.getName(), null, null) : card;
    }

    /** Prefers non-deprioritized prints, then the chosen edition, CMM, FDC, FDN, and newest set. */
    private static Optional<PaperCard> findPreferredPrint(PaperCard card, Predicate<PaperCard> filter,
            String preferredEdition) {
        return FModel.getMagicDb().getCommonCards()
                .getAllCards(card).stream()
                .filter(filter)
                .filter(candidate -> !candidate.hasFlavorName())
                .min(Comparator.comparing((PaperCard candidate) ->
                            DEPRIORITIZED_RANDOM_PRINT_SET_CODES.contains(candidate.getEdition()))
                        .thenComparing(candidate -> !candidate.getEdition().equals(preferredEdition))
                        .thenComparing(candidate -> !candidate.getEdition().equals("CMM"))
                        .thenComparing(candidate -> !candidate.getEdition().equals("FDC"))
                        .thenComparing(candidate -> !candidate.getEdition().equals("FDN"))
                        .thenComparing(candidate -> db.getCardEdition(candidate.getEdition()).getDate(),
                                Comparator.reverseOrder())
                        .thenComparingInt(PaperCard::getArtIndex)
                        .thenComparing(PaperCard::getCollectorNumberSortingKey)
                        .thenComparing(PaperCard::getEdition));
    }

    /** Deck setting first, then the actual Commander printing, then the standard land set. */
    public static String getBasicLandEdition(String landEdition, PaperCard commander) {
        if (!StringUtils.isBlank(landEdition)) {
            return landEdition;
        }
        return commander != null && !StringUtils.isBlank(commander.getEdition())
                ? commander.getEdition() : FALLBACK_LAND_SET;
    }

    /** All basic-land arts in an edition for the Rogue Deck Editor. */
    public static List<PaperCard> getBasicLandsFromEdition(String edition) {
        if (StringUtils.isBlank(edition)) {
            return ImmutableList.of();
        }
        List<PaperCard> basics = new ArrayList<>();
        for (Map.Entry<PaperCard, Integer> entry : FModel.getAllCards()) {
            PaperCard card = entry.getKey();
            if (card.getRules().getType().isBasicLand() && edition.equals(card.getEdition())) {
                basics.add(card);
            }
        }
        return basics;
    }

    /** Lowest-index basic-land art from the preferred edition, or its standard fallback. */
    public static PaperCard getBasicLandPrint(String name, String preferredEdition) {
        String fallbackEdition = "Wastes".equals(name) ? WASTES_FALLBACK_LAND_SET : FALLBACK_LAND_SET;
        return db.getCommonCards().getAllCards(name).stream()
                .filter(card -> card.getRules().getType().isBasicLand()
                        && (preferredEdition.equals(card.getEdition())
                            || fallbackEdition.equals(card.getEdition())))
                .min(Comparator.comparing((PaperCard card) -> !preferredEdition.equals(card.getEdition()))
                        .thenComparingInt(PaperCard::getArtIndex)
                        .thenComparing(PaperCard::getCollectorNumberSortingKey))
                .orElse(null);
    }

    public static PaperCard getFallbackBasicLandPrint(String name) {
        String fallbackEdition = "Wastes".equals(name) ? WASTES_FALLBACK_LAND_SET : FALLBACK_LAND_SET;
        return getBasicLandPrint(name, fallbackEdition);
    }

    private static @NonNull Predicate<PaperCard> getAllFilters(Predicate<PaperCard> filter) {
        Predicate<PaperCard> rogueBaseFilter =
            card -> !card.getRules().isCustom()
                && !EXCLUDED_RANDOM_DRAW_SET_CODES.contains(card.getEdition());
        Predicate<PaperCard> nonDigitalFilter =
            card -> {
                CardEdition edition = db.getCardEdition(card.getEdition());
                return !card.isRebalanced()
                    && edition != null
                    && edition.getType() != CardEdition.Type.ONLINE
                    && edition.getType() != CardEdition.Type.FUNNY;
            };
        Predicate<PaperCard> commanderLegalFilter = DeckFormat.RogueCommander::isLegalCard;
        return filter != null
                ? rogueBaseFilter.and(nonDigitalFilter).and(commanderLegalFilter).and(filter)
                : rogueBaseFilter.and(nonDigitalFilter).and(commanderLegalFilter);
    }

    /**
     * Helper method to get cards from the database.
     * setCode and artIndex may be null to ignore edition or art selection.
     */
    public static PaperCard getCard(String cardName, String setCode, Integer artIndex) {
        PaperCard rogueCard = getRogueCard(cardName);
        if (rogueCard != null) {
            return rogueCard;
        }
        PaperCard card;
        if (setCode == null || setCode.isEmpty()) {
            card = db.getCommonCards().getCard(cardName);
        } else if (artIndex == null) {
            card = db.getCommonCards().getCard(cardName, setCode);
        } else {
            card = db.getCommonCards().getCard(cardName, setCode, artIndex);
        }
        if (card == null) {
            Logger.warn("Card not found: {}", cardName);
            // Return a basic land as fallback
            return db.getCommonCards().getCard("Plain");
        }
        return card;
    }
}
