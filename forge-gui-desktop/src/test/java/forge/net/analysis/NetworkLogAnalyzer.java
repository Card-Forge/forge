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

/**
 * Parses network debug log files to extract delta sync metrics and errors.
 *
 * Uses single-pass parsing with pre-compiled regex patterns for efficiency.
 * Supports parallel file analysis via parallelStream().
 */
public class NetworkLogAnalyzer {

    // Pre-compiled regex patterns for efficiency
    private static final Pattern DELTA_PACKET_PATTERN = Pattern.compile(
            "\\[DeltaSync\\] Packet #(\\d+): Approximate=(\\d+) bytes, ActualNetwork=(\\d+) bytes, FullState=(\\d+) bytes");

    private static final Pattern SAVINGS_PATTERN = Pattern.compile(
            "Savings: Approximate=(\\d+)%, Actual=(\\d+)%");

    private static final Pattern GAME_OUTCOME_PATTERN = Pattern.compile(
            "\\[GameEvent\\] Game outcome: winner = (.+)");

    private static final Pattern TURN_PATTERN = Pattern.compile(
            "\\[GameEvent\\] Turn (\\d+) began");

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
            metrics.setGameIndex(Integer.parseInt(indexMatcher.group(1)));
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

                            metrics.addPacket(new GameLogMetrics.PacketMetrics(
                                    packetNum, approxBytes, actualBytes, fullStateBytes,
                                    approxSavings, actualSavings));
                        }
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
                    int turn = Integer.parseInt(turnMatcher.group(1));
                    if (turn > maxTurn) {
                        maxTurn = turn;
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
                            int count = Integer.parseInt(match);
                            if (count >= 2 && count <= 4) {
                                metrics.setPlayerCount(count);
                                break;
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

            // Calculate average savings
            if (!approximateSavingsList.isEmpty()) {
                double avgApprox = approximateSavingsList.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                metrics.setAverageBandwidthSavingsApproximate(avgApprox);
            }

            if (!actualSavingsList.isEmpty()) {
                double avgActual = actualSavingsList.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);
                metrics.setAverageBandwidthSavingsActual(avgActual);
            }

            // Determine failure mode based on log content
            metrics.setFailureMode(determineFailureMode(metrics));

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
     * Analyze all log files in a directory.
     *
     * @param logDirectory Directory containing log files
     * @return List of GameLogMetrics for each file
     */
    public List<GameLogMetrics> analyzeDirectory(File logDirectory) {
        return analyzeDirectory(logDirectory, "network-debug-*.log");
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
            System.err.println("[NetworkLogAnalyzer] Directory not found: " + logDirectory.getAbsolutePath());
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
     * Analyze recent test logs (those with "-test" suffix).
     *
     * @param logDirectory Directory containing log files
     * @return List of GameLogMetrics for test logs only
     */
    public List<GameLogMetrics> analyzeTestLogs(File logDirectory) {
        return analyzeDirectory(logDirectory, "network-debug-*-test.log");
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
     * Analyze a directory and return an aggregated result.
     *
     * @param logDirectory Directory containing log files
     * @return Aggregated analysis result
     */
    public AnalysisResult analyzeAndAggregate(File logDirectory) {
        List<GameLogMetrics> metrics = analyzeDirectory(logDirectory);
        return buildAnalysisResult(metrics);
    }

    /**
     * Analyze comprehensive test logs and return an aggregated result.
     *
     * @param logDirectory Directory containing log files
     * @return Aggregated analysis result
     */
    public AnalysisResult analyzeComprehensiveTestAndAggregate(File logDirectory) {
        List<GameLogMetrics> metrics = analyzeComprehensiveTestLogs(logDirectory);
        return buildAnalysisResult(metrics);
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
}
