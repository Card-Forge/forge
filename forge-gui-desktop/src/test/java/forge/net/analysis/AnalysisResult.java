package forge.net.analysis;

import forge.net.TestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aggregated analysis across multiple games, built by {@link NetworkLogAnalyzer}
 * from individual {@link GameLogMetrics}. Generates markdown reports and provides
 * validation queries (success rates, checksum mismatches, bandwidth savings).
 */
public class AnalysisResult {

    private final List<GameLogMetrics> allMetrics;
    private final LocalDateTime analysisTime;

    // Cached aggregations
    private int totalGames;
    private int completedGames;
    private int incompleteGames;  // Timeout or unknown
    private int failedGames;     // Catastrophic error / didn't start
    private int gamesWithErrors;
    private int gamesWithWarnings;
    private int gamesWithChecksumMismatches;
    private int totalChecksumMismatches;
    private int maxMismatchesPerGame;  // For persistent/cascading detection
    private int totalSendErrors;

    private long totalDeltaBytes;
    private long totalFullStateBytes;
    private double averageBandwidthSavings;
    private long stateOnlyDeltaBytes;
    private long stateOnlyFullBytes;
    private double stateOnlySavings;
    private int totalTurns;
    private double averageTurns;
    private Set<String> uniqueDeckNames;

    private Map<Integer, PlayerCountStats> statsByPlayerCount;
    private Map<String, FormatStats> statsByFormat;

    // Enhanced error and failure pattern analysis
    private Map<GameLogMetrics.FailureMode, Integer> failureModeCounts;
    private Map<String, Integer> errorFrequency; // Sorted by total count descending
    private Map<String, Integer> errorGameCount; // How many games each error appeared in
    private Map<String, List<String>> errorGameNames; // Which games each error appeared in
    private Map<Integer, BatchStats> batchStats; // Batch number -> stats
    private Map<String, Integer> winnerFrequency; // Winner -> win count

    // Network performance aggregates
    private long totalEncodedBytes;
    private int totalEncodedMessages;
    private long minEncodedBytes;
    private long maxEncodedBytes;
    private int totalSendBlocked;
    private long totalBlockedMs;
    private long minBlockedMs;
    private long maxBlockedMs;


    public AnalysisResult(List<GameLogMetrics> metrics) {
        this.allMetrics = metrics;
        this.analysisTime = LocalDateTime.now();
        aggregateMetrics();
    }

