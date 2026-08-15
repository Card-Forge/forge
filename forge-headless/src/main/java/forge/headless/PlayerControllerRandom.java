package forge.headless;

import forge.LobbyPlayer;
import forge.game.Game;
import forge.game.player.Player;
import forge.util.MyRandom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Agent that makes random valid choices.
 * Useful for fuzz testing and exploring different game paths.
 */
public class PlayerControllerRandom extends PlayerControllerTUI {

    public PlayerControllerRandom(Game game, Player p, LobbyPlayer lp, boolean askMana, boolean numericChoices) {
        super(game, p, lp, askMana, numericChoices, new RandomChoiceReader());
    }

    @Override
    public boolean isAI() {
        return true; // Act like an AI for game logic purposes
    }

    /**
     * A BufferedReader that generates random numeric choices when readLine() is called.
     * Package-private so PlayerControllerTUI can detect and configure it.
     */
    static class RandomChoiceReader extends BufferedReader {
        private int currentMin = 0;
        private int currentMax = 0;
        private int consecutiveCalls = 0; // Track repeated calls with same range

        public RandomChoiceReader() {
            super(new EmptyReader());
        }

        /**
         * Set the valid range for the next choice.
         * This should be called before readLine() to ensure valid random choices.
         */
        public void setRange(int min, int max) {
            // Only reset counter if range actually changed
            if (this.currentMin != min || this.currentMax != max) {
                this.consecutiveCalls = 0;
            }
            this.currentMin = min;
            this.currentMax = max;
        }

        @Override
        public String readLine() throws IOException {
            consecutiveCalls++;

            // After a few calls with the same range, increase probability of ending loop
            // This handles attacker/blocker selection loops
            int endLoopChance = Math.min(consecutiveCalls * 10, 50); // Increases to 50% max
            if (consecutiveCalls >= 2 && MyRandom.getRandom().nextInt(100) < endLoopChance) {
                return ""; // Empty string to end loops
            }

            // Generate a random choice within the valid range
            if (currentMax < currentMin) {
                // Invalid range, default to 0
                return "0";
            }

            // Generate a random number within the valid range [min, max]
            // Bias toward choosing minimum value (30% chance) as it often means "pass" or "skip"
            int range = currentMax - currentMin + 1;
            int randomValue = MyRandom.getRandom().nextInt(100);

            if (randomValue < 30) {
                // 30% chance to choose minimum
                return String.valueOf(currentMin);
            } else {
                // 70% chance to choose any value in the range
                int choice = currentMin + MyRandom.getRandom().nextInt(range);
                return String.valueOf(choice);
            }
        }

        /**
         * Dummy Reader that does nothing
         */
        private static class EmptyReader extends Reader {
            @Override
            public int read(char[] cbuf, int off, int len) throws IOException {
                return -1; // EOF
            }

            @Override
            public void close() throws IOException {
                // No-op
            }
        }
    }
}
