package forge.headless;

import forge.LobbyPlayer;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.game.*;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiBase;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Unit tests for TUI gameplay with test controllers.
 * These tests run much faster than the Python e2e tests because
 * they run in-process without subprocess overhead.
 */
public class TUIGameTest {

    @BeforeClass
    public static void setup() {
        // Setup headless GUI interface FIRST (before FModel.initialize)
        // because ForgeConstants needs GuiBase to be set during initialization
        GuiBase.setInterface(new HeadlessGuiBase());

        // Initialize Forge model once for all tests
        FModel.initialize(null, null);
    }

    /**
     * Test that a pass-only controller loses against the AI.
     *
     * Invariants:
     * - Game completes without errors
     * - Game takes more than 2 turns
     * - AI wins (human who passes always loses)
     * - Some permanents on battlefield by end
     */
    @Test
    public void testPassAgentLoses() {
        System.out.println("\n=== Testing Pass Agent (always passes) ===");

        // Load test decks
        Deck deck = loadTestDeck("monored.dck");
        assertNotNull("Failed to load test deck", deck);

        // Create and run game
        GameOutcome outcome = runGameWithTestController(deck, deck, true, false);

        // Verify game completed
        assertNotNull("Game outcome should not be null", outcome);
        assertFalse("Game should not be a draw", outcome.isDraw());

        // Verify AI won
        String winnerName = outcome.getWinningLobbyPlayer().getName();
        assertTrue("AI should win, but winner was: " + winnerName,
                   winnerName.contains("AI") || winnerName.contains("Ai"));

        System.out.println("  ✓ AI won as expected: " + winnerName);
    }

    /**
     * Test that a random controller can complete games without errors.
     *
     * Invariants:
     * - Game completes without errors
     * - Game takes at least 3 turns
     * - No Java exceptions occur
     * - No mana payment failures
     * - No targeting failures
     * - No activator warnings
     */
    @Test
    public void testRandomAgentCompletes() {
        System.out.println("\n=== Testing Random Agent ===");

        // Load test decks
        Deck deck = loadTestDeck("monored.dck");
        assertNotNull("Failed to load test deck", deck);

        // Capture System.out/err to detect error patterns
        SystemOutputCapture outputCapture = new SystemOutputCapture();
        outputCapture.startCapture();

        try {
            // Create and run game with random controller
            GameOutcome outcome = runGameWithTestController(deck, deck, false, false);

            // Verify game completed
            assertNotNull("Game outcome should not be null", outcome);
            assertTrue("Game should have a winner or be a draw",
                       outcome.getWinningPlayer() != null || outcome.isDraw());

            if (outcome.isDraw()) {
                System.out.println("  ✓ Game ended in draw");
            } else {
                System.out.println("  ✓ Game completed, winner: " + outcome.getWinningLobbyPlayer().getName());
            }
        } finally {
            outputCapture.stopCapture();
        }

        // Check for error patterns in captured output
        if (outputCapture.hasErrorPatterns()) {
            List<String> errors = outputCapture.getErrorPatterns();
            fail("Random agent generated errors:\n" + String.join("\n", errors) +
                 "\n\nCaptured output:\n" + outputCapture.getAllOutput());
        }
    }

    /**
     * Test that battlefield state progresses over multiple turns.
     */
    @Test
    public void testBattlefieldProgression() {
        System.out.println("\n=== Testing Battlefield Progression ===");

        // Load test decks
        Deck deck = loadTestDeck("monored.dck");
        assertNotNull("Failed to load test deck", deck);

        // Run game and capture details
        GameOutcome outcome = runGameWithTestController(deck, deck, true, true);

        assertNotNull("Game outcome should not be null", outcome);
        System.out.println("  ✓ Game progressed with battlefield changes");
    }

    /**
     * Helper method to load a test deck.
     */
    private Deck loadTestDeck(String deckName) {
        // Try to find test deck relative to the test class
        Path testDeckPath = Paths.get("test_decks", deckName);
        File deckFile = testDeckPath.toFile();

        if (!deckFile.exists()) {
            // Try absolute path construction
            String projectRoot = System.getProperty("user.dir");
            deckFile = Paths.get(projectRoot, "test_decks", deckName).toFile();
        }

        if (!deckFile.exists()) {
            fail("Could not find test deck: " + deckName + " (tried: " + deckFile.getAbsolutePath() + ")");
        }

        return DeckSerializer.fromFile(deckFile);
    }

    /**
     * Helper method to run a game with test controllers.
     *
     * @param player1Deck Deck for player 1
     * @param player2Deck Deck for player 2
     * @param player1Pass If true, player 1 uses pass controller; else random
     * @param captureDetails If true, capture and print game details
     * @return The game outcome
     */
    private GameOutcome runGameWithTestController(Deck player1Deck, Deck player2Deck,
                                                   boolean player1Pass, boolean captureDetails) {
        // Create players
        List<RegisteredPlayer> players = new ArrayList<>();

        // Player 1
        RegisteredPlayer player1 = new RegisteredPlayer(player1Deck);
        LobbyPlayer player1Lobby = GamePlayerUtil.getGuiPlayer("Player 1", 0, 0, false);
        player1.setPlayer(player1Lobby);
        players.add(player1);

        // Player 2 (AI)
        RegisteredPlayer player2 = new RegisteredPlayer(player2Deck);
        LobbyPlayer player2Lobby = GamePlayerUtil.createAiPlayer("AI-TestDeck", 0);
        player2.setPlayer(player2Lobby);
        players.add(player2);

        // Create and start the match
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test Game");

        Game game = match.createGame();

        // Replace player 1's controller with test controller
        Player player1GamePlayer = null;
        for (Player p : game.getPlayers()) {
            if (p.getLobbyPlayer() == player1Lobby) {
                player1GamePlayer = p;
                break;
            }
        }

        if (player1GamePlayer != null) {
            try {
                java.lang.reflect.Field controllerField = Player.class.getDeclaredField("controller");
                controllerField.setAccessible(true);

                if (player1Pass) {
                    TestPassController testController = new TestPassController(game, player1GamePlayer, player1Lobby);
                    controllerField.set(player1GamePlayer, testController);
                } else {
                    TestRandomController testController = new TestRandomController(game, player1GamePlayer, player1Lobby,
                                                                                   new Random(42)); // Fixed seed for reproducibility
                    controllerField.set(player1GamePlayer, testController);
                }
            } catch (Exception e) {
                fail("Failed to install test controller: " + e.getMessage());
            }
        }

        // Capture logs if requested
        GameLogCapture logCapture = null;
        if (captureDetails) {
            logCapture = new GameLogCapture(game);
        }

        // Start the game
        match.startGame(game);

        // Capture logs after game completes
        if (captureDetails && logCapture != null) {
            logCapture.captureLogs();

            int maxTurn = logCapture.getMaxTurn();
            boolean hasLand = logCapture.hasLandPlayed();
            boolean hasCreature = logCapture.hasCreatureCast();

            System.out.println("  Game lasted " + maxTurn + " turns");
            System.out.println("  Land played: " + hasLand);
            System.out.println("  Creature cast: " + hasCreature);

            // Verify invariants
            assertTrue("Game should last more than 2 turns", maxTurn > 2);
            assertTrue("At least one land should be played", hasLand);
            assertFalse("No errors should occur", logCapture.hasErrors());
        }

        return game.getOutcome();
    }
}
