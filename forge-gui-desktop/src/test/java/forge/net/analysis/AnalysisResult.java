package forge.net.analysis;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private long totalDeltaBytes;
    private long totalFullStateBytes;
    private double averageBandwidthSavings;
    private double averageTurns;

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

        totalDeltaBytes = allMetrics.stream().mapToLong(GameLogMetrics::getTotalDeltaBytes).sum();
        totalFullStateBytes = allMetrics.stream().mapToLong(GameLogMetrics::getTotalFullStateBytes).sum();

        if (totalFullStateBytes > 0) {
            averageBandwidthSavings = 100.0 * (1.0 - (double) totalDeltaBytes / totalFullStateBytes);
        }

        averageTurns = allMetrics.stream()
                .filter(GameLogMetrics::isGameCompleted)
                .mapToInt(GameLogMetrics::getTurnCount)
                .average()
                .orElse(0.0);

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
        sb.append(String.format("| Average Bandwidth Savings | %.1f%% |\n", averageBandwidthSavings));
        sb.append(String.format("| Checksum Mismatches | %d |\n", gamesWithChecksumMismatches));
        sb.append(String.format("| Games with Errors | %d |\n", gamesWithErrors));
        sb.append(String.format("| Games with Warnings | %d |\n", gamesWithWarnings));
        sb.append(String.format("| Average Turns per Game | %.1f |\n", averageTurns));
        sb.append(String.format("| Total Delta Bytes | %,d |\n", totalDeltaBytes));
        sb.append(String.format("| Total Full State Bytes | %,d |\n", totalFullStateBytes));
        sb.append("\n");

        // Results by Player Count
        if (!statsByPlayerCount.isEmpty()) {
            sb.append("### Results by Player Count\n\n");
            sb.append("| Players | Games | Success Rate | Avg Turns | Avg Savings | Total Bytes |\n");
            sb.append("|---------|-------|--------------|-----------|-------------|-------------|\n");
            for (int p = 2; p <= 4; p++) {
                PlayerCountStats stats = statsByPlayerCount.get(p);
                if (stats != null) {
                    sb.append(String.format("| %d | %d | %.1f%% | %.1f | %.1f%% | %,d |\n",
                            p, stats.gameCount, stats.successRate, stats.averageTurns,
                            stats.averageBandwidthSavings, stats.totalDeltaBytes));
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

            this.totalDeltaBytes = metrics.stream().mapToLong(GameLogMetrics::getTotalDeltaBytes).sum();
            this.totalFullStateBytes = metrics.stream().mapToLong(GameLogMetrics::getTotalFullStateBytes).sum();

            this.averageBandwidthSavings = totalFullStateBytes > 0
                    ? 100.0 * (1.0 - (double) totalDeltaBytes / totalFullStateBytes)
                    : 0.0;
        }
    }
}
