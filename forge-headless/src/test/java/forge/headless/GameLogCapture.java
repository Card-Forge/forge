package forge.headless;

import forge.game.Game;
import forge.game.GameLogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for capturing and analyzing game logs during tests.
 * Provides methods to check game invariants.
 */
public class GameLogCapture {

    private final Game game;
    private final List<String> capturedLogs;

    public GameLogCapture(Game game) {
        this.game = game;
        this.capturedLogs = new ArrayList<>();
    }

    /**
     * Capture all log entries from the game.
     */
    public void captureLogs() {
        List<GameLogEntry> entries = game.getGameLog().getLogEntries(null);
        capturedLogs.clear();
        // Logs are in reverse order (newest first), so reverse them
        for (int i = entries.size() - 1; i >= 0; i--) {
            capturedLogs.add(entries.get(i).toString());
        }
    }

    /**
     * Get all captured log entries.
     */
    public List<String> getCapturedLogs() {
        return new ArrayList<>(capturedLogs);
    }

    /**
     * Get the maximum turn number reached in the game.
     */
    public int getMaxTurn() {
        Pattern turnPattern = Pattern.compile("Turn (\\d+)");
        int maxTurn = 0;
        for (String log : capturedLogs) {
            Matcher m = turnPattern.matcher(log);
            if (m.find()) {
                int turn = Integer.parseInt(m.group(1));
                if (turn > maxTurn) {
                    maxTurn = turn;
                }
            }
        }
        return maxTurn;
    }

    /**
     * Check if a land was played during the game.
     */
    public boolean hasLandPlayed() {
        for (String log : capturedLogs) {
            if (log.contains("played Mountain") ||
                log.contains("played Plains") ||
                log.contains("played Forest") ||
                log.contains("played Island") ||
                log.contains("played Swamp")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if any creatures were cast during the game.
     */
    public boolean hasCreatureCast() {
        for (String log : capturedLogs) {
            if (log.contains("cast") && log.contains("Creature")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if any spell was cast during the game.
     */
    public boolean hasSpellCast() {
        for (String log : capturedLogs) {
            if (log.contains("cast")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if there are any error messages in the logs.
     * Also captures System.out/err messages by checking if specific error patterns occurred.
     */
    public boolean hasErrors() {
        for (String log : capturedLogs) {
            if (log.contains("error") || log.contains("Error") ||
                log.contains("Exception") || log.contains("failed")) {
                return true;
            }
            // Check for specific error patterns that indicate problems
            if (log.contains("cost was not paid for")) {
                return true;
            }
            if (log.contains("Couldn't add to stack, failed to target")) {
                return true;
            }
            if (log.contains("Did not have activator set")) {
                return true;
            }
            if (log.contains("AI failed to play")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all logs as a single string.
     */
    public String getLogsAsString() {
        return String.join("\n", capturedLogs);
    }
}
