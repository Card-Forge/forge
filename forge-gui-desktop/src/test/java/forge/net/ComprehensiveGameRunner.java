package forge.net;

import forge.gamemodes.net.IHasNetLog;
import forge.gamemodes.net.NetworkLogConfig;

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
public class ComprehensiveGameRunner implements IHasNetLog {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: ComprehensiveGameRunner <port> <gameIndex> <playerCount> [batchId] [batchNumber]");
            System.exit(2);
        }

        int port;
        int gameIndex;
        int playerCount;
        String batchId = null;
        int batchNumber = 0;
        try {
            port = Integer.parseInt(args[0]);
            gameIndex = Integer.parseInt(args[1]);
            playerCount = Integer.parseInt(args[2]);
            if (args.length >= 4) {
                batchId = args[3];
            }
            if (args.length >= 5) {
                batchNumber = Integer.parseInt(args[4]);
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid arguments: port, gameIndex, playerCount, and batchNumber must be integers");
            System.exit(2);
            return;
        }

        if (playerCount < 2 || playerCount > 4) {
            System.err.println("Player count must be 2, 3, or 4");
            System.exit(2);
            return;
        }

        System.exit(runGame(port, gameIndex, playerCount, batchId, batchNumber));
    }

    /**
     * Run a single game and return exit code.
     *
     * @param port Network port for the game server
     * @param gameIndex Index of this game (for identification within the batch)
     * @param playerCount Number of players (2, 3, or 4)
     * @param batchId Optional batch ID for correlating logs from the same test run
     * @param batchNumber Batch number for unique log filenames across batches
     * @return Exit code: 0=success, 1=failure, 2=error
     */
    public static int runGame(int port, int gameIndex, int playerCount, String batchId, int batchNumber) {
        try {
            // Initialize FModel FIRST - required before NetworkLogConfig is accessed
            // because the logger's static initialization chain requires GuiBase.getInterface()
            TestUtils.ensureFModelInitialized();

            // Set up logging for this game instance (must be after GuiBase initialization)
            NetworkLogConfig.setTestMode(true);
            if (batchId != null) {
                NetworkLogConfig.setBatchId(batchId);
            }
            // Include batch number in log filename to prevent overwrites across batches
            NetworkLogConfig.setInstanceSuffix("batch" + batchNumber + "-game" + gameIndex + "-" + playerCount + "p");

            netLog.info("Starting game {} with {} players on port {}",
                    gameIndex, playerCount, port);

            // Use UnifiedNetworkHarness for all player counts
            UnifiedNetworkHarness.GameResult result = runGame(port, playerCount);

            // Log result
            netLog.info("Game {} result: success={}, turns={}, winner={}",
                    gameIndex, result.success, result.turnCount, result.winner);

            // Output result to stdout for parent process to parse
            System.out.println("RESULT:" + formatResult(gameIndex, playerCount, result));

            // Close the log file
            NetworkLogConfig.closeThreadLogger();

            return result.success ? 0 : 1;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 2;
        }
    }

    /**
     * Run a game using UnifiedNetworkHarness.
     * All non-host players are remote network clients to test delta sync.
     */
    private static UnifiedNetworkHarness.GameResult runGame(int port, int playerCount) {
        return new UnifiedNetworkHarness()
                .playerCount(playerCount)
                .remoteClients(playerCount - 1)  // All but host are remote
                .port(port)
                .gameTimeout(300000)  // 5 minute timeout
                .execute();
    }

    /**
     * Format result for parent process parsing.
     * Format: gameIndex|success|playerCount|deltas|turns|bytes|winner|deck1,deck2,...
     */
    private static String formatResult(int gameIndex, int playerCount, UnifiedNetworkHarness.GameResult result) {
        String decksStr = result.deckNames.isEmpty() ? "" : String.join(",", result.deckNames);
        return String.format("%d|%s|%d|%d|%d|%d|%s|%s",
                gameIndex,
                result.success,
                playerCount,
                result.deltaPacketsReceived,
                result.turnCount,
                result.totalDeltaBytes,
                result.winner != null ? result.winner : "null",
                decksStr);
    }
}
