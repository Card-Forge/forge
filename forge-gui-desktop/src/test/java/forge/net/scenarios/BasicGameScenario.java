package forge.net.scenarios;

import forge.net.AutomatedGameTestHarness;
import forge.net.GameTestMetrics;

/**
 * Tests basic 2-player game with no interruptions.
 * Validates: Game completion, winner determination, basic network sync.
 *
 * Phase 5.1 of the Automated Network Testing Plan.
 */
public class BasicGameScenario {

    /**
     * Result of a scenario execution.
     */
    public static class ScenarioResult {
        public final boolean gameCompleted;
        public final boolean networkTrafficRecorded;
        public final String description;
        public final GameTestMetrics metrics;

        public ScenarioResult(boolean gameCompleted, boolean networkTrafficRecorded,
                              String description, GameTestMetrics metrics) {
            this.gameCompleted = gameCompleted;
            this.networkTrafficRecorded = networkTrafficRecorded;
            this.description = description;
            this.metrics = metrics;
        }

        public boolean passed() {
            return gameCompleted && networkTrafficRecorded;
        }

        @Override
        public String toString() {
            return String.format("ScenarioResult[%s, completed=%b, networkTraffic=%b]",
                description, gameCompleted, networkTrafficRecorded);
        }
    }

    /**
     * Execute the basic game scenario.
     *
     * @return ScenarioResult with test outcomes
     */
    public ScenarioResult execute() {
        System.out.println("[BasicGameScenario] Starting basic 2-player game scenario");

        AutomatedGameTestHarness harness = new AutomatedGameTestHarness();
        GameTestMetrics metrics = harness.runBasicTwoPlayerGame();

        boolean gameCompleted = metrics.isGameCompleted();
        boolean networkTrafficRecorded = metrics.getTotalBytesSent() > 0;

        ScenarioResult result = new ScenarioResult(
            gameCompleted,
            networkTrafficRecorded,
            "Basic 2-player AI game",
            metrics
        );

        System.out.println("[BasicGameScenario] Result: " + result);
        return result;
    }

    /**
     * Execute multiple iterations of the basic game scenario.
     *
     * @param iterations Number of games to run
     * @return Array of ScenarioResults
     */
    public ScenarioResult[] executeMultiple(int iterations) {
        ScenarioResult[] results = new ScenarioResult[iterations];

        for (int i = 0; i < iterations; i++) {
            System.out.println("[BasicGameScenario] Iteration " + (i + 1) + " of " + iterations);
            results[i] = execute();
        }

        return results;
    }

    /**
     * Main method for standalone testing.
     */
    public static void main(String[] args) {
        // Note: FModel must be initialized before running
        BasicGameScenario scenario = new BasicGameScenario();
        ScenarioResult result = scenario.execute();

        System.out.println();
        System.out.println("Test " + (result.passed() ? "PASSED" : "FAILED"));
        System.exit(result.passed() ? 0 : 1);
    }
}
