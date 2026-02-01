package forge.net.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts context around errors in network debug log files.
 * This helps Claude debug test failures by showing the game state and log lines around errors.
 */
public class LogContextExtractor {

    // Number of lines to capture before and after error
    private static final int LINES_BEFORE = 20;
    private static final int LINES_AFTER = 5;

    // Patterns for extracting game state
    private static final Pattern TURN_PATTERN = Pattern.compile(
            "\\[GameEvent\\] Turn (\\d+) began");

    private static final Pattern PHASE_PATTERN = Pattern.compile(
            "Phase:\\s*(.+?)(?:\\s*$|\\s*,)");

    private static final Pattern DELTA_PHASE_PATTERN = Pattern.compile(
            "\\[DeltaSync\\]\\s+Phase:\\s*(\\w+)");

    private static final Pattern ERROR_PATTERN = Pattern.compile(
            "\\[ERROR\\]|CHECKSUM MISMATCH", Pattern.CASE_INSENSITIVE);

    private static final Pattern WARN_PATTERN = Pattern.compile(
            "\\[WARN\\]", Pattern.CASE_INSENSITIVE);

    // Pattern for player state from checksum detail lines
    // Example: [ERROR] [DeltaSync]   Player 0 (Alice (Host AI)): Life=20, Hand=7, GY=2, BF=0
    private static final Pattern PLAYER_STATE_PATTERN = Pattern.compile(
            "Player\\s+(\\d+)\\s+\\(([^)]+)\\):\\s*Life=(\\d+),\\s*Hand=(\\d+),\\s*GY=(\\d+),\\s*BF=(\\d+)");

    /**
     * Immutable record representing a player's state at error time.
     */
    public record PlayerState(
        int playerId,
        String playerName,
        int life,
        int handSize,
        int graveyardSize,
        int battlefieldSize
    ) {
        @Override
        public String toString() {
            return String.format("Player %d (%s): Life=%d, Hand=%d, GY=%d, BF=%d",
                    playerId, playerName, life, handSize, graveyardSize, battlefieldSize);
        }
    }

    /**
     * Immutable record containing all context around an error.
     */
    public record ErrorContext(
        String logFileName,
        int errorLineNumber,
        int turnAtError,
        String phaseAtError,
        List<PlayerState> playerStates,
        List<String> linesBefore,      // Lines before error
        List<String> linesAfter,       // Lines after error
        List<String> warningsBefore,   // Warnings that preceded error
        String errorMessage
    ) {
        /**
         * Generate a markdown-formatted representation of this error context.
         */
        public String toMarkdown() {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("**%s** (error at line %d):\n", logFileName, errorLineNumber));
            sb.append(String.format("- Turn: %d, Phase: %s\n", turnAtError, phaseAtError != null ? phaseAtError : "unknown"));

            // Player states table
            if (!playerStates.isEmpty()) {
                sb.append("- Player states:\n");
                sb.append("  | Player | Life | Hand | GY | Battlefield |\n");
                sb.append("  |--------|------|------|-----|-------------|\n");
                for (PlayerState ps : playerStates) {
                    sb.append(String.format("  | %s | %d | %d | %d | %d |\n",
                            ps.playerName(), ps.life(), ps.handSize(),
                            ps.graveyardSize(), ps.battlefieldSize()));
                }
            }

            // Warnings before error
            if (!warningsBefore.isEmpty()) {
                sb.append(String.format("\n- Warnings before error (%d):\n", warningsBefore.size()));
                int shown = 0;
                for (String warning : warningsBefore) {
                    if (shown++ >= 5) {
                        sb.append(String.format("  - ... and %d more warnings\n", warningsBefore.size() - 5));
                        break;
                    }
                    sb.append(String.format("  - `%s`\n", truncate(warning, 100)));
                }
            }

            // Lines around error
            sb.append("\n- Lines around error:\n");
            sb.append("  ```\n");
            for (String line : linesBefore) {
                sb.append(String.format("  %s\n", truncate(line, 120)));
            }
            sb.append(String.format("  >>> %s <<<\n", truncate(errorMessage, 120)));
            for (String line : linesAfter) {
                sb.append(String.format("  %s\n", truncate(line, 120)));
            }
            sb.append("  ```\n");

            return sb.toString();
        }

