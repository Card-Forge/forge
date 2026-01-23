package forge.net;

import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Main entry point for running automated network tests from the console.
 * Designed for use from IntelliJ terminal or CI/CD pipelines.
 *
 * Uses HeadlessGuiDesktop to run without a display server.
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="forge.net.ConsoleNetworkTestRunner" \
 *       -Dexec.classpathScope=test -Dexec.args="--games 5"
 *
 * Phase 1 of the Automated Network Testing Plan.
 */
public class ConsoleNetworkTestRunner {

    private static boolean initialized = false;

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("Forge Automated Network Test Runner");
        System.out.println("=".repeat(60));

        // Parse command-line arguments
        int gameCount = parseIntArg(args, "--games", 1);
        boolean verbose = hasFlag(args, "--verbose");

        System.out.println("Configuration:");
        System.out.println("  Games to run: " + gameCount);
        System.out.println("  Verbose mode: " + verbose);
        System.out.println();

        // Initialize FModel with HeadlessGuiDesktop (no display server required)
        try {
            initializeFModel();
        } catch (Exception e) {
            System.err.println("Failed to initialize FModel: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // Verify precon decks are available
        if (!TestDeckLoader.hasPrecons()) {
            System.err.println("No precon decks found. Cannot run tests.");
            System.err.println("Check that res/quest/precons/ contains .dck files.");
            System.exit(1);
        }
        System.out.println("Found " + TestDeckLoader.getPreconCount() + " precon decks available");
        System.out.println();

        // Run tests
        List<GameTestMetrics> results = new ArrayList<>();
        int passed = 0;
        int failed = 0;

        for (int i = 1; i <= gameCount; i++) {
            System.out.println("-".repeat(40));
            System.out.println("Running game " + i + " of " + gameCount);
            System.out.println("-".repeat(40));

            try {
                AutomatedGameTestHarness harness = new AutomatedGameTestHarness();
                GameTestMetrics metrics = harness.runBasicTwoPlayerGame();
                results.add(metrics);

                if (metrics.isGameCompleted()) {
                    passed++;
                    System.out.println("Game " + i + ": PASSED");
                } else {
                    failed++;
                    System.out.println("Game " + i + ": FAILED - " + metrics.getErrorMessage());
                }
            } catch (Exception e) {
                failed++;
                System.err.println("Game " + i + ": EXCEPTION - " + e.getMessage());
                if (verbose) {
                    e.printStackTrace();
                }
            }

            System.out.println();
        }

        // Print summary
        System.out.println("=".repeat(60));
        System.out.println("Test Summary");
        System.out.println("=".repeat(60));
        System.out.println("Total games: " + gameCount);
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);

        if (!results.isEmpty()) {
            // Calculate aggregate network metrics
            long totalBytes = 0;
            long totalDelta = 0;
            long totalFullState = 0;
            int gamesWithMetrics = 0;

            for (GameTestMetrics m : results) {
                if (m.getTotalBytesSent() > 0) {
                    totalBytes += m.getTotalBytesSent();
                    totalDelta += m.getDeltaBytesSent();
                    totalFullState += m.getFullStateBytesSent();
                    gamesWithMetrics++;
                }
            }

            if (gamesWithMetrics > 0) {
                System.out.println();
                System.out.println("Network Metrics (across " + gamesWithMetrics + " games):");
                System.out.println("  Total bytes: " + totalBytes);
                System.out.println("  Delta bytes: " + totalDelta);
                System.out.println("  Full state bytes: " + totalFullState);
                System.out.println("  Avg bytes/game: " + (totalBytes / gamesWithMetrics));
            }
        }

        System.out.println("=".repeat(60));

        // Exit with appropriate status code
        System.exit(failed > 0 ? 1 : 0);
    }

    /**
     * Initialize FModel using HeadlessGuiDesktop for testing without display server.
     */
    private static void initializeFModel() {
        if (initialized) {
            return;
        }

        System.out.println("Initializing FModel with HeadlessGuiDesktop...");

        // Set headless GUI interface (allows running without display server)
        GuiBase.setInterface(new HeadlessGuiDesktop());

        // Initialize FModel with preferences
        // Disable lazy loading for test stability
        FModel.initialize(null, preferences -> {
            preferences.setPref(FPref.LOAD_CARD_SCRIPTS_LAZILY, false);
            preferences.setPref(FPref.UI_LANGUAGE, "en-US");
            return null;
        });

        initialized = true;
        System.out.println("FModel initialized successfully (headless mode)");
    }

    /**
     * Parse an integer argument from command-line args.
     */
    private static int parseIntArg(String[] args, String name, int defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (name.equals(args[i])) {
                try {
                    return Integer.parseInt(args[i + 1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid value for " + name + ": " + args[i + 1]);
                }
            }
        }
        return defaultValue;
    }

    /**
     * Check if a flag is present in command-line args.
     */
    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Print usage information.
     */
    private static void printUsage() {
        System.out.println("Usage: ConsoleNetworkTestRunner [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --games N     Number of games to run (default: 1)");
        System.out.println("  --verbose     Print detailed error messages");
        System.out.println("  --help        Print this help message");
        System.out.println();
        System.out.println("Example:");
        System.out.println("  mvn exec:java -Dexec.mainClass=\"forge.net.ConsoleNetworkTestRunner\" \\");
        System.out.println("      -Dexec.classpathScope=test -Dexec.args=\"--games 5 --verbose\"");
        System.out.println();
        System.out.println("Note: Uses HeadlessGuiDesktop, no display server required.");
    }
}
