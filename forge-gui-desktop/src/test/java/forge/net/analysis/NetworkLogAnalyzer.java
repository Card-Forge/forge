package forge.net.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import forge.gamemodes.net.NetworkLogConfig;

/**
 * Parses game log files to extract metrics, errors, and game health data.
 *
 * Supports both structured network debug logs (with [DeltaSync] and [GAME EVENT]
 * prefixes) and generic forge.log / game output formats. Structured patterns are
 * tried first; generic fallbacks catch games logged without network debug prefixes.
 *
 * Uses single-pass parsing with pre-compiled regex patterns for efficiency.
 * Supports parallel file analysis via parallelStream().
 *
 * Also provides error context extraction for debugging failures
 * (formerly in separate LogContextExtractor class).
 */
public class NetworkLogAnalyzer {

    // Number of lines to capture before and after error for context
    private static final int CONTEXT_LINES_BEFORE = 20;
    private static final int CONTEXT_LINES_AFTER = 5;

    // ==================== Structured (delta sync) patterns ====================

    private static final Pattern DELTA_PACKET_PATTERN = Pattern.compile(
            "\\[DeltaSync\\] Packet #(\\d+): Delta=(\\d+) bytes, FullState=(\\d+) bytes, Savings=(\\d+)%");

    private static final Pattern GAME_EVENT_OUTCOME_PATTERN = Pattern.compile(
            "\\[GAME EVENT\\] Game outcome: winner = (.+)");

    private static final Pattern GAME_EVENT_TURN_PATTERN = Pattern.compile(
            "\\[GAME EVENT\\] Turn (\\d+) began");

    // ==================== Generic (forge.log / game output) patterns ====================

    // Fallback turn detection: "Turn N" anywhere in line
    private static final Pattern GENERIC_TURN_PATTERN = Pattern.compile(
            "Turn (\\d+)");

    // Fallback winner detection: forge.log format
    private static final Pattern GENERIC_WINNER_PATTERN = Pattern.compile(
            "(.+) has won the game|winner=(.+?)(?:,|$|\\s)", Pattern.CASE_INSENSITIVE);

    // Game completion from structured output lines
    private static final Pattern RESULT_LINE_PATTERN = Pattern.compile(
            "RESULT:(SUCCESS|FAILURE):(\\d+):(\\d+):(\\d+)");

    // ==================== Network performance patterns ====================

    // "Encoded 95527 bytes (compressed) for GuiGameEvent"
    private static final Pattern ENCODED_BYTES_PATTERN = Pattern.compile(
            "Encoded (\\d+) bytes \\(compressed\\)");

    // "send() blocked 214 ms for <player>"
    private static final Pattern SEND_BLOCKED_PATTERN = Pattern.compile(
            "send\\(\\) blocked (\\d+) ms");

    // Turn detection from event batch lists: "GameEventTurnBegan" in batch contents
    private static final Pattern BATCH_TURN_BEGAN_PATTERN = Pattern.compile(
            "GameEventTurnBegan");

    // Timestamp at start of log line: "23:03:11" or "13:45:35.885"
    private static final Pattern LINE_TIMESTAMP_PATTERN = Pattern.compile(
            "^(\\d{2}:\\d{2}:\\d{2})");

    // ==================== Protocol-agnostic patterns ====================

    private static final Pattern GAME_COMPLETED_PATTERN = Pattern.compile(
            "Game completed|game finished|Game COMPLETED|isGameOver.*true");

    private static final Pattern CHECKSUM_MISMATCH_PATTERN = Pattern.compile(
            "CHECKSUM MISMATCH|checksum mismatch|desync", Pattern.CASE_INSENSITIVE);

    private static final Pattern ERROR_PATTERN = Pattern.compile(
            "\\[ERROR\\]|\\bERROR\\b:|Exception|exception", Pattern.CASE_INSENSITIVE);

    private static final Pattern TIMEOUT_PATTERN = Pattern.compile(
            "timeout|timed out|time limit exceeded", Pattern.CASE_INSENSITIVE);

    private static final Pattern WARN_PATTERN = Pattern.compile(
            "\\[WARN\\]|WARN:|WARNING:|Unknown protocol method|Non-serializable", Pattern.CASE_INSENSITIVE);

    // JVM/Netty noise that should be suppressed from warning/error counts
    private static final Pattern SUPPRESSED_WARN_PATTERN = Pattern.compile(
            "sun\\.misc\\.Unsafe|io\\.netty|An illegal reflective access|" +
            "WARNING: An illegal reflective|WARNING: Use --illegal-access|" +
            "WARNING: All illegal access operations|" +
            "WARNING: Please consider reporting|" +
            "netty.*reflective|Epoll\\.isAvailable", Pattern.CASE_INSENSITIVE);

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

