package forge.net.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import forge.gamemodes.net.NetworkDebugLogger;

/**
 * Parses network debug log files to extract delta sync metrics and errors.
 *
 * Uses single-pass parsing with pre-compiled regex patterns for efficiency.
 * Supports parallel file analysis via parallelStream().
 *
 * Also provides error context extraction for debugging test failures
 * (formerly in separate LogContextExtractor class).
 */
public class NetworkLogAnalyzer {

    // Number of lines to capture before and after error for context
    private static final int CONTEXT_LINES_BEFORE = 20;
    private static final int CONTEXT_LINES_AFTER = 5;

    // Pre-compiled regex patterns for efficiency
    private static final Pattern DELTA_PACKET_PATTERN = Pattern.compile(
            "\\[DeltaSync\\] Packet #(\\d+): Approximate=(\\d+) bytes, ActualNetwork=(\\d+) bytes, FullState=(\\d+) bytes");

    private static final Pattern SAVINGS_PATTERN = Pattern.compile(
            "Savings: Approximate=(\\d+)%, Actual=(\\d+)%");

    private static final Pattern GAME_OUTCOME_PATTERN = Pattern.compile(
            "\\[GAME EVENT\\] Game outcome: winner = (.+)");

    private static final Pattern TURN_PATTERN = Pattern.compile(
            "\\[GAME EVENT\\] Turn (\\d+) began");

    private static final Pattern GAME_COMPLETED_PATTERN = Pattern.compile(
            "Game completed|game finished|Game COMPLETED|isGameOver.*true");

    private static final Pattern CHECKSUM_MISMATCH_PATTERN = Pattern.compile(
            "CHECKSUM MISMATCH|checksum mismatch|desync", Pattern.CASE_INSENSITIVE);

    private static final Pattern ERROR_PATTERN = Pattern.compile(
            "\\[ERROR\\]|ERROR:|Exception|exception", Pattern.CASE_INSENSITIVE);

    private static final Pattern TIMEOUT_PATTERN = Pattern.compile(
            "timeout|timed out|time limit exceeded", Pattern.CASE_INSENSITIVE);

    private static final Pattern WARN_PATTERN = Pattern.compile(
            "\\[WARN\\]|WARN:|WARNING:", Pattern.CASE_INSENSITIVE);

    // Patterns for error context extraction
    private static final Pattern PHASE_PATTERN = Pattern.compile(
            "Phase:\\s*(.+?)(?:\\s*$|\\s*,)");

    private static final Pattern DELTA_PHASE_PATTERN = Pattern.compile(
            "\\[DeltaSync\\]\\s+Phase:\\s*(\\w+)");

    private static final Pattern PLAYER_STATE_PATTERN = Pattern.compile(
            "Player\\s+(\\d+)\\s+\\(([^)]+)\\):\\s*Life=(\\d+),\\s*Hand=(\\d+),\\s*GY=(\\d+),\\s*BF=(\\d+)");

    private static final Pattern PLAYER_COUNT_PATTERN = Pattern.compile(
            "Players:\\s*(\\d+)|player count.*(\\d+)|(\\d+)-player", Pattern.CASE_INSENSITIVE);

    private static final Pattern GAME_INDEX_PATTERN = Pattern.compile(
            "game(\\d+)");

    // Pattern for "deck=[name], ready=" format (deck names may contain commas)
    private static final Pattern DECK_EQUALS_PATTERN = Pattern.compile(
            "deck=(.+?),\\s*ready=", Pattern.CASE_INSENSITIVE);

    // Pattern for "deck pre-loaded: [name])" format - capture until final ) at end of line
    // Handles deck names with parentheses like "Core Set 2019 Welcome Deck (BG)"
    private static final Pattern DECK_PRELOADED_PATTERN = Pattern.compile(
            "deck\\s+pre-loaded:\\s*(.+)\\)\\s*$", Pattern.CASE_INSENSITIVE);

