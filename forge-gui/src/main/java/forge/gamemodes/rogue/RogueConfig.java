package forge.gamemodes.rogue;

import forge.StaticData;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.deck.io.DeckSerializer;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Configuration for Rogue Commander mode.
 * Decks are loaded from res/rogue/commanders/ directory.
 * Paths are generated for each run.
 */
public class RogueConfig {

    private static final StaticData db = StaticData.instance();

    // Cache all plane cards to avoid reloading them repeatedly
    private static CardPool cachedPlanarPool = null;

    /**
     * Load all available Rogue Decks from the commanders directory.
     * Scans for .dck files and their corresponding _rewards.dck and .meta files.
     */
    public static List<RogueDeckData> loadRogueDecks() {
        List<RogueDeckData> decks = new ArrayList<>();
        File commanderDir = new File(ForgeConstants.RES_DIR, "rogue/commanders");

        if (!commanderDir.exists() || !commanderDir.isDirectory()) {
            System.err.println("Warning: Rogue commanders directory not found: " + commanderDir.getAbsolutePath());
            return decks;
        }

        File[] files = commanderDir.listFiles((dir, name) ->
                name.endsWith(".dck") && !name.endsWith("_rewards.dck"));

        if (files == null || files.length == 0) {
            System.err.println("Warning: No commander deck files found in " + commanderDir.getAbsolutePath());
            return decks;
        }

        for (File deckFile : files) {
            try {
                RogueDeckData rogueDeck = loadRogueDeckFromFile(deckFile);
                if (rogueDeck != null) {
                    decks.add(rogueDeck);
                }
            } catch (Exception e) {
                System.err.println("Error loading rogue deck from " + deckFile.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return decks;
    }

    /**
     * Load a single Rogue Deck from a file.
     * Expected files: [name].dck, [name]_rewards.dck, [name].meta
     */
    private static RogueDeckData loadRogueDeckFromFile(File deckFile) throws IOException {
        // Extract base name (e.g., "anim_pakal" from "anim_pakal.dck")
        String baseName = deckFile.getName().replace(".dck", "");

        // Load start deck
        Deck startDeck = DeckSerializer.fromFile(deckFile);
        if (startDeck == null) {
            System.err.println("Warning: Failed to load deck from " + deckFile.getName());
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
        RogueDeckData rogueDeck = new RogueDeckData();
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
            for (PaperCard card : db.getVariantCards().getAllCards()) {
                if (card.getRules().getType().isPlane()) {
                    cachedPlanarPool.add(card);
                }
            }
        }
        return cachedPlanarPool;
    }

    /**
     * Get all available Planebound configurations.
     * These represent the pool of possible plane encounters.
     */
    public static List<PlaneboundConfig> getAllPlanebounds() {
        List<PlaneboundConfig> planebounds = new ArrayList<>();

        // Ravnica Planebounds
        planebounds.add(new PlaneboundConfig(
                "Bloodhill Bastion",
                "Lyzolda, the Blood Witch",
                "rogue/planebounds/lyzolda.dck",
                100));

        planebounds.add(new PlaneboundConfig(
                "Izzet Steam Maze",
                "Niv-Mizzet, the Firemind",
                "rogue/planebounds/niv_mizzet.dck",
            58));

        planebounds.add(new PlaneboundConfig(
                "The Zephyr Maze",
                "Isperia, Supreme Judge",
                "rogue/planebounds/isperia.dck",
            47));

        planebounds.add(new PlaneboundConfig(
                "Selesnya Loft Gardens",
                "Trostani, Selesnya's Voice",
                "rogue/planebounds/trostani.dck",
            74));

        planebounds.add(new PlaneboundConfig(
                "The Dark Barony",
                "Lazav, Dimir Mastermind",
                "rogue/planebounds/lazav.dck",
            83));

        planebounds.add(new PlaneboundConfig(
                "Stronghold Furnace",
                "Rakdos, Lord Of Riots",
                "rogue/planebounds/rakdos.dck",
            62));

        // Add more planebounds here as you create them

        return planebounds;
    }

    /**
     * Generate the default linear path for a run.
     * Uses PathGenerator to create a randomized path from available planebounds.
     *
     */
    public static PathData getDefaultPath() {
        return PathGenerator.generateRandomLinearPath(5);
    }

    // Helper method to get cards from the database
    public static PaperCard getCard(String cardName, String setCode) {
        PaperCard card;
        if (setCode == null || setCode.isEmpty()) {
            card = db.getCommonCards().getCard(cardName);
        } else {
            card = db.getCommonCards().getCard(cardName, setCode);
        }
        if (card == null) {
            System.err.println("Warning: Card not found: " + cardName);
            // Return a basic land as fallback
            return db.getCommonCards().getCard("Plain");
        }
        return card;
    }
}