        private static String truncate(String s, int maxLen) {
            if (s == null) return "";
            if (s.length() <= maxLen) return s;
            return s.substring(0, maxLen - 3) + "...";
        }
    }

    /**
     * Extract context around the first error in a log file.
     *
     * @param logFile The log file to analyze
     * @return ErrorContext with details, or null if no errors found
     */
    public ErrorContext extractAroundFirstError(File logFile) {
        if (logFile == null || !logFile.exists()) {
            return null;
        }

        List<String> allLines = new ArrayList<>();
        int errorLineIndex = -1;
        String errorMessage = null;
        int currentTurn = 0;
        String currentPhase = null;
        List<String> warningsBefore = new ArrayList<>();
        List<PlayerState> playerStates = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                allLines.add(line);

                // Track turn changes
                Matcher turnMatcher = TURN_PATTERN.matcher(line);
                if (turnMatcher.find()) {
                    try {
                        currentTurn = Integer.parseInt(turnMatcher.group(1));
                    } catch (NumberFormatException e) {
                        // Skip malformed turn line
                    }
                }

                // Track phase changes (try delta sync pattern first, then general)
                Matcher deltaPhase = DELTA_PHASE_PATTERN.matcher(line);
                if (deltaPhase.find()) {
                    currentPhase = deltaPhase.group(1);
                } else {
                    Matcher phaseMatcher = PHASE_PATTERN.matcher(line);
                    if (phaseMatcher.find()) {
                        currentPhase = phaseMatcher.group(1).trim();
                    }
                }

                // Track warnings before first error
                if (errorLineIndex < 0 && WARN_PATTERN.matcher(line).find()) {
                    warningsBefore.add(line);
                    // Keep only recent warnings
                    if (warningsBefore.size() > 50) {
                        warningsBefore.remove(0);
                    }
                }

                // Look for first error
                if (errorLineIndex < 0 && ERROR_PATTERN.matcher(line).find()) {
                    errorLineIndex = lineNumber - 1; // 0-indexed
                    errorMessage = line;
                }
            }
        } catch (IOException e) {
            return null;
        }

        // No error found
        if (errorLineIndex < 0) {
            return null;
        }

        // Extract lines around error
        int startIndex = Math.max(0, errorLineIndex - LINES_BEFORE);
        int endIndex = Math.min(allLines.size() - 1, errorLineIndex + LINES_AFTER);

        List<String> linesBefore = new ArrayList<>();
        List<String> linesAfter = new ArrayList<>();

        for (int i = startIndex; i < errorLineIndex; i++) {
            linesBefore.add(allLines.get(i));
        }
        for (int i = errorLineIndex + 1; i <= endIndex; i++) {
            linesAfter.add(allLines.get(i));
        }

        // Extract player states from lines around error (typically in error detail lines)
        playerStates = extractPlayerStates(allLines, errorLineIndex);

        return new ErrorContext(
                logFile.getName(),
                errorLineIndex + 1, // 1-indexed line number
                currentTurn,
                currentPhase,
                playerStates,
                linesBefore,
                linesAfter,
                new ArrayList<>(warningsBefore), // Copy to ensure immutability
                errorMessage
        );
    }

    /**
     * Extract player states from lines around an error.
     * Player states are typically logged as part of checksum error details.
     *
     * @param lines All lines from the log file
     * @param errorLineIndex Index of the error line
     * @return List of PlayerState records
     */
    public List<PlayerState> extractPlayerStates(List<String> lines, int errorLineIndex) {
        List<PlayerState> states = new ArrayList<>();

        // Search in a window around the error (player states usually follow error line)
        int searchStart = Math.max(0, errorLineIndex - 5);
        int searchEnd = Math.min(lines.size(), errorLineIndex + 20);

        for (int i = searchStart; i < searchEnd; i++) {
            String line = lines.get(i);
            Matcher matcher = PLAYER_STATE_PATTERN.matcher(line);
            if (matcher.find()) {
                try {
                    states.add(new PlayerState(
                            Integer.parseInt(matcher.group(1)),
                            matcher.group(2).trim(),
                            Integer.parseInt(matcher.group(3)),
                            Integer.parseInt(matcher.group(4)),
                            Integer.parseInt(matcher.group(5)),
                            Integer.parseInt(matcher.group(6))
                    ));
                } catch (NumberFormatException e) {
                    // Skip malformed player state line
                }
            }
        }

        return states;
    }

    /**
     * Extract player states from a list of strings.
     *
     * @param lines Lines to search for player state information
     * @return List of PlayerState records found
     */
    public List<PlayerState> extractPlayerStates(List<String> lines) {
        return extractPlayerStates(lines, lines.size() / 2);
    }
}