    private void aggregateMetrics() {
        totalGames = allMetrics.size();
        completedGames = (int) allMetrics.stream().filter(GameLogMetrics::isGameCompleted).count();
        incompleteGames = (int) allMetrics.stream()
                .filter(m -> !m.isGameCompleted())
                .filter(m -> m.getFailureMode() == GameLogMetrics.FailureMode.TIMEOUT
                        || m.getFailureMode() == GameLogMetrics.FailureMode.INCOMPLETE)
                .count();
        failedGames = (int) allMetrics.stream()
                .filter(m -> m.getFailureMode() == GameLogMetrics.FailureMode.EXCEPTION)
                .count();
        gamesWithErrors = (int) allMetrics.stream().filter(m -> !m.getErrors().isEmpty()).count();
        gamesWithWarnings = (int) allMetrics.stream().filter(m -> !m.getWarnings().isEmpty()).count();
        gamesWithChecksumMismatches = (int) allMetrics.stream().filter(GameLogMetrics::hasChecksumMismatch).count();
        totalChecksumMismatches = allMetrics.stream().mapToInt(GameLogMetrics::getChecksumMismatchCount).sum();
        maxMismatchesPerGame = allMetrics.stream().mapToInt(GameLogMetrics::getChecksumMismatchCount).max().orElse(0);
        totalSendErrors = allMetrics.stream().mapToInt(GameLogMetrics::getSendErrors).sum();

        totalDeltaBytes = allMetrics.stream().mapToLong(GameLogMetrics::getTotalDeltaBytes).sum();
        totalFullStateBytes = allMetrics.stream().mapToLong(GameLogMetrics::getTotalFullStateBytes).sum();

        if (totalFullStateBytes > 0) {
            averageBandwidthSavings = 100.0 * (1.0 - (double) totalDeltaBytes / totalFullStateBytes);
        }

        stateOnlyDeltaBytes = allMetrics.stream().mapToLong(GameLogMetrics::getStateOnlyDeltaBytes).sum();
        stateOnlyFullBytes = allMetrics.stream().mapToLong(GameLogMetrics::getStateOnlyFullBytes).sum();
        if (stateOnlyFullBytes > 0) {
            stateOnlySavings = 100.0 * (1.0 - (double) stateOnlyDeltaBytes / stateOnlyFullBytes);
        }

        totalTurns = allMetrics.stream()
                .filter(m -> m.getTurnCount() > 0)
                .mapToInt(GameLogMetrics::getTurnCount)
                .sum();

        averageTurns = allMetrics.stream()
                .filter(m -> m.getTurnCount() > 0)
                .mapToInt(GameLogMetrics::getTurnCount)
                .average()
                .orElse(0.0);

        // Collect unique deck names across all games
        uniqueDeckNames = new HashSet<>();
        for (GameLogMetrics m : allMetrics) {
            uniqueDeckNames.addAll(m.getDeckNames());
        }

        // Aggregate by player count
        statsByPlayerCount = new HashMap<>();
        for (int p = 2; p <= 4; p++) {
            final int playerCount = p;
            List<GameLogMetrics> filtered = allMetrics.stream()
                    .filter(m -> m.getPlayerCount() == playerCount)
                    .collect(Collectors.toList());

            if (!filtered.isEmpty()) {
                statsByPlayerCount.put(playerCount, new PlayerCountStats(playerCount, filtered));
            }
        }

        // Aggregate by format
        statsByFormat = new HashMap<>();
        Map<String, List<GameLogMetrics>> byFormat = allMetrics.stream()
                .collect(Collectors.groupingBy(GameLogMetrics::getGameFormat));
        for (Map.Entry<String, List<GameLogMetrics>> entry : byFormat.entrySet()) {
            statsByFormat.put(entry.getKey(), new FormatStats(entry.getKey(), entry.getValue()));
        }

        // Enhanced analysis: failure modes
        failureModeCounts = new EnumMap<>(GameLogMetrics.FailureMode.class);
        for (GameLogMetrics m : allMetrics) {
            GameLogMetrics.FailureMode mode = m.getFailureMode();
            failureModeCounts.merge(mode, 1, Integer::sum);
        }

        // Error frequency with game cross-referencing (using pre-computed errorCounts)
        Map<String, Integer> tempErrorFreq = new HashMap<>();
        Map<String, Integer> tempErrorGames = new HashMap<>();
        Map<String, List<String>> tempErrorGameNames = new HashMap<>();
        for (GameLogMetrics m : allMetrics) {
            for (Map.Entry<String, Integer> ec : m.getErrorCounts().entrySet()) {
                tempErrorFreq.merge(ec.getKey(), ec.getValue(), Integer::sum);
                tempErrorGames.merge(ec.getKey(), 1, Integer::sum);
                tempErrorGameNames.computeIfAbsent(ec.getKey(), k -> new ArrayList<>())
                        .add(m.getLogFileName());
            }
        }
        // Sort by total count, keep top 20
        List<String> topErrors = tempErrorFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        errorFrequency = new LinkedHashMap<>();
        errorGameCount = new LinkedHashMap<>();
        errorGameNames = new LinkedHashMap<>();
        for (String key : topErrors) {
            errorFrequency.put(key, tempErrorFreq.get(key));
            errorGameCount.put(key, tempErrorGames.get(key));
            errorGameNames.put(key, tempErrorGameNames.get(key));
        }

        // Winner frequency
        Map<String, Integer> tempWinnerFreq = new HashMap<>();
        for (GameLogMetrics m : allMetrics) {
            String winner = m.getWinner();
            if (winner != null && !winner.isEmpty()) {
                tempWinnerFreq.merge(winner, 1, Integer::sum);
            }
        }
        winnerFrequency = tempWinnerFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        // Batch stats (10 games per batch)
        batchStats = new LinkedHashMap<>();
        List<GameLogMetrics> sorted = allMetrics.stream()
                .sorted(Comparator.comparingInt(GameLogMetrics::getGameIndex))
                .collect(Collectors.toList());
        for (int i = 0; i < sorted.size(); i += 10) {
            int batchNum = i / 10;
            List<GameLogMetrics> batch = sorted.subList(i, Math.min(i + 10, sorted.size()));
            batchStats.put(batchNum, new BatchStats(batchNum, batch));
        }

        // Network performance aggregates
        totalEncodedBytes = allMetrics.stream().mapToLong(GameLogMetrics::getTotalEncodedBytes).sum();
        totalEncodedMessages = allMetrics.stream().mapToInt(GameLogMetrics::getEncodedMessageCount).sum();
        minEncodedBytes = allMetrics.stream()
                .filter(m -> m.getEncodedMessageCount() > 0)
                .mapToLong(GameLogMetrics::getMinEncodedBytes)
                .min().orElse(0);
        maxEncodedBytes = allMetrics.stream().mapToLong(GameLogMetrics::getMaxEncodedBytes).max().orElse(0);
        totalSendBlocked = allMetrics.stream().mapToInt(GameLogMetrics::getSendBlockedCount).sum();
        totalBlockedMs = allMetrics.stream().mapToLong(GameLogMetrics::getTotalBlockedMs).sum();
        minBlockedMs = allMetrics.stream()
                .filter(m -> m.getSendBlockedCount() > 0)
                .mapToLong(GameLogMetrics::getMinBlockedMs)
                .min().orElse(0);
        maxBlockedMs = allMetrics.stream().mapToLong(GameLogMetrics::getMaxBlockedMs).max().orElse(0);
    }