    // ==================== Error Normalization ====================

    /**
     * Normalize error messages for grouping by removing timestamps, IDs, and other variable data.
     * Static so it can be used by AnalysisResult and other classes.
     */
    public static String normalizeError(String error) {
        // Remove timestamps like [HH:mm:ss.SSS]
        String normalized = error.replaceAll("\\[\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\]", "");
        // Remove id=NNNNN patterns
        normalized = normalized.replaceAll("id=\\d+", "id=X");
        // Remove large numbers (likely IDs or memory addresses)
        normalized = normalized.replaceAll("\\d{5,}", "NNNN");
        // Remove file paths (keep just filename)
        normalized = normalized.replaceAll("[A-Za-z]:[/\\\\][^\\s]+[/\\\\]", "");
        return normalized.trim();
    }

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
        // Qualify filename with parent directory for batch identification in reports
        String parentName = logFile.getParentFile() != null ? logFile.getParentFile().getName() : "";
        metrics.setLogFileName(parentName.isEmpty() ? logFile.getName() : parentName + "/" + logFile.getName());

        // Try to extract game index from filename (e.g., "game5" -> 5)
        Matcher indexMatcher = GAME_INDEX_PATTERN.matcher(logFile.getName());
        if (indexMatcher.find()) {
            try {
                metrics.setGameIndex(Integer.parseInt(indexMatcher.group(1)));
            } catch (NumberFormatException e) {
                // Invalid game index in filename, leave as default
            }
        }

        int maxTurn = 0;
        int batchTurnCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            int packetCount = 0;
            long totalDeltaBytes = 0;
            long totalFullStateBytes = 0;

