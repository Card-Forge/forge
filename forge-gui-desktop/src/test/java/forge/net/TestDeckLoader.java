package forge.net;

import forge.card.CardDb;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Thin wrapper around existing DeckSerializer for test deck loading.
 * Uses quest precons which are known-good balanced decks.
 *
 * Phase 0.5 of the Automated Network Testing Plan.
 */
public class TestDeckLoader {

    private static final Random random = new Random();

    /** Minimum cards required for a valid constructed deck */
    private static final int MIN_DECK_SIZE = 60;

    /** Maximum retry attempts when retryOnInvalidDeck is enabled */
    private static final int MAX_RETRIES = 10;

    /**
     * When true, getRandomPrecon() will automatically retry if a deck
     * fails validation (e.g., has fewer than 60 cards).
     * Default is true for general testing robustness.
     * Set to false when specifically testing deck validation.
     */
    private static boolean retryOnInvalidDeck = true;

    /**
     * Load a quest precon deck by name.
     * @param name Deck name without .dck extension
     * @return Deck with valid cards
     */
    public static Deck loadQuestPrecon(String name) {
        File deckFile = new File(ForgeConstants.QUEST_PRECON_DIR, name + ".dck");
        if (!deckFile.exists()) {
            throw new IllegalArgumentException("Precon deck not found: " + name +
                " at " + deckFile.getAbsolutePath());
        }
        Deck deck = DeckSerializer.fromFile(deckFile);
        if (deck == null) {
            throw new IllegalStateException("Failed to parse deck file: " + deckFile.getAbsolutePath());
        }
        return deck;
    }

    /**
     * List all available quest precon deck names.
     * @return List of deck names (without .dck extension)
     */
    public static List<String> listAvailablePrecons() {
        File preconDir = new File(ForgeConstants.QUEST_PRECON_DIR);
        if (!preconDir.exists() || !preconDir.isDirectory()) {
            System.err.println("[TestDeckLoader] Precon directory not found: " + preconDir.getAbsolutePath());
            return Collections.emptyList();
        }

        File[] files = preconDir.listFiles((d, n) -> n.endsWith(".dck"));
        if (files == null || files.length == 0) {
            System.err.println("[TestDeckLoader] No .dck files found in: " + preconDir.getAbsolutePath());
            return Collections.emptyList();
        }

        return Arrays.stream(files)
            .map(f -> f.getName().replace(".dck", ""))
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * Get a random quest precon deck.
     *
     * If retryOnInvalidDeck is true (default), this method will automatically
     * try different decks if the selected one fails validation.
     *
     * @return Random precon deck with valid cards
     */
    public static Deck getRandomPrecon() {
        List<String> precons = listAvailablePrecons();
        if (precons.isEmpty()) {
            throw new IllegalStateException("No quest precon decks found in " +
                ForgeConstants.QUEST_PRECON_DIR);
        }

        // Shuffle to avoid retrying the same decks
        List<String> shuffled = new java.util.ArrayList<>(precons);
        Collections.shuffle(shuffled, random);

        int attempts = Math.min(MAX_RETRIES, shuffled.size());

        for (int i = 0; i < attempts; i++) {
            String deckName = shuffled.get(i);
            Deck deck = loadQuestPrecon(deckName);

            if (!retryOnInvalidDeck || isValidDeck(deck)) {
                System.out.println("[TestDeckLoader] Selected random precon: " + deckName);
                return deck;
            }

            int cardCount = deck.getMain() != null ? deck.getMain().countAll() : 0;
            System.out.println("[TestDeckLoader] Deck '" + deckName +
                "' has " + cardCount + " cards (need " + MIN_DECK_SIZE +
                "), trying another...");
        }

        // If we exhausted retries, return the last deck anyway with a warning
        String lastName = shuffled.get(0);
        System.err.println("[TestDeckLoader] WARNING: Could not find valid deck after " +
            attempts + " attempts, returning '" + lastName + "' anyway");
        return loadQuestPrecon(lastName);
    }

    /**
     * Check if a deck meets minimum validation requirements.
     *
     * @param deck Deck to validate
     * @return true if deck has at least MIN_DECK_SIZE cards
     */
    public static boolean isValidDeck(Deck deck) {
        if (deck == null || deck.getMain() == null) {
            return false;
        }
        return deck.getMain().countAll() >= MIN_DECK_SIZE;
    }

    /**
     * Get the number of available precon decks.
     * @return Count of available precons
     */
    public static int getPreconCount() {
        return listAvailablePrecons().size();
    }

    /**
     * Check if precon decks are available.
     * @return true if at least one precon deck exists
     */
    public static boolean hasPrecons() {
        return !listAvailablePrecons().isEmpty();
    }

    /**
     * Create a minimal deck with basic lands only.
     * Used for fast CI testing where game completion matters more than strategic play.
     * Games with these decks end quickly as players can only play lands and eventually deck out.
     *
     * Note: Deck legality checking must be disabled (ENFORCE_DECK_LEGALITY=false) to use
     * decks smaller than 60 cards.
     *
     * @param landName Basic land name: "Plains", "Island", "Swamp", "Mountain", or "Forest"
     * @param count Number of copies (10 for fast tests, 60 for legal minimum)
     * @return Deck with only basic lands
     */
    public static Deck createMinimalDeck(String landName, int count) {
        CardDb cardDb = FModel.getMagicDb().getCommonCards();
        PaperCard land = cardDb.getCard(landName);
        if (land == null) {
            throw new IllegalStateException("Basic land not found: " + landName);
        }

        Deck deck = new Deck(landName + " x" + count);
        for (int i = 0; i < count; i++) {
            deck.getMain().add(land);
        }
        return deck;
    }
}
