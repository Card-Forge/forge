package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;
import forge.gui.GuiBase;
import forge.model.FModel;
import forge.net.scenarios.MultiplayerNetworkScenario;

/**
 * Standalone runner for comprehensive network game testing.
 * Supports 2-4 player games with configurable options.
 *
 * Designed to be invoked as a separate JVM process for parallel execution.
 *
 * Usage: java -cp <classpath> forge.net.ComprehensiveGameRunner <port> <gameIndex> <playerCount> [batchId]
 *
 * Arguments:
 *   port        - Network port for the game server
 *   gameIndex   - Index of this game (for identification in logs)
 *   playerCount - Number of players (2, 3, or 4)
 *   batchId     - Optional batch ID for correlating logs from the same test run
 *
 * Exit codes:
 *   0 = Success (game completed with winner and delta packets)
 *   1 = Failure (game failed or no delta packets)
 *   2 = Error (exception during execution)
 *
 * Output format (for parent process parsing):
 *   RESULT:gameIndex|success|playerCount|deltas|turns|bytes|winner|deck1,deck2,...
 */
public class ComprehensiveGameRunner {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: ComprehensiveGameRunner <port> <gameIndex> <playerCount> [batchId]");
            System.exit(2);
        }

        int port;
        int gameIndex;
        int playerCount;
        String batchId = null;
        try {
            port = Integer.parseInt(args[0]);
            gameIndex = Integer.parseInt(args[1]);
            playerCount = Integer.parseInt(args[2]);
            if (args.length >= 4) {
                batchId = args[3];
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid arguments: port, gameIndex, and playerCount must be integers");
            System.exit(2);
            return;
        }

        if (playerCount < 2 || playerCount > 4) {
            System.err.println("Player count must be 2, 3, or 4");
            System.exit(2);
            return;
        }

        System.exit(runGame(port, gameIndex, playerCount, batchId));
    }

    /**
     * Run a single game and return exit code.
     *
     * @param port Network port for the game server
     * @param gameIndex Index of this game (for identification)
     * @param playerCount Number of players (2, 3, or 4)
     * @return Exit code: 0=success, 1=failure, 2=error
     */
    public static int runGame(int port, int gameIndex, int playerCount) {
        return runGame(port, gameIndex, playerCount, null);
    }

    /**
     * Run a single game and return exit code.
     *
     * @param port Network port for the game server
     * @param gameIndex Index of this game (for identification)
     * @param playerCount Number of players (2, 3, or 4)
     * @param batchId Optional batch ID for correlating logs from the same test run
     * @return Exit code: 0=success, 1=failure, 2=error
     */
    public static int runGame(int port, int gameIndex, int playerCount, String batchId) {
        try {
            // Set up logging for this game instance
            NetworkDebugLogger.setTestMode(true);
            if (batchId != null) {
                NetworkDebugLogger.setBatchId(batchId);
            }
            NetworkDebugLogger.setInstanceSuffix("game" + gameIndex + "-" + playerCount + "p");

            // Initialize FModel
            if (GuiBase.getInterface() == null) {
                GuiBase.setInterface(new HeadlessGuiDesktop());
                FModel.initialize(null, preferences -> null);
            }

            NetworkDebugLogger.log("[ComprehensiveGameRunner] Starting game %d with %d players on port %d",
                    gameIndex, playerCount, port);

            GameRunResult result;

            if (playerCount == 2) {
                // Use NetworkClientTestHarness for 2-player games
                result = runTwoPlayerGame(port, gameIndex);
            } else {
                // Use MultiplayerScenario for 3-4 player games
                result = runMultiplayerGame(port, gameIndex, playerCount);
            }

            // Log result
            NetworkDebugLogger.log("[ComprehensiveGameRunner] Game %d result: success=%s, turns=%d, winner=%s",
                    gameIndex, result.success, result.turns, result.winner);

            // Output result to stdout for parent process to parse
            System.out.println("RESULT:" + formatResult(gameIndex, playerCount, result));

            // Close the log file
            NetworkDebugLogger.closeThreadLogger();

            return result.success ? 0 : 1;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 2;
        }
    }

    /**
     * Run a 2-player network game using NetworkClientTestHarness.
     */
    private static GameRunResult runTwoPlayerGame(int port, int gameIndex) {
        NetworkClientTestHarness harness = new NetworkClientTestHarness();
        harness.setPort(port);
        NetworkClientTestHarness.TestResult testResult = harness.runTwoPlayerNetworkTest();

        return new GameRunResult(
                testResult.success,
                testResult.turns,
                testResult.deltaPacketsReceived,
                testResult.totalDeltaBytes,
                testResult.winner,
                testResult.deckNames
        );
    }

    /**
     * Run a 3-4 player multiplayer game using MultiplayerNetworkScenario.
     * All non-host players are remote network clients to test delta sync.
     */
    private static GameRunResult runMultiplayerGame(int port, int gameIndex, int playerCount) {
        MultiplayerNetworkScenario scenario = new MultiplayerNetworkScenario()
                .playerCount(playerCount)
                .port(port) // Use the port assigned by the parent process
                .gameTimeout(300000); // 5 minute timeout in ms

        MultiplayerNetworkScenario.ScenarioResult scenarioResult = scenario.execute();

        // Multiplayer games now have actual remote clients receiving delta packets
        return new GameRunResult(
                scenarioResult.passed(),
                scenarioResult.turnCount,
                scenarioResult.totalDeltaPackets,
                scenarioResult.totalDeltaBytes,
                scenarioResult.winner,
                scenarioResult.deckNames
        );
    }

    /**
     * Format result for parent process parsing.
     * Format: gameIndex|success|playerCount|deltas|turns|bytes|winner|deck1,deck2,...
     */
    private static String formatResult(int gameIndex, int playerCount, GameRunResult result) {
        String decksStr = result.deckNames.isEmpty() ? "" : String.join(",", result.deckNames);
        return String.format("%d|%s|%d|%d|%d|%d|%s|%s",
                gameIndex,
                result.success,
                playerCount,
                result.deltaPackets,
                result.turns,
                result.bytes,
                result.winner != null ? result.winner : "null",
                decksStr);
    }

    /**
     * Internal result holder for game runs.
     */
    private static class GameRunResult {
        final boolean success;
        final int turns;
        final long deltaPackets;
        final long bytes;
        final String winner;
        final java.util.List<String> deckNames;

        GameRunResult(boolean success, int turns, long deltaPackets, long bytes, String winner) {
            this(success, turns, deltaPackets, bytes, winner, java.util.Collections.emptyList());
        }

        GameRunResult(boolean success, int turns, long deltaPackets, long bytes, String winner, java.util.List<String> deckNames) {
            this.success = success;
            this.turns = turns;
            this.deltaPackets = deltaPackets;
            this.bytes = bytes;
            this.winner = winner;
            this.deckNames = deckNames != null ? deckNames : java.util.Collections.emptyList();
        }
    }
}
