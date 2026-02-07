package forge.net.analysis;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * Aggregates results across all analyzed games and generates reports.
 */
public class AnalysisResult {

    private final List<GameLogMetrics> allMetrics;
    private final LocalDateTime analysisTime;

    // Cached aggregations
    private int totalGames;
    private int successfulGames;
    private int failedGames;
    private int gamesWithErrors;
    private int gamesWithWarnings;
    private int gamesWithChecksumMismatches;

    private long totalApproximateBytes;
    private long totalDeltaBytes;
    private long totalFullStateBytes;
    private double averageBandwidthSavings;
    private int totalTurns;
    private double averageTurns;
    private Set<String> uniqueDeckNames;

    private Map<Integer, PlayerCountStats> statsByPlayerCount;

    // Enhanced error and failure pattern analysis
    private Map<GameLogMetrics.FailureMode, Integer> failureModeCounts;
    private Map<String, Integer> errorFrequency; // Sorted by frequency descending
    private Map<Integer, BatchStats> batchStats; // Batch number -> stats

    // Failure patterns
    private int maxConsecutiveFailures;
    private double firstHalfSuccessRate;
    private double secondHalfSuccessRate;
    private int warningsLeadingToFailure;

    // Turn distribution histogram: [0]=1-5, [1]=6-10, [2]=11-20, [3]=21-30, [4]=30+
    private int[] turnHistogram = new int[5];

    public AnalysisResult(List<GameLogMetrics> metrics) {
        this.allMetrics = metrics;
        this.analysisTime = LocalDateTime.now();
        aggregateMetrics();
    }

    private void aggregateMetrics() {
        totalGames = allMetrics.size();
        successfulGames = (int) allMetrics.stream().filter(GameLogMetrics::isSuccessful).count();
        failedGames = totalGames - successfulGames;
        gamesWithErrors = (int) allMetrics.stream().filter(m -> !m.getErrors().isEmpty()).count();
        gamesWithWarnings = (int) allMetrics.stream().filter(m -> !m.getWarnings().isEmpty()).count();
        gamesWithChecksumMismatches = (int) allMetrics.stream().filter(GameLogMetrics::hasChecksumMismatch).count();

        totalApproximateBytes = allMetrics.stream().mapToLong(GameLogMetrics::getTotalApproximateBytes).sum();
        totalDeltaBytes = allMetrics.stream().mapToLong(GameLogMetrics::getTotalDeltaBytes).sum();
        totalFullStateBytes = allMetrics.stream().mapToLong(GameLogMetrics::getTotalFullStateBytes).sum();

        if (totalFullStateBytes > 0) {
            averageBandwidthSavings = 100.0 * (1.0 - (double) totalDeltaBytes / totalFullStateBytes);
        }

        totalTurns = allMetrics.stream()
                .filter(GameLogMetrics::isGameCompleted)
                .mapToInt(GameLogMetrics::getTurnCount)
                .sum();

        averageTurns = allMetrics.stream()
                .filter(GameLogMetrics::isGameCompleted)
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

        // Enhanced analysis: failure modes
        failureModeCounts = new EnumMap<>(GameLogMetrics.FailureMode.class);
        for (GameLogMetrics m : allMetrics) {
            GameLogMetrics.FailureMode mode = m.getFailureMode();
            failureModeCounts.merge(mode, 1, Integer::sum);
        }

        // Error frequency analysis (normalize errors for grouping)
        Map<String, Integer> tempErrorFreq = new HashMap<>();
        for (GameLogMetrics m : allMetrics) {
            for (String error : m.getErrors()) {
                String normalized = normalizeError(error);
                tempErrorFreq.merge(normalized, 1, Integer::sum);
            }
        }
        // Sort by frequency descending, limit to top 20
        errorFrequency = tempErrorFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(20)
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

        // Consecutive failures tracking
        int currentStreak = 0;
        maxConsecutiveFailures = 0;
        for (GameLogMetrics m : sorted) {
            if (!m.isSuccessful()) {
                currentStreak++;
                maxConsecutiveFailures = Math.max(maxConsecutiveFailures, currentStreak);
            } else {
                currentStreak = 0;
            }
        }

        // First half vs second half success rates
        int midpoint = sorted.size() / 2;
        if (midpoint > 0) {
            long firstHalfSuccess = sorted.subList(0, midpoint).stream()
                    .filter(GameLogMetrics::isSuccessful).count();
            long secondHalfSuccess = sorted.subList(midpoint, sorted.size()).stream()
                    .filter(GameLogMetrics::isSuccessful).count();
            firstHalfSuccessRate = 100.0 * firstHalfSuccess / midpoint;
            secondHalfSuccessRate = 100.0 * secondHalfSuccess / (sorted.size() - midpoint);
        }

        // Warnings leading to failure
        warningsLeadingToFailure = (int) allMetrics.stream()
                .filter(m -> !m.getWarnings().isEmpty() && !m.isSuccessful())
                .count();

        // Turn distribution histogram
        for (GameLogMetrics m : allMetrics) {
            int turns = m.getTurnCount();
            if (turns <= 5) turnHistogram[0]++;
            else if (turns <= 10) turnHistogram[1]++;
            else if (turns <= 20) turnHistogram[2]++;
            else if (turns <= 30) turnHistogram[3]++;
            else turnHistogram[4]++;
        }
    }

