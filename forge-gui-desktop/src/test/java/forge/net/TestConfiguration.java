package forge.net;

import forge.deck.Deck;
import forge.deck.io.DeckSerializer;

import java.io.File;

/**
 * Configuration class for command-line test execution via system properties.
 *
 * Supported System Properties:
 * - deck1: Path to first deck file (default: random precon)
 * - deck2: Path to second deck file (default: random precon)
 * - testMode: NETWORK_LOCAL to disable remote client (default: remote enabled)
 * - playerCount: Number of players for multiplayer (2, 3, or 4) (default: 2)
 * - iterations: Number of iterations for batch testing (default: 1)
 * - precon1: Name of quest precon for player 1 (alternative to deck1)
 * - precon2: Name of quest precon for player 2 (alternative to deck2)
 *
 * Example usage:
 * mvn test -Dtest=NetworkPlayIntegrationTest#testWithSystemProperties \
 *     -Ddeck1=/path/to/deck1.dck \
 *     -Ddeck2=/path/to/deck2.dck \
 *     -DtestMode=NETWORK_REMOTE \
 *     -Diterations=5
 */
public class TestConfiguration {

    private static final String PROP_DECK1 = "deck1";
    private static final String PROP_DECK2 = "deck2";
    private static final String PROP_PRECON1 = "precon1";
    private static final String PROP_PRECON2 = "precon2";
    private static final String PROP_TEST_MODE = "testMode";
    private static final String PROP_PLAYER_COUNT = "playerCount";
    private static final String PROP_ITERATIONS = "iterations";

    private Deck deck1;
    private Deck deck2;
    private boolean useRemoteClient;
    private int playerCount;
    private int iterations;

    /**
     * Loads configuration from system properties.
     * Falls back to defaults if properties are not set.
     */
    public TestConfiguration() {
        this.deck1 = loadDeck1();
        this.deck2 = loadDeck2();
        this.useRemoteClient = loadUseRemoteClient();
        this.playerCount = loadPlayerCount();
        this.iterations = loadIterations();
    }

    /**
     * Loads deck 1 from system properties.
     * Priority: deck1 (file path) > precon1 (precon name) > random precon
     */
    private Deck loadDeck1() {
        String deckPath = System.getProperty(PROP_DECK1);
        if (deckPath != null && !deckPath.isEmpty()) {
            File deckFile = new File(deckPath);
            if (deckFile.exists()) {
                System.out.println("[TestConfiguration] Loading deck1 from file: " + deckPath);
                return DeckSerializer.fromFile(deckFile);
            } else {
                System.err.println("[TestConfiguration] WARNING: deck1 file not found: " + deckPath);
            }
        }

        String preconName = System.getProperty(PROP_PRECON1);
        if (preconName != null && !preconName.isEmpty()) {
            System.out.println("[TestConfiguration] Loading deck1 from precon: " + preconName);
            return TestDeckLoader.loadQuestPrecon(preconName);
        }

        System.out.println("[TestConfiguration] Using random precon for deck1");
        return TestDeckLoader.getRandomPrecon();
    }

    /**
     * Loads deck 2 from system properties.
     * Priority: deck2 (file path) > precon2 (precon name) > random precon
     */
    private Deck loadDeck2() {
        String deckPath = System.getProperty(PROP_DECK2);
        if (deckPath != null && !deckPath.isEmpty()) {
            File deckFile = new File(deckPath);
            if (deckFile.exists()) {
                System.out.println("[TestConfiguration] Loading deck2 from file: " + deckPath);
                return DeckSerializer.fromFile(deckFile);
            } else {
                System.err.println("[TestConfiguration] WARNING: deck2 file not found: " + deckPath);
            }
        }

        String preconName = System.getProperty(PROP_PRECON2);
        if (preconName != null && !preconName.isEmpty()) {
            System.out.println("[TestConfiguration] Loading deck2 from precon: " + preconName);
            return TestDeckLoader.loadQuestPrecon(preconName);
        }

        System.out.println("[TestConfiguration] Using random precon for deck2");
        return TestDeckLoader.getRandomPrecon();
    }

    /**
     * Loads remote client flag from system properties.
     * Default: true (delta sync testing with real TCP client)
     * Set -DtestMode=NETWORK_LOCAL to disable remote client.
     */
    private boolean loadUseRemoteClient() {
        String mode = System.getProperty(PROP_TEST_MODE);
        if (mode != null && !mode.isEmpty()) {
            boolean remote = !"NETWORK_LOCAL".equalsIgnoreCase(mode);
            System.out.println("[TestConfiguration] Using remote client: " + remote);
            return remote;
        }

        System.out.println("[TestConfiguration] Using default: remote client enabled");
        return true;
    }

    /**
     * Loads player count from system properties.
     * Default: 2
     */
    private int loadPlayerCount() {
        String count = System.getProperty(PROP_PLAYER_COUNT);
        if (count != null && !count.isEmpty()) {
            try {
                int playerCount = Integer.parseInt(count);
                if (playerCount >= 2 && playerCount <= 4) {
                    System.out.println("[TestConfiguration] Using player count: " + playerCount);
                    return playerCount;
                } else {
                    System.err.println("[TestConfiguration] WARNING: Invalid player count: " + count +
                        ". Must be 2-4. Using default: 2");
                }
            } catch (NumberFormatException e) {
                System.err.println("[TestConfiguration] WARNING: Invalid player count format: " + count +
                    ". Using default: 2");
            }
        }

        System.out.println("[TestConfiguration] Using default player count: 2");
        return 2;
    }

    /**
     * Loads iteration count from system properties.
     * Default: 1
     */
    private int loadIterations() {
        String count = System.getProperty(PROP_ITERATIONS);
        if (count != null && !count.isEmpty()) {
            try {
                int iterations = Integer.parseInt(count);
                if (iterations > 0) {
                    System.out.println("[TestConfiguration] Using iterations: " + iterations);
                    return iterations;
                } else {
                    System.err.println("[TestConfiguration] WARNING: Invalid iterations: " + count +
                        ". Must be > 0. Using default: 1");
                }
            } catch (NumberFormatException e) {
                System.err.println("[TestConfiguration] WARNING: Invalid iterations format: " + count +
                    ". Using default: 1");
            }
        }

        System.out.println("[TestConfiguration] Using default iterations: 1");
        return 1;
    }

    // Getters

    public Deck getDeck1() {
        return deck1;
    }

    public Deck getDeck2() {
        return deck2;
    }

    public boolean isUseRemoteClient() {
        return useRemoteClient;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public int getIterations() {
        return iterations;
    }

    /**
     * Prints the current configuration to stdout.
     */
    public void printConfiguration() {
        System.out.println("\n========================================");
        System.out.println("Test Configuration:");
        System.out.println("========================================");
        System.out.println("Deck 1: " + (deck1 != null ? deck1.getName() : "null"));
        System.out.println("Deck 2: " + (deck2 != null ? deck2.getName() : "null"));
        System.out.println("Remote Client: " + useRemoteClient);
        System.out.println("Player Count: " + playerCount);
        System.out.println("Iterations: " + iterations);
        System.out.println("========================================\n");
    }

}