    // Getters

    public int getTotalGames() { return totalGames; }
    public int getCompletedGames() { return completedGames; }
    public int getIncompleteGames() { return incompleteGames; }
    public int getFailedGames() { return failedGames; }
    public int getTotalSendErrors() { return totalSendErrors; }
    public int getMaxMismatchesPerGame() { return maxMismatchesPerGame; }
    public List<GameLogMetrics> getAllMetrics() { return allMetrics; }

    public double getCompletionRate() {
        if (totalGames == 0) return 0.0;
        return 100.0 * completedGames / totalGames;
    }

    public int getGamesWithChecksumMismatches() {
        return gamesWithChecksumMismatches;
    }

    public PlayerCountStats getStatsByPlayerCount(int playerCount) {
        return statsByPlayerCount.get(playerCount);
    }

    private int getUniqueDeckCount() {
        return uniqueDeckNames != null ? uniqueDeckNames.size() : 0;
    }

    private boolean hasDeltaSyncData() {
        return totalFullStateBytes > 0;
    }

    /** Compute session duration from first/last timestamps across all games. */
    private String computeAggregateDuration() {
        String earliest = null;
        String latest = null;
        for (GameLogMetrics m : allMetrics) {
            String first = m.getFirstTimestamp();
            String last = m.getLastTimestamp();
            if (first != null && (earliest == null || first.compareTo(earliest) < 0)) {
                earliest = first;
            }
            if (last != null && (latest == null || last.compareTo(latest) > 0)) {
                latest = last;
            }
        }
        if (earliest == null || latest == null) return null;
        try {
            java.time.LocalTime start = java.time.LocalTime.parse(earliest);
            java.time.LocalTime end = java.time.LocalTime.parse(latest);
            long seconds = java.time.Duration.between(start, end).getSeconds();
            if (seconds <= 0) return null;
            return formatDurationSeconds(seconds);
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatDurationSeconds(long seconds) {
        if (seconds < 60) return seconds + " sec";
        long min = seconds / 60;
        long sec = seconds % 60;
        if (min < 60) return String.format("%d min %d sec", min, sec);
        long hrs = min / 60;
        min = min % 60;
        return String.format("%d hr %d min", hrs, min);
    }

    private static String formatDurationMs(long ms) {
        if (ms < 1000) return ms + " ms";
        return formatDurationSeconds(ms / 1000);
    }

    /** True when logs come from a batch test run (filenames contain gameN indices). */
    private boolean isBatchTestData() {
        return allMetrics.stream().anyMatch(m -> m.getGameIndex() >= 0);
    }

    /**
     * Get all unique errors across all games.
     */
    public List<String> getAllErrors() {
        return allMetrics.stream()
                .flatMap(m -> m.getErrors().stream())
                .distinct()
                .limit(100)
                .collect(Collectors.toList());
    }

    /**
     * Get all unique warnings across all games.
     */
    public List<String> getAllWarnings() {
        return allMetrics.stream()
                .flatMap(m -> m.getWarnings().stream())
                .distinct()
                .limit(100)
                .collect(Collectors.toList());
    }

    /**
     * Get all files that have any warnings.
     * @return list of log file names that have warnings
     */
    public List<String> getFilesWithAnyWarnings() {
        return allMetrics.stream()
                .filter(m -> !m.getWarnings().isEmpty())
                .map(GameLogMetrics::getLogFileName)
                .collect(Collectors.toList());
    }

    /**
     * Get all files that have any errors.
     * @return list of log file names that have errors
     */
    public List<String> getFilesWithAnyErrors() {
        return allMetrics.stream()
                .filter(m -> !m.getErrors().isEmpty())
                .map(GameLogMetrics::getLogFileName)
                .collect(Collectors.toList());
    }

    /**
     * Generate a markdown report.
     * Adapts content based on whether delta sync data is present.
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();

        sb.append("_Generated by forge.net.analysis.AnalysisResult (NetworkLogAnalyzer)_\n\n");
        sb.append("## Network Log Analysis Results\n\n");
        sb.append(String.format("**Analysis Date:** %s\n\n",
                analysisTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        // Game Completion
        sb.append("### Game Completion\n\n");
        sb.append("| Total | Completed | Incomplete | Failed |\n");
        sb.append("|-------|-----------|------------|--------|\n");
        sb.append(String.format("| %d | %d | %d | %d |\n", totalGames, completedGames, incompleteGames, failedGames));
        if (getUniqueDeckCount() > 0) {
            sb.append(String.format("\n**Unique Decks Used:** %d\n", getUniqueDeckCount()));
        }
        sb.append("\n");

        // Data Integrity
        sb.append("### Data Integrity\n\n");
        sb.append("| Checksum Mismatches (games) | Total Mismatches | Errors (games) | Warnings (games) |\n");
        sb.append("|----------------------------|------------------|----------------|------------------|\n");
        sb.append(String.format("| %d | %d | %d | %d |\n",
                gamesWithChecksumMismatches, totalChecksumMismatches, gamesWithErrors, gamesWithWarnings));
        sb.append("\n");

        // Network Performance
        boolean hasNetPerf = totalEncodedMessages > 0 || totalTurns > 0;
        if (hasNetPerf) {
            sb.append("### Network Performance\n\n");
            sb.append("| Metric | Value |\n");
            sb.append("|--------|-------|\n");

            // Session duration and turns
            String duration = computeAggregateDuration();
            if (duration != null) {
                sb.append(String.format("| Session Duration | %s |\n", duration));
            }
            if (totalTurns > 0) {
                if (totalGames == 1) {
                    sb.append(String.format("| Turns | %d |\n", totalTurns));
                } else {
                    sb.append(String.format("| Total Turns | %d |\n", totalTurns));
                    sb.append(String.format("| Average Turns | %.1f |\n", averageTurns));
                }
            }

            // Large messages (>20KB threshold logged by CompatibleObjectEncoder)
            if (totalEncodedMessages > 0) {
                sb.append(String.format("| Large Messages (>20KB) | %,d |\n",
                        totalEncodedMessages));
                sb.append(String.format("| Large Message Traffic | %s |\n",
                        TestUtils.formatBytes(totalEncodedBytes)));
                sb.append(String.format("| Avg Large Message Size | %s |\n",
                        TestUtils.formatBytes((long) ((double) totalEncodedBytes / totalEncodedMessages))));
                sb.append(String.format("| Large Message Size Range | %s - %s |\n",
                        TestUtils.formatBytes(minEncodedBytes), TestUtils.formatBytes(maxEncodedBytes)));
            }

            // send() blocking
            if (totalSendBlocked > 0) {
                sb.append(String.format("| send() Blocked Count | %,d |\n",
                        totalSendBlocked));
                sb.append(String.format("| Avg Blocking Time | %d ms |\n",
                        totalBlockedMs / totalSendBlocked));
                sb.append(String.format("| Blocking Range | %d - %,d ms |\n",
                        minBlockedMs, maxBlockedMs));
                sb.append(String.format("| Total Thread Blocking | %s |\n",
                        formatDurationMs(totalBlockedMs)));
            }

            if (totalSendErrors > 0) {
                sb.append(String.format("| Total Send Errors | %d |\n", totalSendErrors));
            }
            sb.append("\n");
        }

        // Delta Sync Bandwidth (only if delta sync data present)
        if (hasDeltaSyncData()) {
            sb.append("### Delta Sync Bandwidth\n\n");
            sb.append("| Metric | Total | Avg per Game |\n");
            sb.append("|--------|-------|--------------|\n");
            long avgDelta = totalGames > 0 ? totalDeltaBytes / totalGames : 0;
            long avgFull = totalGames > 0 ? totalFullStateBytes / totalGames : 0;
            sb.append(String.format("| **Delta** | **%s** | **%s** |\n",
                    TestUtils.formatBytes(totalDeltaBytes), TestUtils.formatBytes(avgDelta)));
            if (stateOnlyFullBytes > 0) {
                long avgSoDelta = totalGames > 0 ? stateOnlyDeltaBytes / totalGames : 0;
                long eventsDelta = totalDeltaBytes - stateOnlyDeltaBytes;
                long avgEventsDelta = totalGames > 0 ? eventsDelta / totalGames : 0;
                sb.append(String.format("| \u00a0\u00a0\u00a0State only | %s | %s |\n",
                        TestUtils.formatBytes(stateOnlyDeltaBytes), TestUtils.formatBytes(avgSoDelta)));
                sb.append(String.format("| \u00a0\u00a0\u00a0Events | %s | %s |\n",
                        TestUtils.formatBytes(eventsDelta), TestUtils.formatBytes(avgEventsDelta)));
            }
            sb.append(String.format("| **FullState** | **%s** | **%s** |\n",
                    TestUtils.formatBytes(totalFullStateBytes), TestUtils.formatBytes(avgFull)));
            if (stateOnlyFullBytes > 0) {
                long avgSoFull = totalGames > 0 ? stateOnlyFullBytes / totalGames : 0;
                long eventsFull = totalFullStateBytes - stateOnlyFullBytes;
                long avgEventsFull = totalGames > 0 ? eventsFull / totalGames : 0;
                sb.append(String.format("| \u00a0\u00a0\u00a0State only | %s | %s |\n",
                        TestUtils.formatBytes(stateOnlyFullBytes), TestUtils.formatBytes(avgSoFull)));
                sb.append(String.format("| \u00a0\u00a0\u00a0Events | %s | %s |\n",
                        TestUtils.formatBytes(eventsFull), TestUtils.formatBytes(avgEventsFull)));
            }
            sb.append("\n");
            sb.append(String.format("**Savings (overall):** %.1f%%", averageBandwidthSavings));
            if (stateOnlyFullBytes > 0) {
                sb.append(String.format(" | **State only:** %.1f%%", stateOnlySavings));
            }
            sb.append("\n\n");
        }

        // Results by Player Count (batch tests only)
        if (isBatchTestData() && !statsByPlayerCount.isEmpty()) {
            sb.append("### Results by Player Count\n\n");
            if (hasDeltaSyncData()) {
                sb.append("| Players | Games | Success Rate | Avg Turns | Avg Savings | Send Errors |\n");
                sb.append("|---------|-------|--------------|-----------|-------------|-------------|\n");
            } else {
                sb.append("| Players | Games | Success Rate | Avg Turns | Send Errors |\n");
                sb.append("|---------|-------|--------------|-----------|-------------|\n");
            }
            for (int p = 2; p <= 4; p++) {
                PlayerCountStats stats = statsByPlayerCount.get(p);
                if (stats != null) {
                    if (hasDeltaSyncData()) {
                        sb.append(String.format("| %d | %d | %.1f%% | %.1f | %.1f%% | %d |\n",
                                p, stats.gameCount, stats.successRate, stats.averageTurns,
                                stats.averageBandwidthSavings, stats.totalSendErrors));
                    } else {
                        sb.append(String.format("| %d | %d | %.1f%% | %.1f | %d |\n",
                                p, stats.gameCount, stats.successRate, stats.averageTurns,
                                stats.totalSendErrors));
                    }
                }
            }
            sb.append("\n");

            // Bandwidth by player count (only if delta sync data present)
            if (hasDeltaSyncData()) {
                sb.append("### Bandwidth by Player Count\n\n");
                sb.append("| Players | Games | Delta | Avg Delta | FullState | Avg FullState | Savings |\n");
                sb.append("|---------|-------|-------|-----------|-----------|---------------|--------|\n");
                for (int p = 2; p <= 4; p++) {
                    PlayerCountStats stats = statsByPlayerCount.get(p);
                    if (stats != null) {
                        long avgDeltaPerGame = stats.gameCount > 0 ? stats.totalDeltaBytes / stats.gameCount : 0;
                        long avgFullPerGame = stats.gameCount > 0 ? stats.totalFullStateBytes / stats.gameCount : 0;
                        sb.append(String.format("| %d | %d | %s | %s | %s | %s | %.1f%% |\n",
                                p, stats.gameCount,
                                TestUtils.formatBytes(stats.totalDeltaBytes), TestUtils.formatBytes(avgDeltaPerGame),
                                TestUtils.formatBytes(stats.totalFullStateBytes), TestUtils.formatBytes(avgFullPerGame),
                                stats.averageBandwidthSavings));
                    }
                }
                sb.append("\n");
            }
        }

        // Results by Format (only show if there are multiple formats)
        if (isBatchTestData() && statsByFormat.size() > 1) {
            sb.append("### Results by Format\n\n");
            if (hasDeltaSyncData()) {
                sb.append("| Format | Games | Success Rate | Avg Turns | Avg Savings |\n");
                sb.append("|--------|-------|--------------|-----------|-------------|\n");
            } else {
                sb.append("| Format | Games | Success Rate | Avg Turns |\n");
                sb.append("|--------|-------|--------------|----------|\n");
            }
            for (FormatStats stats : statsByFormat.values()) {
                if (hasDeltaSyncData()) {
                    sb.append(String.format("| %s | %d | %.1f%% | %.1f | %.1f%% |\n",
                            stats.format, stats.gameCount, stats.successRate,
                            stats.averageTurns, stats.averageBandwidthSavings));
                } else {
                    sb.append(String.format("| %s | %d | %.1f%% | %.1f |\n",
                            stats.format, stats.gameCount, stats.successRate, stats.averageTurns));
                }
            }
            sb.append("\n");
        }

        // Winner distribution
        if (!winnerFrequency.isEmpty()) {
            sb.append("### Winner Distribution\n\n");
            sb.append("| Player | Wins | % |\n");
            sb.append("|--------|------|---|\n");
            for (Map.Entry<String, Integer> entry : winnerFrequency.entrySet()) {
                double pct = completedGames > 0 ? 100.0 * entry.getValue() / completedGames : 0;
                sb.append(String.format("| %s | %d | %.1f%% |\n",
                        entry.getKey(), entry.getValue(), pct));
            }
            sb.append("\n");
        }

        // Failure Mode Analysis (before error analysis for quick triage)
        if (incompleteGames + failedGames > 0) {
            sb.append("### Failure Mode Analysis\n\n");
            sb.append("| Mode | Count | % |\n");
            sb.append("|------|-------|---|\n");
            for (GameLogMetrics.FailureMode mode : GameLogMetrics.FailureMode.values()) {
                if (mode == GameLogMetrics.FailureMode.NONE) continue;
                int count = failureModeCounts.getOrDefault(mode, 0);
                if (count > 0) {
                    double pct = totalGames > 0 ? 100.0 * count / totalGames : 0;
                    sb.append(String.format("| %s | %d | %.1f%% |\n", mode.name(), count, pct));
                }
            }
            sb.append("\n");
        }

        // Error Analysis
        if (gamesWithErrors > 0 || gamesWithChecksumMismatches > 0) {
            sb.append("### Error Analysis\n\n");

            if (gamesWithChecksumMismatches > 0) {
                sb.append(String.format("**Checksum Mismatches:** %d game(s), %d total (%s)\n\n",
                        gamesWithChecksumMismatches, totalChecksumMismatches,
                        maxMismatchesPerGame <= 1 ? "transient only" : "persistent detected"));
            }

            // List files with errors for quick reference
            List<String> filesWithErrors = getFilesWithAnyErrors();
            if (!filesWithErrors.isEmpty()) {
                sb.append("**Files with Errors:**\n");
                for (String file : filesWithErrors) {
                    sb.append(String.format("- `%s`\n", file));
                }
                sb.append("\n");
            }
        }

        // Checksum Mismatch Details (server vs client breakdown comparison)
        if (gamesWithChecksumMismatches > 0) {
            boolean hasAnyDetails = allMetrics.stream()
                    .anyMatch(m -> !m.getChecksumMismatchDetails().isEmpty());
            if (hasAnyDetails) {
                sb.append("### Checksum Mismatch Details\n\n");
                for (GameLogMetrics m : allMetrics) {
                    if (m.getChecksumMismatchDetails().isEmpty()) continue;
                    sb.append(String.format("#### `%s`\n\n", m.getLogFileName()));
                    for (NetworkLogAnalyzer.ChecksumMismatchDetail detail : m.getChecksumMismatchDetails()) {
                        sb.append(detail.toMarkdown());
                        sb.append("\n");
                    }
                }
            }
        }

        // Top errors with game cross-referencing
        if (!errorFrequency.isEmpty()) {
            sb.append("### Top Errors (by frequency)\n\n");
            sb.append("| Error Pattern | Games | Count | Log Files |\n");
            sb.append("|---------------|-------|-------|-----------|\n");
            int shown = 0;
            for (Map.Entry<String, Integer> entry : errorFrequency.entrySet()) {
                if (shown++ >= 10) break;
                String errorTruncated = entry.getKey().length() > 80 ?
                        entry.getKey().substring(0, 77) + "..." : entry.getKey();
                int games = errorGameCount.getOrDefault(entry.getKey(), 0);
                List<String> gameNames = errorGameNames.getOrDefault(entry.getKey(), List.of());
                String logFiles = String.join(", ", gameNames);
                sb.append(String.format("| `%s` | %d | %d | %s |\n",
                        errorTruncated, games, entry.getValue(), logFiles));
            }
            sb.append("\n");

            // Error context — one example per distinct error type, collected across all games
            Map<String, NetworkLogAnalyzer.ErrorContext> distinctContexts = new LinkedHashMap<>();
            for (GameLogMetrics m : allMetrics) {
                for (Map.Entry<String, NetworkLogAnalyzer.ErrorContext> entry : m.getErrorContexts().entrySet()) {
                    distinctContexts.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }

            if (!distinctContexts.isEmpty()) {
                sb.append("### Error Context (first occurrence per unique error type)\n\n");
                for (NetworkLogAnalyzer.ErrorContext ctx : distinctContexts.values()) {
                    sb.append(ctx.toMarkdown());
                    sb.append("\n");
                }
            }
        }

        // Warning Analysis
        if (gamesWithWarnings > 0) {
            sb.append("### Warning Analysis\n\n");
            sb.append(String.format("**Games with Warnings:** %d\n\n", gamesWithWarnings));

            List<String> warnings = getAllWarnings();
            if (!warnings.isEmpty()) {
                sb.append("**Unique Warnings:**\n");
                for (String warning : warnings.subList(0, Math.min(10, warnings.size()))) {
                    sb.append(String.format("- `%s`\n", warning));
                }
                if (warnings.size() > 10) {
                    sb.append(String.format("- ... and %d more\n", warnings.size() - 10));
                }
                sb.append("\n");
            }
        }

        // Batch Performance Section (batch tests only)
        if (isBatchTestData() && batchStats != null && !batchStats.isEmpty()) {
            sb.append("### Batch Performance\n\n");
            sb.append("| Batch | Games | Success Rate | Failures |\n");
            sb.append("|-------|-------|--------------|----------|\n");
            for (BatchStats batch : batchStats.values()) {
                String failures = batch.failedGames.isEmpty() ? "-" :
                        batch.failedGames.stream().limit(3).collect(Collectors.joining(", "));
                if (batch.failedGames.size() > 3) {
                    failures += ", ...";
                }
                sb.append(String.format("| %d | %d | %.0f%% | %s |\n",
                        batch.batchNumber, batch.gameCount, batch.successRate, failures));
            }
            sb.append("\n");
        }

        // Validation Summary (batch tests only)
        if (isBatchTestData()) {
            sb.append("### Validation Status\n\n");
            boolean passed = passesValidation();

            if (passed) {
                sb.append("**PASSED** - All validation criteria met:\n");
            } else {
                sb.append("**FAILED** - Validation criteria not met:\n");
            }
            sb.append(String.format("- [%s] Completion rate >= 90%% (actual: %.1f%%)\n",
                    getCompletionRate() >= 90.0 ? "x" : " ", getCompletionRate()));
            sb.append(String.format("- [%s] Zero send errors (actual: %d)\n",
                    totalSendErrors == 0 ? "x" : " ", totalSendErrors));
            if (hasDeltaSyncData()) {
                sb.append(String.format("- [%s] Average bandwidth savings >= 90%% (actual: %.1f%%)\n",
                        averageBandwidthSavings >= 90.0 ? "x" : " ", averageBandwidthSavings));
            }
            sb.append(String.format("- [%s] No persistent checksum mismatches (max %d per game, threshold: 1)\n",
                    maxMismatchesPerGame <= 1 ? "x" : " ", maxMismatchesPerGame));

            // Per-player-count completion rates
            for (int p = 2; p <= 4; p++) {
                PlayerCountStats stats = statsByPlayerCount.get(p);
                if (stats != null) {
                    sb.append(String.format("- [%s] %dp completion rate >= 80%% (actual: %.1f%%)\n",
                            stats.successRate >= 80.0 ? "x" : " ", p, stats.successRate));
                }
            }
            sb.append("\n");
        }

        // List of analyzed log files
        sb.append("### Analyzed Log Files\n\n");
        sb.append(String.format("**Total files analyzed:** %d\n\n", totalGames));
        if (!allMetrics.isEmpty()) {
            List<GameLogMetrics> sortedMetrics = allMetrics.stream()
                    .sorted(Comparator.comparingInt(GameLogMetrics::getGameIndex))
                    .collect(Collectors.toList());

            boolean hasMultipleFormats = statsByFormat.size() > 1;
            if (hasMultipleFormats) {
                sb.append("| # | Log File | Status | Format | Players | Turns | Packets | Checksums | Mismatches | Winner |\n");
                sb.append("|---|----------|--------|--------|---------|-------|---------|-----------|------------|--------|\n");
            } else {
                sb.append("| # | Log File | Status | Players | Turns | Packets | Checksums | Mismatches | Winner |\n");
                sb.append("|---|----------|--------|---------|-------|---------|-----------|------------|--------|\n");
            }
            for (GameLogMetrics m : sortedMetrics) {
                String status;
                if (m.isGameCompleted()) {
                    status = m.hasChecksumMismatch() ? "DESYNC" : "OK";
                } else {
                    status = m.getFailureMode().name();
                }
                String winner = m.getWinner() != null ? m.getWinner() : "-";
                if (hasMultipleFormats) {
                    sb.append(String.format("| %d | `%s` | %s | %s | %d | %d | %d | %d | %d | %s |\n",
                            m.getGameIndex(),
                            m.getLogFileName(),
                            status,
                            m.getGameFormat(),
                            m.getPlayerCount(),
                            m.getTurnCount(),
                            m.getDeltaPacketCount(),
                            m.getChecksumCount(),
                            m.getChecksumMismatchCount(),
                            winner));
                } else {
                    sb.append(String.format("| %d | `%s` | %s | %d | %d | %d | %d | %d | %s |\n",
                            m.getGameIndex(),
                            m.getLogFileName(),
                            status,
                            m.getPlayerCount(),
                            m.getTurnCount(),
                            m.getDeltaPacketCount(),
                            m.getChecksumCount(),
                            m.getChecksumMismatchCount(),
                            winner));
                }
            }
        }

        return sb.toString();
    }

    /**
     * Generate a plain text summary.
     */
    public String toSummary() {
        return String.format(
                "AnalysisResult[total=%d, completed=%d (%.1f%%), incomplete=%d, failed=%d, " +
                "sendErrors=%d, errors=%d, checksumMismatches=%d (max %d/game), avgSavings=%.1f%%, avgTurns=%.1f]",
                totalGames, completedGames, getCompletionRate(), incompleteGames, failedGames,
                totalSendErrors, gamesWithErrors, gamesWithChecksumMismatches, maxMismatchesPerGame,
                averageBandwidthSavings, averageTurns);
    }

    /**
     * Check if validation criteria are met.
     * Checksum mismatches: transient (max 1 per game) are tolerable,
     * persistent/cascading (2+ per game) fail validation.
     */
    public boolean passesValidation() {
        if (getCompletionRate() < 90.0) return false;
        if (totalSendErrors > 0) return false;
        if (maxMismatchesPerGame > 1) return false;
        if (hasDeltaSyncData() && averageBandwidthSavings < 90.0) return false;
        for (PlayerCountStats stats : statsByPlayerCount.values()) {
            if (stats.successRate < 80.0) return false;
        }
        return true;
    }

    /**
     * Statistics for a specific player count.
     */
    public static class PlayerCountStats {
        public final int playerCount;
        public final int gameCount;
        public final int successCount;
        public final double successRate;
        public final double averageTurns;
        public final double averageBandwidthSavings;
        public final long totalDeltaBytes;
        public final long totalFullStateBytes;
        public final int totalSendErrors;

        public PlayerCountStats(int playerCount, List<GameLogMetrics> metrics) {
            this.playerCount = playerCount;
            this.gameCount = metrics.size();
            this.successCount = (int) metrics.stream().filter(GameLogMetrics::isSuccessful).count();
            this.successRate = gameCount > 0 ? 100.0 * successCount / gameCount : 0.0;

            this.averageTurns = metrics.stream()
                    .filter(GameLogMetrics::isGameCompleted)
                    .mapToInt(GameLogMetrics::getTurnCount)
                    .average()
                    .orElse(0.0);

            this.totalDeltaBytes = metrics.stream().mapToLong(GameLogMetrics::getTotalDeltaBytes).sum();
            this.totalFullStateBytes = metrics.stream().mapToLong(GameLogMetrics::getTotalFullStateBytes).sum();

            this.averageBandwidthSavings = totalFullStateBytes > 0
                    ? 100.0 * (1.0 - (double) totalDeltaBytes / totalFullStateBytes)
                    : 0.0;

            this.totalSendErrors = metrics.stream().mapToInt(GameLogMetrics::getSendErrors).sum();
        }
    }

    /**
     * Statistics for a batch of games (typically 10 games per batch).
     */
    public static class BatchStats {
        public final int batchNumber;
        public final int gameCount;
        public final int successCount;
        public final int failureCount;
        public final double successRate;
        public final List<String> failedGames;

        public BatchStats(int batchNumber, List<GameLogMetrics> games) {
            this.batchNumber = batchNumber;
            this.gameCount = games.size();
            this.successCount = (int) games.stream().filter(GameLogMetrics::isSuccessful).count();
            this.failureCount = gameCount - successCount;
            this.successRate = gameCount > 0 ? 100.0 * successCount / gameCount : 0.0;
            this.failedGames = games.stream()
                    .filter(m -> !m.isSuccessful())
                    .map(m -> {
                        String reason = m.getFailureMode().name().toLowerCase();
                        return m.getLogFileName() + " (" + reason + ")";
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * Statistics for a specific game format (Constructed, Commander, etc.).
     */
    public static class FormatStats {
        public final String format;
        public final int gameCount;
        public final int successCount;
        public final double successRate;
        public final double averageTurns;
        public final double averageBandwidthSavings;
        public final long totalDeltaBytes;
        public final long totalFullStateBytes;

        public FormatStats(String format, List<GameLogMetrics> metrics) {
            this.format = format;
            this.gameCount = metrics.size();
            this.successCount = (int) metrics.stream().filter(GameLogMetrics::isSuccessful).count();
            this.successRate = gameCount > 0 ? 100.0 * successCount / gameCount : 0.0;

            this.averageTurns = metrics.stream()
                    .filter(GameLogMetrics::isGameCompleted)
                    .mapToInt(GameLogMetrics::getTurnCount)
                    .average()
                    .orElse(0.0);

            this.totalDeltaBytes = metrics.stream().mapToLong(GameLogMetrics::getTotalDeltaBytes).sum();
            this.totalFullStateBytes = metrics.stream().mapToLong(GameLogMetrics::getTotalFullStateBytes).sum();

            this.averageBandwidthSavings = totalFullStateBytes > 0
                    ? 100.0 * (1.0 - (double) totalDeltaBytes / totalFullStateBytes)
                    : 0.0;
        }
    }
}