    /**
     * Normalize error messages for grouping by removing timestamps, IDs, and other variable data.
     */
    private String normalizeError(String error) {
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

    // Getters

    public int getTotalGames() {
        return totalGames;
    }

    public int getSuccessfulGames() {
        return successfulGames;
    }

    public int getFailedGames() {
        return failedGames;
    }

    public double getSuccessRate() {
        if (totalGames == 0) return 0.0;
        return 100.0 * successfulGames / totalGames;
    }

    public int getGamesWithErrors() {
        return gamesWithErrors;
    }

    public int getGamesWithWarnings() {
        return gamesWithWarnings;
    }

    public int getGamesWithChecksumMismatches() {
        return gamesWithChecksumMismatches;
    }

    public long getTotalApproximateBytes() {
        return totalApproximateBytes;
    }

    public long getTotalDeltaBytes() {
        return totalDeltaBytes;
    }

    public long getTotalFullStateBytes() {
        return totalFullStateBytes;
    }

    public double getAverageBandwidthSavings() {
        return averageBandwidthSavings;
    }

    public double getAverageTurns() {
        return averageTurns;
    }

    public int getTotalTurns() {
        return totalTurns;
    }

    public Set<String> getUniqueDeckNames() {
        return uniqueDeckNames;
    }

    public int getUniqueDeckCount() {
        return uniqueDeckNames != null ? uniqueDeckNames.size() : 0;
    }

    /**
     * Format bytes in human-readable form (B, KB, MB, GB).
     */
    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public Map<Integer, PlayerCountStats> getStatsByPlayerCount() {
        return statsByPlayerCount;
    }

    public List<GameLogMetrics> getAllMetrics() {
        return allMetrics;
    }

    public Map<GameLogMetrics.FailureMode, Integer> getFailureModeCounts() {
        return failureModeCounts;
    }

    public Map<String, Integer> getErrorFrequency() {
        return errorFrequency;
    }

    public Map<Integer, BatchStats> getBatchStats() {
        return batchStats;
    }

    public int getMaxConsecutiveFailures() {
        return maxConsecutiveFailures;
    }

    public double getFirstHalfSuccessRate() {
        return firstHalfSuccessRate;
    }

    public double getSecondHalfSuccessRate() {
        return secondHalfSuccessRate;
    }

    public int getWarningsLeadingToFailure() {
        return warningsLeadingToFailure;
    }

    public int[] getTurnHistogram() {
        return turnHistogram;
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
     * Get files that contain a specific error (normalized for grouping).
     * @param normalizedError the normalized error string
     * @return list of log file names containing this error
     */
    public List<String> getFilesWithError(String normalizedError) {
        return allMetrics.stream()
                .filter(m -> m.getErrors().stream()
                        .anyMatch(e -> normalizeError(e).equals(normalizedError)))
                .map(GameLogMetrics::getLogFileName)
                .limit(5)
                .collect(Collectors.toList());
    }

    /**
     * Get files that contain a specific warning (normalized for grouping).
     * @param normalizedWarning the normalized warning string
     * @return list of log file names containing this warning
     */
    public List<String> getFilesWithWarning(String normalizedWarning) {
        return allMetrics.stream()
                .filter(m -> m.getWarnings().stream()
                        .anyMatch(w -> normalizeError(w).equals(normalizedWarning)))
                .map(GameLogMetrics::getLogFileName)
                .limit(5)
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
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();

        sb.append("_Generated by forge.net.analysis.AnalysisResult (NetworkLogAnalyzer)_\n\n");
        sb.append("## Comprehensive Delta Sync Validation Results\n\n");
        sb.append(String.format("**Analysis Date:** %s\n\n",
                analysisTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        // Summary Table
        sb.append("### Summary Results\n\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        sb.append(String.format("| Total Games Run | %d |\n", totalGames));
        sb.append(String.format("| Successful Games | %d |\n", successfulGames));
        sb.append(String.format("| Failed Games | %d |\n", failedGames));
        sb.append(String.format("| Overall Success Rate | %.1f%% |\n", getSuccessRate()));
        sb.append(String.format("| Total Turns | %d |\n", totalTurns));
        sb.append(String.format("| Average Turns per Game | %.1f |\n", averageTurns));
        sb.append(String.format("| Unique Decks Used | %d |\n", getUniqueDeckCount()));
        sb.append(String.format("| Average Bandwidth Savings | %.1f%% |\n", averageBandwidthSavings));
        sb.append(String.format("| Total Network Traffic | %s |\n", formatBytes(totalDeltaBytes)));
        sb.append(String.format("| Checksum Mismatches | %d |\n", gamesWithChecksumMismatches));
        sb.append(String.format("| Games with Errors | %d |\n", gamesWithErrors));
        sb.append(String.format("| Games with Warnings | %d |\n", gamesWithWarnings));
        sb.append("\n");

        // Bandwidth Usage Breakdown
        sb.append("### Bandwidth Usage Breakdown\n\n");
        sb.append("| Metric | Total | Avg per Game | Description |\n");
        sb.append("|--------|-------|--------------|-------------|\n");
        long avgApprox = totalGames > 0 ? totalApproximateBytes / totalGames : 0;
        long avgDelta = totalGames > 0 ? totalDeltaBytes / totalGames : 0;
        long avgFull = totalGames > 0 ? totalFullStateBytes / totalGames : 0;
        sb.append(String.format("| Approximate | %s | %s | Estimated delta size from object diffs |\n",
                formatBytes(totalApproximateBytes), formatBytes(avgApprox)));
        sb.append(String.format("| ActualNetwork | %s | %s | Actual bytes sent over network |\n",
                formatBytes(totalDeltaBytes), formatBytes(avgDelta)));
        sb.append(String.format("| FullState | %s | %s | Size if full state was sent |\n",
                formatBytes(totalFullStateBytes), formatBytes(avgFull)));
        sb.append("\n");

        // Bandwidth savings calculations
        double approxVsFull = totalFullStateBytes > 0 ? 100.0 * (1.0 - (double) totalApproximateBytes / totalFullStateBytes) : 0;
        double actualVsFull = totalFullStateBytes > 0 ? 100.0 * (1.0 - (double) totalDeltaBytes / totalFullStateBytes) : 0;
        double actualVsApprox = totalApproximateBytes > 0 ? 100.0 * (1.0 - (double) totalDeltaBytes / totalApproximateBytes) : 0;
        sb.append("**Bandwidth Savings:**\n");
        sb.append(String.format("- Approximate vs FullState: %.1f%% savings\n", approxVsFull));
        sb.append(String.format("- ActualNetwork vs FullState: %.1f%% savings\n", actualVsFull));
        sb.append(String.format("- ActualNetwork vs Approximate: %.1f%% %s (compression effect)\n",
                Math.abs(actualVsApprox), actualVsApprox >= 0 ? "savings" : "overhead"));
        sb.append("\n");

        // Results by Player Count
        if (!statsByPlayerCount.isEmpty()) {
            sb.append("### Results by Player Count\n\n");
            sb.append("| Players | Games | Success Rate | Avg Turns | Avg Savings |\n");
            sb.append("|---------|-------|--------------|-----------|-------------|\n");
            for (int p = 2; p <= 4; p++) {
                PlayerCountStats stats = statsByPlayerCount.get(p);
                if (stats != null) {
                    sb.append(String.format("| %d | %d | %.1f%% | %.1f | %.1f%% |\n",
                            p, stats.gameCount, stats.successRate, stats.averageTurns,
                            stats.averageBandwidthSavings));
                }
            }
            sb.append("\n");

            // Bandwidth by player count
            sb.append("### Bandwidth by Player Count\n\n");
            sb.append("| Players | Games | ActualNetwork | Avg Actual | FullState | Avg FullState | Savings |\n");
            sb.append("|---------|-------|---------------|------------|-----------|---------------|--------|\n");
            for (int p = 2; p <= 4; p++) {
                PlayerCountStats stats = statsByPlayerCount.get(p);
                if (stats != null) {
                    long avgActualPerGame = stats.gameCount > 0 ? stats.totalDeltaBytes / stats.gameCount : 0;
                    long avgFullPerGame = stats.gameCount > 0 ? stats.totalFullStateBytes / stats.gameCount : 0;
                    sb.append(String.format("| %d | %d | %s | %s | %s | %s | %.1f%% |\n",
                            p, stats.gameCount,
                            formatBytes(stats.totalDeltaBytes), formatBytes(avgActualPerGame),
                            formatBytes(stats.totalFullStateBytes), formatBytes(avgFullPerGame),
                            stats.averageBandwidthSavings));
                }
            }
            sb.append("\n");
        }

        // Error Analysis
        if (gamesWithErrors > 0 || gamesWithChecksumMismatches > 0) {
            sb.append("### Error Analysis\n\n");

            if (gamesWithChecksumMismatches > 0) {
                sb.append(String.format("**Checksum Mismatches:** %d games had desync issues\n\n",
                        gamesWithChecksumMismatches));
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

            List<String> errors = getAllErrors();
            if (!errors.isEmpty()) {
                sb.append("**Unique Errors:**\n");
                for (String error : errors.subList(0, Math.min(10, errors.size()))) {
                    sb.append(String.format("- `%s`\n", error));
                }
                if (errors.size() > 10) {
                    sb.append(String.format("- ... and %d more\n", errors.size() - 10));
                }
                sb.append("\n");
            }

            // Error Context section for failed games
            List<GameLogMetrics> failedGames = allMetrics.stream()
                    .filter(m -> !m.isSuccessful() && m.getErrorContext() != null)
                    .sorted(Comparator.comparingInt(GameLogMetrics::getGameIndex))
                    .limit(5) // Limit to 5 most detailed error contexts
                    .collect(Collectors.toList());

            if (!failedGames.isEmpty()) {
                sb.append("### Error Context for Failed Games\n\n");
                for (GameLogMetrics m : failedGames) {
                    NetworkLogAnalyzer.ErrorContext ctx = m.getErrorContext();
                    if (ctx != null) {
                        sb.append(ctx.toMarkdown());
                        sb.append("\n");
                    }
                }
            }
        }

        // Win Rate Distribution
        {
            // Overall wins per player
            Map<String, Integer> overallWins = new LinkedHashMap<>();
            for (GameLogMetrics m : allMetrics) {
                if (m.getWinner() != null && !m.getWinner().isEmpty()) {
                    overallWins.merge(m.getWinner(), 1, Integer::sum);
                }
            }

            if (!overallWins.isEmpty()) {
                sb.append("### Win Rate Distribution\n\n");

                // Sort by wins descending
                List<Map.Entry<String, Integer>> sortedOverall = overallWins.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .collect(Collectors.toList());

                sb.append("**Overall**\n\n");
                sb.append("| Player | Wins | Total | Win % |\n");
                sb.append("|--------|------|-------|-------|\n");
                for (Map.Entry<String, Integer> e : sortedOverall) {
                    int pct = totalGames > 0 ? (e.getValue() * 100) / totalGames : 0;
                    sb.append(String.format("| %s | %d | %d | %d%% |\n",
                            e.getKey(), e.getValue(), totalGames, pct));
                }
                sb.append("\n");

                // Wins by player count
                Map<Integer, Map<String, Integer>> winsByPlayerCount = new LinkedHashMap<>();
                Map<Integer, Integer> gamesByPlayerCount = new LinkedHashMap<>();
                for (GameLogMetrics m : allMetrics) {
                    int pc = m.getPlayerCount();
                    gamesByPlayerCount.merge(pc, 1, Integer::sum);
                    if (m.getWinner() != null && !m.getWinner().isEmpty()) {
                        winsByPlayerCount.computeIfAbsent(pc, k -> new LinkedHashMap<>())
                                .merge(m.getWinner(), 1, Integer::sum);
                    }
                }

                sb.append("**By Player Count**\n\n");
                sb.append("| Players | Player | Wins | Games | Win % | Expected |\n");
                sb.append("|---------|--------|------|-------|-------|----------|\n");
                for (int pc : winsByPlayerCount.keySet().stream().sorted().collect(Collectors.toList())) {
                    int gamesForPc = gamesByPlayerCount.getOrDefault(pc, 0);
                    String expected = String.format("~%d%%", gamesForPc > 0 ? 100 / pc : 0);
                    Map<String, Integer> wins = winsByPlayerCount.get(pc);
                    List<Map.Entry<String, Integer>> sorted = wins.entrySet().stream()
                            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                            .collect(Collectors.toList());
                    for (Map.Entry<String, Integer> e : sorted) {
                        int pct = gamesForPc > 0 ? (e.getValue() * 100) / gamesForPc : 0;
                        sb.append(String.format("| %dp | %s | %d | %d | %d%% | %s |\n",
                                pc, e.getKey(), e.getValue(), gamesForPc, pct, expected));
                    }
                }
                sb.append("\n");
            }
        }

        // Warning Analysis
        if (gamesWithWarnings > 0) {
            sb.append("### Warning Analysis\n\n");
            sb.append(String.format("**Games with Warnings:** %d\n\n", gamesWithWarnings));

            // List files with warnings for quick reference
            List<String> filesWithWarnings = getFilesWithAnyWarnings();
            if (!filesWithWarnings.isEmpty()) {
                sb.append("**Files with Warnings:**\n");
                for (String file : filesWithWarnings) {
                    sb.append(String.format("- `%s`\n", file));
                }
                sb.append("\n");
            }

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

        // Turn Distribution Section
        sb.append("### Turn Distribution\n\n");
        sb.append("| Range | Count | % | Notes |\n");
        sb.append("|-------|-------|---|-------|\n");
        String[] turnRanges = {"1-5", "6-10", "11-20", "21-30", "30+"};
        for (int i = 0; i < turnHistogram.length; i++) {
            double pct = totalGames > 0 ? 100.0 * turnHistogram[i] / totalGames : 0;
            String notes = "";
            if (i == 0 && pct > 5.0) {
                notes = "Early termination";
            }
            sb.append(String.format("| %s | %d | %.1f%% | %s |\n",
                    turnRanges[i], turnHistogram[i], pct, notes));
        }
        sb.append("\n");

        // Failure Mode Analysis Section
        if (failedGames > 0 && failureModeCounts != null && !failureModeCounts.isEmpty()) {
            sb.append("### Failure Mode Analysis\n\n");
            sb.append("| Mode | Count | % | Affected Games |\n");
            sb.append("|------|-------|---|----------------|\n");
            for (GameLogMetrics.FailureMode mode : GameLogMetrics.FailureMode.values()) {
                if (mode == GameLogMetrics.FailureMode.NONE) continue;
                int count = failureModeCounts.getOrDefault(mode, 0);
                if (count > 0) {
                    double pct = totalGames > 0 ? 100.0 * count / totalGames : 0;
                    // List affected games
                    String affected = allMetrics.stream()
                            .filter(m -> m.getFailureMode() == mode)
                            .map(m -> "game" + m.getGameIndex())
                            .limit(5)
                            .collect(Collectors.joining(", "));
                    if (count > 5) {
                        affected += ", ...";
                    }
                    sb.append(String.format("| %s | %d | %.1f%% | %s |\n",
                            mode.name(), count, pct, affected));
                }
            }
            sb.append("\n");
        }

        // Batch Performance Section
        if (batchStats != null && !batchStats.isEmpty()) {
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

        // Top Errors by Frequency Section
        if (errorFrequency != null && !errorFrequency.isEmpty()) {
            sb.append("### Top Errors (by frequency)\n\n");
            sb.append("| Error Pattern | Count |\n");
            sb.append("|---------------|-------|\n");
            int shown = 0;
            for (Map.Entry<String, Integer> entry : errorFrequency.entrySet()) {
                if (shown++ >= 10) break;
                String errorTruncated = entry.getKey().length() > 80 ?
                        entry.getKey().substring(0, 77) + "..." : entry.getKey();
                sb.append(String.format("| `%s` | %d |\n", errorTruncated, entry.getValue()));
            }
            sb.append("\n");
        }

        // Failure Patterns Section
        sb.append("### Failure Patterns\n\n");
        sb.append("| Pattern | Value | Status |\n");
        sb.append("|---------|-------|--------|\n");
        String consecutiveStatus = maxConsecutiveFailures > 2 ? "Concerning" : "OK";
        sb.append(String.format("| Max Consecutive Failures | %d | %s |\n",
                maxConsecutiveFailures, consecutiveStatus));
        sb.append(String.format("| First Half Success (0-%d) | %.1f%% | |\n",
                (totalGames / 2) - 1, firstHalfSuccessRate));
        double dropRate = firstHalfSuccessRate - secondHalfSuccessRate;
        String dropStatus = dropRate > 5.0 ? "Degrading" : "";
        sb.append(String.format("| Second Half Success (%d-%d) | %.1f%% | %s |\n",
                totalGames / 2, totalGames - 1, secondHalfSuccessRate, dropStatus));
        int gamesWithWarningsAndFailed = warningsLeadingToFailure;
        double warnFailPct = gamesWithWarnings > 0 ?
                100.0 * gamesWithWarningsAndFailed / gamesWithWarnings : 0;
        sb.append(String.format("| Warnings -> Failures | %d/%d (%.1f%%) | |\n",
                gamesWithWarningsAndFailed, gamesWithWarnings, warnFailPct));
        sb.append("\n");

        // Stability Trend
        String trend = "STABLE";
        if (maxConsecutiveFailures > 2 || dropRate > 5.0) {
            trend = "DEGRADING";
        }
        sb.append(String.format("**Stability Trend:** %s\n\n", trend));

        // Validation Summary
        sb.append("### Validation Status\n\n");
        boolean passed = getSuccessRate() >= 90.0 &&
                averageBandwidthSavings >= 90.0 &&
                gamesWithChecksumMismatches == 0;

        if (passed) {
            sb.append("**PASSED** - All validation criteria met:\n");
        } else {
            sb.append("**FAILED** - Validation criteria not met:\n");
        }
        sb.append(String.format("- [%s] Success rate >= 90%% (actual: %.1f%%)\n",
                getSuccessRate() >= 90.0 ? "x" : " ", getSuccessRate()));
        sb.append(String.format("- [%s] Average bandwidth savings >= 90%% (actual: %.1f%%)\n",
                averageBandwidthSavings >= 90.0 ? "x" : " ", averageBandwidthSavings));
        sb.append(String.format("- [%s] Zero checksum mismatches (actual: %d)\n",
                gamesWithChecksumMismatches == 0 ? "x" : " ", gamesWithChecksumMismatches));
        sb.append("\n");

        // List of analyzed log files
        sb.append("### Analyzed Log Files\n\n");
        sb.append(String.format("**Total files analyzed:** %d\n\n", totalGames));
        if (!allMetrics.isEmpty()) {
            // Sort by game index for consistent ordering
            List<GameLogMetrics> sortedMetrics = allMetrics.stream()
                    .sorted((a, b) -> Integer.compare(a.getGameIndex(), b.getGameIndex()))
                    .collect(Collectors.toList());

            sb.append("| # | Log File | Status | Players | Turns | Winner |\n");
            sb.append("|---|----------|--------|---------|-------|--------|\n");
            for (GameLogMetrics m : sortedMetrics) {
                String status = m.isSuccessful() ? "OK" : (m.hasChecksumMismatch() ? "DESYNC" : "FAIL");
                String winner = m.getWinner() != null ? m.getWinner() : "-";
                sb.append(String.format("| %d | `%s` | %s | %d | %d | %s |\n",
                        m.getGameIndex(),
                        m.getLogFileName(),
                        status,
                        m.getPlayerCount(),
                        m.getTurnCount(),
                        winner));
            }
        }

        return sb.toString();
    }

    /**
     * Generate a plain text summary.
     */
    public String toSummary() {
        return String.format(
                "AnalysisResult[total=%d, success=%d (%.1f%%), failed=%d, " +
                "errors=%d, checksumMismatches=%d, avgSavings=%.1f%%, avgTurns=%.1f]",
                totalGames, successfulGames, getSuccessRate(), failedGames,
                gamesWithErrors, gamesWithChecksumMismatches, averageBandwidthSavings, averageTurns);
    }

    /**
     * Check if validation criteria are met.
     */
    public boolean passesValidation() {
        return getSuccessRate() >= 90.0 &&
                averageBandwidthSavings >= 90.0 &&
                gamesWithChecksumMismatches == 0;
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
        public final long totalApproximateBytes;
        public final long totalDeltaBytes;
        public final long totalFullStateBytes;

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

            this.totalApproximateBytes = metrics.stream().mapToLong(GameLogMetrics::getTotalApproximateBytes).sum();
            this.totalDeltaBytes = metrics.stream().mapToLong(GameLogMetrics::getTotalDeltaBytes).sum();
            this.totalFullStateBytes = metrics.stream().mapToLong(GameLogMetrics::getTotalFullStateBytes).sum();

            this.averageBandwidthSavings = totalFullStateBytes > 0
                    ? 100.0 * (1.0 - (double) totalDeltaBytes / totalFullStateBytes)
                    : 0.0;
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
}
