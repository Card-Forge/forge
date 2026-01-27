package forge.net.analysis;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
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
     * Generate a markdown report.
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();

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
            sb.append("| Players | Approximate | ActualNetwork | FullState | Savings |\n");
            sb.append("|---------|-------------|---------------|-----------|--------|\n");
            for (int p = 2; p <= 4; p++) {
                PlayerCountStats stats = statsByPlayerCount.get(p);
                if (stats != null) {
                    sb.append(String.format("| %d | %s | %s | %s | %.1f%% |\n",
                            p, formatBytes(stats.totalApproximateBytes), formatBytes(stats.totalDeltaBytes),
                            formatBytes(stats.totalFullStateBytes), stats.averageBandwidthSavings));
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
}