            while ((line = reader.readLine()) != null) {
                // Track first/last timestamp for game duration
                Matcher tsMatcher = LINE_TIMESTAMP_PATTERN.matcher(line);
                if (tsMatcher.find()) {
                    String ts = tsMatcher.group(1);
                    if (metrics.getFirstTimestamp() == null) {
                        metrics.setFirstTimestamp(ts);
                    }
                    metrics.setLastTimestamp(ts);
                }

                // Encoded message sizes
                Matcher encodedMatcher = ENCODED_BYTES_PATTERN.matcher(line);
                if (encodedMatcher.find()) {
                    try {
                        metrics.recordEncodedMessage(Long.parseLong(encodedMatcher.group(1)));
                    } catch (NumberFormatException e) {
                        // skip
                    }
                    continue;
                }

                // send() blocking times
                Matcher blockedMatcher = SEND_BLOCKED_PATTERN.matcher(line);
                if (blockedMatcher.find()) {
                    try {
                        metrics.recordSendBlocked(Long.parseLong(blockedMatcher.group(1)));
                    } catch (NumberFormatException e) {
                        // skip
                    }
                    continue;
                }

                // Turn detection from batch event lists (GameEventTurnBegan)
                // Used as fallback when no numbered turn lines are present
                if (BATCH_TURN_BEGAN_PATTERN.matcher(line).find()) {
                    Matcher turnMatcher = BATCH_TURN_BEGAN_PATTERN.matcher(line);
                    while (turnMatcher.find()) {
                        batchTurnCount++;
                    }
                }

                // Delta packet metrics (structured logs only)
                Matcher packetMatcher = DELTA_PACKET_PATTERN.matcher(line);
                if (packetMatcher.find()) {
                    try {
                        packetCount++;
                        long deltaBytes = Long.parseLong(packetMatcher.group(2));
                        long fullStateBytes = Long.parseLong(packetMatcher.group(3));

                        totalDeltaBytes += deltaBytes;
                        totalFullStateBytes += fullStateBytes;
                    } catch (NumberFormatException e) {
                        // Skip malformed packet line
                    }
                    continue;
                }

                // Game outcome — try structured pattern first, then generic
                Matcher eventOutcome = GAME_EVENT_OUTCOME_PATTERN.matcher(line);
                if (eventOutcome.find()) {
                    metrics.setWinner(eventOutcome.group(1).trim());
                    metrics.setGameCompleted(true);
                    continue;
                }
                Matcher genericWinner = GENERIC_WINNER_PATTERN.matcher(line);
                if (genericWinner.find()) {
                    String winner = genericWinner.group(1) != null ? genericWinner.group(1) : genericWinner.group(2);
                    if (winner != null) {
                        metrics.setWinner(winner.trim());
                        metrics.setGameCompleted(true);
                    }
                    continue;
                }

                // Structured result line (from multi-process executor output)
                Matcher resultMatcher = RESULT_LINE_PATTERN.matcher(line);
                if (resultMatcher.find()) {
                    metrics.setGameCompleted("SUCCESS".equals(resultMatcher.group(1)));
                    try {
                        metrics.setPlayerCount(Integer.parseInt(resultMatcher.group(2)));
                        metrics.setTurnCount(Integer.parseInt(resultMatcher.group(3)));
                        metrics.setSendErrors(Integer.parseInt(resultMatcher.group(4)));
                    } catch (NumberFormatException e) {
                        // Skip malformed result line
                    }
                    continue;
                }

                // Turn tracking — try structured pattern first, then generic
                Matcher eventTurn = GAME_EVENT_TURN_PATTERN.matcher(line);
                if (eventTurn.find()) {
                    try {
                        int turn = Integer.parseInt(eventTurn.group(1));
                        if (turn > maxTurn) maxTurn = turn;
                    } catch (NumberFormatException e) {
                        // Skip malformed turn line
                    }
                    continue;
                }
                Matcher genericTurn = GENERIC_TURN_PATTERN.matcher(line);
                if (genericTurn.find()) {
                    try {
                        int turn = Integer.parseInt(genericTurn.group(1));
                        if (turn > maxTurn) maxTurn = turn;
                    } catch (NumberFormatException e) {
                        // Skip malformed turn line
                    }
                    // Don't continue — generic turn pattern is broad, line may match other patterns too
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

                // Errors — suppress JVM/Netty noise before counting
                if (ERROR_PATTERN.matcher(line).find()) {
                    if (!SUPPRESSED_WARN_PATTERN.matcher(line).find()) {
                        String truncated = truncateLine(line);
                        metrics.addError(truncated);
                        metrics.incrementErrorCount(normalizeError(truncated));
                        // Record the turn when first error occurred
                        if (metrics.getFirstErrorTurn() < 0 && maxTurn > 0) {
                            metrics.setFirstErrorTurn(maxTurn);
                        }
                    }
                    continue;
                }

                // Warnings — suppress JVM/Netty noise
                if (WARN_PATTERN.matcher(line).find()) {
                    if (!SUPPRESSED_WARN_PATTERN.matcher(line).find()) {
                        metrics.addWarning(truncateLine(line));
                    }
                }
            }

            // Set aggregated metrics
            metrics.setDeltaPacketCount(packetCount);
            metrics.setTotalDeltaBytes(totalDeltaBytes);
            metrics.setTotalFullStateBytes(totalFullStateBytes);
            // Prefer numbered turn from structured/generic patterns; fall back to batch event count
            if (maxTurn > 0) {
                metrics.setTurnCount(maxTurn);
            } else if (batchTurnCount > 0) {
                metrics.setTurnCount(batchTurnCount);
            }

            // Determine failure mode based on log content
            metrics.setFailureMode(determineFailureMode(metrics));

            // Extract per-type error contexts if there are errors
            if (!metrics.getErrors().isEmpty() || metrics.hasChecksumMismatch()) {
                Map<String, ErrorContext> contexts = extractErrorContexts(logFile);
                metrics.setErrorContexts(contexts);
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
            System.err.println("[NetworkLogAnalyzer] Directory not found: " + NetworkLogConfig.sanitizePath(logDirectory.getAbsolutePath()));
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
     * Supports both subdirectory layout and legacy flat layout.
     *
     * @param logDirectory Directory containing log files
     * @return List of GameLogMetrics for comprehensive test logs
     */
    public List<GameLogMetrics> analyzeComprehensiveTestLogs(File logDirectory) {
        // Match patterns for:
        // - Subdirectory layout: network-debug-game0-4p-test.log, network-debug-batch0-game0-4p-test.log
        // - Legacy flat layout: network-debug-run20260128-064643-batch0-game0-4p-test.log
        return analyzeDirectory(logDirectory, "network-debug-.*(?:batch\\d+-)?game\\d+-\\d+p-test\\.log");
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
     * Extract timestamp from log filename or parent directory.
     * Supports formats:
     * - Legacy flat: network-debug-YYYYMMDD-HHMMSS-PID-*.log
     * - Legacy with run prefix: network-debug-runYYYYMMDD-HHMMSS-*.log
     * - Subdirectory layout: run20260309-134822/network-debug-batch0-game0-4p-test.log
     *   (timestamp is in the parent directory name, not the filename)
     */
    private LocalDateTime extractTimestampFromFilename(String filename, DateTimeFormatter formatter) {
        // Try filename timestamp (legacy formats)
        Pattern timestampPattern = Pattern.compile("network-debug-(?:run)?(\\d{8}-\\d{6})-");
        Matcher matcher = timestampPattern.matcher(filename);
        if (matcher.find()) {
            try {
                return LocalDateTime.parse(matcher.group(1), formatter);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
        // Try parent directory timestamp (subdirectory layout: run20260309-134822/...)
        Pattern dirPattern = Pattern.compile("run(\\d{8}-\\d{6})/");
        Matcher dirMatcher = dirPattern.matcher(filename);
        if (dirMatcher.find()) {
            try {
                return LocalDateTime.parse(dirMatcher.group(1), formatter);
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
     * Extract context around each unique error type in a log file.
     * Returns one ErrorContext per normalized error pattern (first occurrence of each).
     *
     * @param logFile The log file to analyze
     * @return Map of normalized error pattern to ErrorContext, or empty map if no errors
     */
    public Map<String, ErrorContext> extractErrorContexts(File logFile) {
        Map<String, ErrorContext> contexts = new LinkedHashMap<>();
        if (logFile == null || !logFile.exists()) {
            return contexts;
        }

        List<String> allLines = new ArrayList<>();
        int currentTurn = 0;
        String currentPhase = null;
        List<String> warningsBefore = new ArrayList<>();

        // Track which normalized errors we've already captured context for
        java.util.Set<String> seenErrors = new java.util.HashSet<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                allLines.add(line);

                // Track turn changes (try structured first, then generic)
                Matcher eventTurn = GAME_EVENT_TURN_PATTERN.matcher(line);
                if (eventTurn.find()) {
                    try {
                        currentTurn = Integer.parseInt(eventTurn.group(1));
                    } catch (NumberFormatException e) {
                        // skip
                    }
                } else {
                    Matcher genericTurn = GENERIC_TURN_PATTERN.matcher(line);
                    if (genericTurn.find()) {
                        try {
                            int t = Integer.parseInt(genericTurn.group(1));
                            if (t > currentTurn) currentTurn = t;
                        } catch (NumberFormatException e) {
                            // skip
                        }
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

                // Track warnings before errors
                if (WARN_PATTERN.matcher(line).find() && !SUPPRESSED_WARN_PATTERN.matcher(line).find()) {
                    warningsBefore.add(line);
                    if (warningsBefore.size() > 50) {
                        warningsBefore.remove(0);
                    }
                }

                // Capture context for each new error type
                if (ERROR_PATTERN.matcher(line).find() && !SUPPRESSED_WARN_PATTERN.matcher(line).find()) {
                    String normalized = normalizeError(line);
                    if (!seenErrors.contains(normalized)) {
                        seenErrors.add(normalized);
                        int errorIdx = lineNumber - 1;

                        int startIdx = Math.max(0, errorIdx - CONTEXT_LINES_BEFORE);
                        int endIdx = Math.min(allLines.size() - 1, errorIdx + CONTEXT_LINES_AFTER);

                        List<String> linesBef = new ArrayList<>();
                        List<String> linesAft = new ArrayList<>();
                        for (int i = startIdx; i < errorIdx; i++) {
                            linesBef.add(allLines.get(i));
                        }
                        // linesAfter will be incomplete for current line (we haven't read ahead)
                        // but that's acceptable — the context before is more valuable

                        List<PlayerState> playerStates = extractPlayerStates(allLines, errorIdx);

                        contexts.put(normalized, new ErrorContext(
                                logFile.getName(),
                                lineNumber,
                                currentTurn,
                                currentPhase,
                                playerStates,
                                linesBef,
                                linesAft,
                                new ArrayList<>(warningsBefore),
                                line
                        ));
                    }
                }
            }

            // Second pass: fill in linesAfter for each context (now that we have all lines)
            for (Map.Entry<String, ErrorContext> entry : contexts.entrySet()) {
                ErrorContext ctx = entry.getValue();
                int errorIdx = ctx.errorLineNumber() - 1; // back to 0-indexed
                int endIdx = Math.min(allLines.size() - 1, errorIdx + CONTEXT_LINES_AFTER);
                List<String> linesAft = new ArrayList<>();
                for (int i = errorIdx + 1; i <= endIdx; i++) {
                    linesAft.add(allLines.get(i));
                }
                if (!linesAft.isEmpty()) {
                    entry.setValue(new ErrorContext(
                            ctx.logFileName(), ctx.errorLineNumber(), ctx.turnAtError(),
                            ctx.phaseAtError(), ctx.playerStates(), ctx.linesBefore(),
                            linesAft, ctx.warningsBefore(), ctx.errorMessage()
                    ));
                }
            }

        } catch (IOException e) {
            // Return whatever contexts we've collected
        }

        return contexts;
    }

    /**
     * Extract context around the first error in a log file.
     * Convenience method for backward compatibility.
     *
     * @param logFile The log file to analyze
     * @return ErrorContext with details, or null if no errors found
     */
    public ErrorContext extractAroundFirstError(File logFile) {
        Map<String, ErrorContext> contexts = extractErrorContexts(logFile);
        return contexts.isEmpty() ? null : contexts.values().iterator().next();
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
