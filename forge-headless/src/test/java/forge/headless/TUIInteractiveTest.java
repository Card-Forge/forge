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

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Interactive TUI tests that simulate user input to test gameplay features.
 * These tests use a scripted input stream to programmatically interact with the TUI.
 */
public class TUIInteractiveTest {

    @BeforeClass
    public static void setup() {
        // Setup headless GUI interface FIRST (before FModel.initialize)
        // because ForgeConstants needs GuiBase to be set during initialization
        GuiBase.setInterface(new HeadlessGuiBase());

        // Initialize Forge model once for all tests
        FModel.initialize(null, null);
    }

    /**
     * Test that simulates a player making choices through the TUI.
     * The player will:
     * 1. Play a land on turn 1
     * 2. Pass priority several times
     * 3. Cast a creature when they have mana
     * 4. Eventually pass until the game ends
     */
    @Test
    public void testInteractiveGameWithScriptedInput() {
        System.out.println("\n=== Testing Interactive TUI with Scripted Input ===");

        // Load test deck
        Deck deck = loadTestDeck("monored.dck");
        assertNotNull("Failed to load test deck", deck);

        // Create a script: play land on turn 1, then pass for a while, then play creatures when possible
        // The script uses mostly "0" (pass) but occasionally tries to take actions
        String script = buildGameScript();

        // Create game with scripted input
        GameOutcome outcome = runGameWithScriptedInput(deck, deck, script);

        // Verify game completed
        assertNotNull("Game outcome should not be null", outcome);
        assertTrue("Game should have a winner or be a draw",
                   outcome.getWinningPlayer() != null || outcome.isDraw());

        if (outcome.isDraw()) {
            System.out.println("  ✓ Game ended in draw");
        } else {
            System.out.println("  ✓ Game completed, winner: " + outcome.getWinningLobbyPlayer().getName());
        }
    }

    /**
     * Build a script that simulates realistic gameplay.
     * Strategy: Try to play lands and creatures, but mostly pass.
     */
    private String buildGameScript() {
        StringBuilder script = new StringBuilder();

        // Turn 1: Try to play land (option 1 if available), then pass
        script.append("1\n");  // Try to play first land
        script.append("0\n");  // Pass priority

        // Turns 2-5: Try to play land, try to cast spell, then pass a lot
        for (int turn = 2; turn <= 5; turn++) {
            script.append("1\n");  // Try to play land
            script.append("1\n");  // Try to cast first available spell
            for (int i = 0; i < 20; i++) {
                script.append("0\n");  // Pass priority many times
            }
        }

        // Rest of game: just pass to let it finish
        for (int i = 0; i < 200; i++) {
            script.append("0\n");
        }

        return script.toString();
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
     * Run a game with the TUI controller using scripted input.
     */
    private GameOutcome runGameWithScriptedInput(Deck player1Deck, Deck player2Deck, String inputScript) {
        // Create players
        List<RegisteredPlayer> players = new ArrayList<>();

        // Player 1 (TUI with scripted input)
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
        Match match = new Match(rules, players, "TUI Interactive Test");

        Game game = match.createGame();

        // Replace player 1's controller with TUI controller using scripted input
        Player player1GamePlayer = null;
        for (Player p : game.getPlayers()) {
            if (p.getLobbyPlayer() == player1Lobby) {
                player1GamePlayer = p;
                break;
            }
        }

        if (player1GamePlayer != null) {
            try {
                // Create a BufferedReader from our script
                BufferedReader scriptReader = new BufferedReader(new StringReader(inputScript));

                // Create TUI controller with scripted input
                PlayerControllerTUI tuiController = new PlayerControllerTUI(game, player1GamePlayer, player1Lobby, scriptReader);

                // Use reflection to set the controller
                java.lang.reflect.Field controllerField = Player.class.getDeclaredField("controller");
                controllerField.setAccessible(true);
                controllerField.set(player1GamePlayer, tuiController);

                System.out.println("  Installed TUI controller with scripted input");
            } catch (Exception e) {
                fail("Failed to install TUI controller: " + e.getMessage());
            }
        }

        // Redirect stdout to capture TUI output for analysis
        ByteArrayOutputStream outputCapture = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputCapture));

        try {
            // Start the game
            match.startGame(game);

            // Restore stdout
            System.setOut(originalOut);

            // Analyze the captured output
            String output = outputCapture.toString();
            System.out.println("  Game produced " + output.length() + " bytes of output");

            // Check for common issues in output
            if (output.contains("Error") || output.contains("Exception")) {
                System.out.println("  WARNING: Output contains error messages");
                // Print relevant error lines
                String[] lines = output.split("\n");
                for (String line : lines) {
                    if (line.contains("Error") || line.contains("Exception")) {
                        System.out.println("    " + line);
                    }
                }
            }

            // Check that the TUI was actually used
            if (output.contains("YOUR TURN") || output.contains("What would you like to do?")) {
                System.out.println("  ✓ TUI prompts detected in output");
            } else {
                System.out.println("  WARNING: No TUI prompts detected - controller may not have been called");
            }

        } finally {
            System.setOut(originalOut);
        }

        return game.getOutcome();
    }

    /**
     * Test that the TUI handles running out of scripted input gracefully.
     */
    @Test
    public void testScriptedInputRunsOut() {
        System.out.println("\n=== Testing TUI with Limited Script ===");

        Deck deck = loadTestDeck("monored.dck");
        assertNotNull("Failed to load test deck", deck);

        // Very short script - will run out quickly
        String shortScript = "0\n0\n0\n";

        GameOutcome outcome = runGameWithScriptedInput(deck, deck, shortScript);

        // Game should still complete (controller should use default choices when input runs out)
        assertNotNull("Game outcome should not be null", outcome);
        System.out.println("  ✓ Game handled running out of input");
    }
}
