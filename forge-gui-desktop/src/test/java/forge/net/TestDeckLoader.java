package forge.net;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.localinstance.properties.ForgeConstants;

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
     * @return Random precon deck with valid cards
     */
    public static Deck getRandomPrecon() {
        List<String> precons = listAvailablePrecons();
        if (precons.isEmpty()) {
            throw new IllegalStateException("No quest precon decks found in " +
                ForgeConstants.QUEST_PRECON_DIR);
        }
        String randomName = precons.get(random.nextInt(precons.size()));
        System.out.println("[TestDeckLoader] Selected random precon: " + randomName);
        return loadQuestPrecon(randomName);
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
}
