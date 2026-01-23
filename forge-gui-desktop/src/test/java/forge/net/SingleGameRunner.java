package forge.net;

import forge.gamemodes.net.NetworkDebugLogger;
import forge.gui.GuiBase;
import forge.model.FModel;

/**
 * Standalone runner for a single network game.
 * Designed to be invoked as a separate JVM process for parallel execution.
 *
 * Usage: java -cp <classpath> forge.net.SingleGameRunner <port> <gameIndex>
 *
 * Exit codes:
 *   0 = Success (game completed with winner and delta packets)
 *   1 = Failure (game failed or no delta packets)
 *   2 = Error (exception during execution)
 */
public class SingleGameRunner {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: SingleGameRunner <port> <gameIndex>");
            System.exit(2);
        }

        int port;
        int gameIndex;
        try {
            port = Integer.parseInt(args[0]);
            gameIndex = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid arguments: port and gameIndex must be integers");
            System.exit(2);
            return;
        }

        System.exit(runGame(port, gameIndex));
    }

    /**
     * Run a single game and return exit code.
     */
    public static int runGame(int port, int gameIndex) {
        try {
            // Set up logging for this game instance
            NetworkDebugLogger.setTestMode(true);
            NetworkDebugLogger.setInstanceSuffix("game" + gameIndex);

            // Initialize FModel
            if (GuiBase.getInterface() == null) {
                GuiBase.setInterface(new HeadlessGuiDesktop());
                FModel.initialize(null, preferences -> null);
            }

            NetworkDebugLogger.log("[SingleGameRunner] Starting game %d on port %d", gameIndex, port);

            // Run the game
            NetworkClientTestHarness harness = new NetworkClientTestHarness();
            harness.setPort(port);
            NetworkClientTestHarness.TestResult result = harness.runTwoPlayerNetworkTest();

            // Log result
            NetworkDebugLogger.log("[SingleGameRunner] Game %d result: %s", gameIndex, result.toSummary());

            // Output result to stdout for parent process to parse
            System.out.println("RESULT:" + formatResult(gameIndex, result));

            // Close the log file
            NetworkDebugLogger.closeThreadLogger();

            return result.success ? 0 : 1;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return 2;
        }
    }

    private static String formatResult(int gameIndex, NetworkClientTestHarness.TestResult result) {
        return String.format("%d|%s|%d|%d|%d|%s",
                gameIndex,
                result.success,
                result.deltaPacketsReceived,
                result.turns,
                result.totalDeltaBytes,
                result.winner != null ? result.winner : "null");
    }
}