    // Matches other deck name formats:
    // - "deck:" or "deck loaded:"
    // - "with deck:" or "Sending deck:"
    // - "Client deck loaded:"
    private static final Pattern DECK_NAME_PATTERN = Pattern.compile(
            "(?:deck(?:\\s+loaded)?:|with deck:|Sending deck:|Client deck loaded:)\\s*(.+?)(?:\\s*\\(|$)", Pattern.CASE_INSENSITIVE);

    // ==================== Error Context Records ====================

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
        List<String> linesBefore,
        List<String> linesAfter,
        List<String> warningsBefore,
        String errorMessage
    ) {
        /**
         * Generate a markdown-formatted representation of this error context.
         */
        public String toMarkdown() {
            StringBuilder sb = new StringBuilder();

            sb.append(String.format("**%s** (error at line %d):\n", logFileName, errorLineNumber));
            sb.append(String.format("- Turn: %d, Phase: %s\n", turnAtError, phaseAtError != null ? phaseAtError : "unknown"));

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

            if (!warningsBefore.isEmpty()) {
                sb.append(String.format("\n- Warnings before error (%d):\n", warningsBefore.size()));
                int shown = 0;
                for (String warning : warningsBefore) {
                    if (shown++ >= 5) {
                        sb.append(String.format("  - ... and %d more warnings\n", warningsBefore.size() - 5));
                        break;
                    }
                    sb.append(String.format("  - `%s`\n", truncateForContext(warning, 100)));
                }
            }

            sb.append("\n- Lines around error:\n");
            sb.append("  ```\n");
            for (String line : linesBefore) {
                sb.append(String.format("  %s\n", truncateForContext(line, 120)));
            }
            sb.append(String.format("  >>> %s <<<\n", truncateForContext(errorMessage, 120)));
            for (String line : linesAfter) {
                sb.append(String.format("  %s\n", truncateForContext(line, 120)));
            }
            sb.append("  ```\n");

            return sb.toString();
        }

        private static String truncateForContext(String s, int maxLen) {
            if (s == null) return "";
            if (s.length() <= maxLen) return s;
            return s.substring(0, maxLen - 3) + "...";
        }
    }

    // ==================== Log Analysis Methods ====================

    /**
     * Analyze a single log file.
     *
     * @param logFile Path to the log file
     * @return GameLogMetrics with extracted data
     */
    public GameLogMetrics analyzeLogFile(File logFile) {
        GameLogMetrics metrics = new GameLogMetrics();
        metrics.setLogFileName(logFile.getName());

        // Try to extract game index from filename (e.g., "game5" -> 5)
        Matcher indexMatcher = GAME_INDEX_PATTERN.matcher(logFile.getName());
        if (indexMatcher.find()) {
            try {
                metrics.setGameIndex(Integer.parseInt(indexMatcher.group(1)));
            } catch (NumberFormatException e) {
                // Invalid game index in filename, leave as default
            }
        }

        // Track packet-level metrics
        List<Double> approximateSavingsList = new ArrayList<>();
        List<Double> actualSavingsList = new ArrayList<>();
        int maxTurn = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            int packetCount = 0;
            long totalApproximateBytes = 0;
            long totalDeltaBytes = 0;
            long totalFullStateBytes = 0;

            while ((line = reader.readLine()) != null) {
                // Delta packet metrics
                Matcher packetMatcher = DELTA_PACKET_PATTERN.matcher(line);
                if (packetMatcher.find()) {
                    try {
                        packetCount++;
                        int packetNum = Integer.parseInt(packetMatcher.group(1));
                        long approxBytes = Long.parseLong(packetMatcher.group(2));
                        long actualBytes = Long.parseLong(packetMatcher.group(3));
                        long fullStateBytes = Long.parseLong(packetMatcher.group(4));

                        totalApproximateBytes += approxBytes;
                        totalDeltaBytes += actualBytes;
                        totalFullStateBytes += fullStateBytes;

                        // Look for savings line (usually follows immediately)
                        String nextLine = reader.readLine();
                        if (nextLine != null) {
                            Matcher savingsMatcher = SAVINGS_PATTERN.matcher(nextLine);
                            if (savingsMatcher.find()) {
                                double approxSavings = Double.parseDouble(savingsMatcher.group(1));
                                double actualSavings = Double.parseDouble(savingsMatcher.group(2));
                                approximateSavingsList.add(approxSavings);
                                actualSavingsList.add(actualSavings);
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Skip malformed packet line
                    }
                    continue;
                }

                // Game outcome
                Matcher outcomeMatcher = GAME_OUTCOME_PATTERN.matcher(line);
                if (outcomeMatcher.find()) {
                    metrics.setWinner(outcomeMatcher.group(1).trim());
                    metrics.setGameCompleted(true);
                    continue;
                }

                // Turn tracking
                Matcher turnMatcher = TURN_PATTERN.matcher(line);
                if (turnMatcher.find()) {
                    try {
                        int turn = Integer.parseInt(turnMatcher.group(1));
                        if (turn > maxTurn) {
                            maxTurn = turn;
                        }
                    } catch (NumberFormatException e) {
                        // Skip malformed turn line
                    }
                    continue;
                }

                // Game completed (alternative patterns)
                if (GAME_COMPLETED_PATTERN.matcher(line).find()) {
                    metrics.setGameCompleted(true);
                    continue;
                }

                // Checksum mismatch
                if (CHECKSUM_MISMATCH_PATTERN.matcher(line).find()) {
                    metrics.setHasChecksumMismatch(true);
                    continue;
                }

                // Player count
                Matcher playerMatcher = PLAYER_COUNT_PATTERN.matcher(line);
                if (playerMatcher.find()) {
                    for (int i = 1; i <= playerMatcher.groupCount(); i++) {
                        String match = playerMatcher.group(i);
                        if (match != null) {
                            try {
                                int count = Integer.parseInt(match);
                                if (count >= 2 && count <= 4) {
                                    metrics.setPlayerCount(count);
                                    break;
                                }
                            } catch (NumberFormatException e) {
                                // Skip malformed player count
                            }
                        }
                    }
                    continue;
                }

                // Deck names - try multiple patterns to handle different log formats
                // First try "deck=[name], ready=" format (handles deck names with commas)
                Matcher deckEqualsMatcher = DECK_EQUALS_PATTERN.matcher(line);
                if (deckEqualsMatcher.find()) {
                    String deckName = deckEqualsMatcher.group(1).trim();
                    metrics.addDeckName(deckName);
                    continue;
                }

                // Try "deck pre-loaded: [name])" format
                Matcher deckPreloadedMatcher = DECK_PRELOADED_PATTERN.matcher(line);
                if (deckPreloadedMatcher.find()) {
                    String deckName = deckPreloadedMatcher.group(1).trim();
                    metrics.addDeckName(deckName);
                    continue;
                }

                // Try other deck formats
                Matcher deckMatcher = DECK_NAME_PATTERN.matcher(line);
                if (deckMatcher.find()) {
                    String deckName = deckMatcher.group(1).trim();
                    metrics.addDeckName(deckName);
                    continue;
                }

                // Errors - track first error turn for failure analysis
                if (ERROR_PATTERN.matcher(line).find()) {
                    metrics.addError(truncateLine(line));
                    // Record the turn when first error occurred
                    if (metrics.getFirstErrorTurn() < 0 && maxTurn > 0) {
                        metrics.setFirstErrorTurn(maxTurn);
                    }
                    continue;
                }

                // Warnings
                if (WARN_PATTERN.matcher(line).find()) {
                    metrics.addWarning(truncateLine(line));
                }
            }

            // Set aggregated metrics
            metrics.setDeltaPacketCount(packetCount);
            metrics.setTotalApproximateBytes(totalApproximateBytes);
            metrics.setTotalDeltaBytes(totalDeltaBytes);
            metrics.setTotalFullStateBytes(totalFullStateBytes);
            metrics.setTurnCount(maxTurn);

            // Determine failure mode based on log content
            metrics.setFailureMode(determineFailureMode(metrics));

            // Extract error context if there are errors
            if (!metrics.getErrors().isEmpty() || metrics.hasChecksumMismatch()) {
                ErrorContext context = extractAroundFirstError(logFile);
                metrics.setErrorContext(context);
            }

        } catch (IOException e) {
            metrics.addError("Failed to read log file: " + e.getMessage());
            metrics.setFailureMode(GameLogMetrics.FailureMode.EXCEPTION);
        }

        return metrics;
    }

    /**
     * Determine the failure mode based on metrics collected during parsing.
     * Priority: CHECKSUM_MISMATCH > TIMEOUT > EXCEPTION > INCOMPLETE > NONE
     */
    private GameLogMetrics.FailureMode determineFailureMode(GameLogMetrics metrics) {
        // Check for checksum mismatch (desync) first - most critical
        if (metrics.hasChecksumMismatch()) {
            return GameLogMetrics.FailureMode.CHECKSUM_MISMATCH;
        }

        // Check for timeout in error messages
        for (String error : metrics.getErrors()) {
            if (TIMEOUT_PATTERN.matcher(error).find()) {
                return GameLogMetrics.FailureMode.TIMEOUT;
            }
        }

        // Check for any other exceptions/errors
        if (!metrics.getErrors().isEmpty()) {
            return GameLogMetrics.FailureMode.EXCEPTION;
        }

        // Check if game didn't complete for unknown reason
        if (!metrics.isGameCompleted()) {
            return GameLogMetrics.FailureMode.INCOMPLETE;
        }

        return GameLogMetrics.FailureMode.NONE;
    }

    /**
     * Analyze log files matching a pattern in a directory.
     *
     * @param logDirectory Directory containing log files
     * @param pattern Regex pattern for log files
     * @return List of GameLogMetrics for each file
     */
    public List<GameLogMetrics> analyzeDirectory(File logDirectory, String pattern) {
        if (!logDirectory.exists() || !logDirectory.isDirectory()) {
            System.err.println("[NetworkLogAnalyzer] Directory not found: " + NetworkDebugLogger.sanitizePath(logDirectory.getAbsolutePath()));
            return new ArrayList<>();
        }

        try {
            // Find all matching log files
            Path dirPath = logDirectory.toPath();
            Pattern regexPattern = Pattern.compile(pattern);

            List<File> logFiles = Files.walk(dirPath, 1)
                    .filter(Files::isRegularFile)
                    .filter(p -> regexPattern.matcher(p.getFileName().toString()).matches())
                    .map(Path::toFile)
                    .collect(Collectors.toList());

            System.out.println("[NetworkLogAnalyzer] Found " + logFiles.size() + " log files matching pattern: " + pattern);

            // Analyze files in parallel for efficiency
            return logFiles.parallelStream()
                    .map(this::analyzeLogFile)
                    .collect(Collectors.toList());

        } catch (IOException e) {
            System.err.println("[NetworkLogAnalyzer] Error scanning log directory: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Analyze comprehensive test logs (those with "-gameN-Np" suffix).
     *
     * @param logDirectory Directory containing log files
     * @return List of GameLogMetrics for comprehensive test logs
     */
    public List<GameLogMetrics> analyzeComprehensiveTestLogs(File logDirectory) {
        // Match patterns like:
        // - Old: network-debug-20260124-105844-21236-game0-4p-test.log
        // - New: network-debug-run20260127-213221-game0-4p-test.log
        // - Batched: network-debug-run20260128-064643-batch0-game0-4p-test.log
        return analyzeDirectory(logDirectory, "network-debug-.*-(?:batch\\d+-)?game\\d+-\\d+p-test\\.log");
    }

    /**
     * Analyze comprehensive test logs created after a specific time.
     * This allows analyzing only logs from the current test run without clearing old logs.
     *
     * @param logDirectory Directory containing log files
     * @param afterTime Only include logs created after this time
     * @return List of GameLogMetrics for matching logs
     */
    public List<GameLogMetrics> analyzeComprehensiveTestLogsSince(File logDirectory, LocalDateTime afterTime) {
        List<GameLogMetrics> allLogs = analyzeComprehensiveTestLogs(logDirectory);
        return filterLogsByTime(allLogs, logDirectory, afterTime);
    }

    /**
     * Filter logs by timestamp extracted from filename.
     * Log filenames are formatted as: network-debug-YYYYMMDD-HHMMSS-PID-test.log
     * Note: Filename timestamps have second precision, so we truncate afterTime
     * to seconds to avoid excluding logs from the same second as the test start.
     */
    private List<GameLogMetrics> filterLogsByTime(List<GameLogMetrics> metrics, File logDirectory, LocalDateTime afterTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        // Truncate to seconds since filename timestamps don't have sub-second precision
        LocalDateTime afterTimeSeconds = afterTime.truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

        return metrics.stream()
                .filter(m -> {
                    LocalDateTime logTime = extractTimestampFromFilename(m.getLogFileName(), formatter);
                    // Use !isBefore to include logs from the same second
                    return logTime != null && !logTime.isBefore(afterTimeSeconds);
                })
                .collect(Collectors.toList());
    }

    /**
     * Extract timestamp from log filename.
     * Supports two formats:
     * - Old: network-debug-YYYYMMDD-HHMMSS-PID-*.log
     * - New: network-debug-runYYYYMMDD-HHMMSS-*.log
     */
    private LocalDateTime extractTimestampFromFilename(String filename, DateTimeFormatter formatter) {
        // Pattern handles both old format (network-debug-20260124-105844-...)
        // and new format (network-debug-run20260127-213221-...)
        Pattern timestampPattern = Pattern.compile("network-debug-(?:run)?(\\d{8}-\\d{6})-");
        Matcher matcher = timestampPattern.matcher(filename);
        if (matcher.find()) {
            try {
                return LocalDateTime.parse(matcher.group(1), formatter);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Truncate a log line to prevent memory bloat.
     */
    private String truncateLine(String line) {
        if (line.length() > 200) {
            return line.substring(0, 197) + "...";
        }
        return line;
    }

    /**
     * Build an AnalysisResult from a list of GameLogMetrics.
     *
     * @param metrics List of per-game metrics
     * @return Aggregated analysis result
     */
    public AnalysisResult buildAnalysisResult(List<GameLogMetrics> metrics) {
        return new AnalysisResult(metrics);
    }

    /**
     * Analyze comprehensive test logs created after a specific time and return an aggregated result.
     * This allows analyzing only logs from the current test run without clearing old logs.
     *
     * @param logDirectory Directory containing log files
     * @param afterTime Only include logs created after this time
     * @return Aggregated analysis result
     */
    public AnalysisResult analyzeComprehensiveTestAndAggregate(File logDirectory, LocalDateTime afterTime) {
        List<GameLogMetrics> metrics = analyzeComprehensiveTestLogsSince(logDirectory, afterTime);
        System.out.println("[NetworkLogAnalyzer] Filtered to " + metrics.size() + " logs created after " + afterTime);
        return buildAnalysisResult(metrics);
    }

    // ==================== Error Context Extraction Methods ====================

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
        int startIndex = Math.max(0, errorLineIndex - CONTEXT_LINES_BEFORE);
        int endIndex = Math.min(allLines.size() - 1, errorLineIndex + CONTEXT_LINES_AFTER);

        List<String> linesBefore = new ArrayList<>();
        List<String> linesAfter = new ArrayList<>();

        for (int i = startIndex; i < errorLineIndex; i++) {
            linesBefore.add(allLines.get(i));
        }
        for (int i = errorLineIndex + 1; i <= endIndex; i++) {
            linesAfter.add(allLines.get(i));
        }

        // Extract player states from lines around error
        List<PlayerState> playerStates = extractPlayerStates(allLines, errorLineIndex);

        return new ErrorContext(
                logFile.getName(),
                errorLineIndex + 1, // 1-indexed line number
                currentTurn,
                currentPhase,
                playerStates,
                linesBefore,
                linesAfter,
                new ArrayList<>(warningsBefore),
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
}
