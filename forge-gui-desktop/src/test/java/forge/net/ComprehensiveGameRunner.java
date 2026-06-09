package forge.net;

import forge.util.IHasForgeLog;
import forge.gamemodes.net.NetworkLogConfig;

/**
 * Child JVM entry point for parallel game execution. Spawned by
 * {@link MultiProcessGameExecutor}; delegates to {@link UnifiedNetworkHarness}
 * to run a single game and prints a {@code RESULT:} line for the parent to parse.
 *
 * <pre>
 * Usage: java -cp classpath forge.net.ComprehensiveGameRunner port gameIndex playerCount [batchId] [batchNumber] [commander]
 * Exit codes: 0=success, 1=failure, 2=error
 * Output:     RESULT:gameIndex|success|playerCount|deltas|turns|bytes|winner|decks|format|eventMismatches
 * </pre>
 */
public class ComprehensiveGameRunner implements IHasForgeLog {

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
        boolean commander = false;
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
            if (args.length >= 6) {
                commander = Boolean.parseBoolean(args[5]);
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

        System.exit(runGame(port, gameIndex, playerCount, batchId, batchNumber, commander));
    }

    /**
     * Run a single game and return exit code.
     *
     * @param port Network port for the game server
     * @param gameIndex Index of this game (for identification within the batch)
     * @param playerCount Number of players (2, 3, or 4)
     * @param batchId Optional batch ID for correlating logs from the same test run
     * @param batchNumber Batch number for unique log filenames across batches
     * @param commander Whether to use Commander format
     * @return Exit code: 0=success, 1=failure, 2=error
     */
    public static int runGame(int port, int gameIndex, int playerCount, String batchId, int batchNumber, boolean commander) {
        try {
            // Initialize FModel FIRST - required before NetworkLogConfig is accessed
            // because the logger's static initialization chain requires GuiBase.getInterface()
            TestUtils.ensureFModelInitialized();

            NetworkLogConfig.setTestMode(true);
            if (batchId != null) {
                NetworkLogConfig.setBatchId(batchId);
            }
            String formatSuffix = commander ? "-cmdr" : "";
            // Include batch number in log filename to prevent overwrites across batches
            NetworkLogConfig.setInstanceSuffix("batch" + batchNumber + "-game" + gameIndex + "-" + playerCount + "p" + formatSuffix);

            String formatLabel = commander ? "Commander" : "Constructed";
            netLog.info("Starting game {} with {} players on port {} ({})",
                    gameIndex, playerCount, port, formatLabel);

            UnifiedNetworkHarness.GameResult result = new UnifiedNetworkHarness()
                    .playerCount(playerCount)
                    .remoteClients(playerCount - 1)  // All but host are remote
                    .commander(commander)
                    .port(port)
                    .gameTimeout(300000)  // 5 minute timeout
                    .execute();

            netLog.info("Game {} result: success={}, turns={}, winner={}, format={}",
                    gameIndex, result.success, result.turnCount, result.winner, result.gameFormat);

            System.out.println("RESULT:" + formatResult(gameIndex, playerCount, result));
            NetworkLogConfig.closeThreadLogger();

            return result.success ? 0 : 1;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 2;
        }
    }

    private static String formatResult(int gameIndex, int playerCount, UnifiedNetworkHarness.GameResult result) {
        String decksStr = result.deckNames.isEmpty() ? "" : String.join(",", result.deckNames);
        return String.format("%d|%s|%d|%d|%d|%d|%s|%s|%s|%d",
                gameIndex,
                result.success,
                playerCount,
                result.deltaPacketsReceived,
                result.turnCount,
                result.totalDeltaBytes,
                result.winner != null ? result.winner : "null",
                decksStr,
                result.gameFormat,
                result.eventStateMismatches);
    }
}
