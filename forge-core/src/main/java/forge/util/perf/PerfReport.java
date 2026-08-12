/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.util.perf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregates decision records into the latency and work distributions the plan asks every
 * measurement to report: median, p95 and p99 per decision kind, plus the counters needed to
 * normalise them.
 *
 * <p>Records are folded in as they finish and then dropped, so a long batch costs one {@code long}
 * per decision rather than a retained object graph. Percentiles use nearest-rank on the collected
 * samples; no bucketing or estimation is involved, so a run's numbers can be recomputed from the
 * raw samples if those are dumped.</p>
 *
 * <p>A report is safe to register as a sink for a whole run and read afterwards. It is not intended
 * to be read while decisions are still arriving.</p>
 */
public final class PerfReport implements PerfSink {
    /** Per-decision-kind samples and work totals. */
    private static final class KindStats {
        private final PerfAccumulator totals = new PerfAccumulator();
        private long[] durations = new long[256];
        private int count;

        void add(final DecisionRecord record) {
            final long duration = record.getDurationNanos();
            if (duration >= 0L) {
                if (count == durations.length) {
                    durations = Arrays.copyOf(durations, durations.length * 2);
                }
                durations[count++] = duration;
            }
            for (final PerfCounter counter : PerfCounter.values()) {
                final long value = record.get(counter);
                if (value != 0L) {
                    totals.add(counter, value);
                }
            }
            for (final PerfTimer timer : PerfTimer.values()) {
                final long spans = record.getSpanCount(timer);
                if (spans != 0L) {
                    // Preserve both the accumulated time and the span count of the source record.
                    totals.addSpans(timer, record.getNanos(timer), spans);
                }
            }
        }

        long[] sortedDurations() {
            final long[] copy = Arrays.copyOf(durations, count);
            Arrays.sort(copy);
            return copy;
        }
    }

    private final String label;
    private final Map<DecisionKind, KindStats> byKind = new EnumMap<>(DecisionKind.class);
    private final Map<String, String> metadata = new LinkedHashMap<>();
    private long wallClockNanos = -1L;

    public PerfReport(final String label) {
        this.label = label == null ? "" : label;
    }

    /** Adds a free-form key/value pair to the report header (JVM, decks, seed, commit, ...). */
    public PerfReport withMetadata(final String key, final String value) {
        metadata.put(key, value);
        return this;
    }

    /** Records the wall time of the whole measured run, for throughput figures. */
    public void setWallClockNanos(final long nanos) {
        wallClockNanos = nanos;
    }

    @Override
    public void onDecision(final DecisionRecord record) {
        synchronized (byKind) {
            byKind.computeIfAbsent(record.getKind(), k -> new KindStats()).add(record);
        }
    }

    /** Number of decisions of the given kind folded into this report. */
    public int getDecisionCount(final DecisionKind kind) {
        synchronized (byKind) {
            final KindStats stats = byKind.get(kind);
            return stats == null ? 0 : stats.count;
        }
    }

    /** Total decisions of every kind. */
    public int getDecisionCount() {
        synchronized (byKind) {
            int total = 0;
            for (final KindStats stats : byKind.values()) {
                total += stats.count;
            }
            return total;
        }
    }

    /** Work totals accumulated for one decision kind, or null when that kind never occurred. */
    public PerfAccumulator getTotals(final DecisionKind kind) {
        synchronized (byKind) {
            final KindStats stats = byKind.get(kind);
            return stats == null ? null : stats.totals;
        }
    }

    /**
     * Nearest-rank percentile of decision duration in nanoseconds, or -1 when the kind has no
     * samples. {@code percentile} is in [0, 100].
     */
    public long getDurationPercentileNanos(final DecisionKind kind, final double percentile) {
        final long[] sorted;
        synchronized (byKind) {
            final KindStats stats = byKind.get(kind);
            if (stats == null || stats.count == 0) {
                return -1L;
            }
            sorted = stats.sortedDurations();
        }
        return percentileOf(sorted, percentile);
    }

    private static long percentileOf(final long[] sorted, final double percentile) {
        if (sorted.length == 0) {
            return -1L;
        }
        final double clamped = Math.max(0.0d, Math.min(100.0d, percentile));
        int rank = (int) Math.ceil(clamped / 100.0d * sorted.length);
        if (rank < 1) {
            rank = 1;
        }
        if (rank > sorted.length) {
            rank = sorted.length;
        }
        return sorted[rank - 1];
    }

    // ------------------------------------------------------------------ output

    /** The whole report as JSON. Field names are stable and safe to diff between runs. */
    public String toJson() {
        final StringBuilder sb = new StringBuilder(4096);
        sb.append("{\n  \"label\": ").append(PerfJson.quote(label));
        sb.append(",\n  \"wallClockMs\": ").append(wallClockNanos < 0L ? -1L : wallClockNanos / 1_000_000L);
        sb.append(",\n  \"metadata\": {");
        boolean first = true;
        for (final Map.Entry<String, String> entry : metadata.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append("\n    ").append(PerfJson.quote(entry.getKey())).append(": ")
                    .append(PerfJson.quote(entry.getValue()));
        }
        sb.append(metadata.isEmpty() ? "}" : "\n  }");

        sb.append(",\n  \"decisions\": [");
        final List<DecisionKind> kinds = new ArrayList<>();
        synchronized (byKind) {
            for (final DecisionKind kind : DecisionKind.values()) {
                if (byKind.containsKey(kind)) {
                    kinds.add(kind);
                }
            }
        }
        for (int i = 0; i < kinds.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            appendKind(sb, kinds.get(i));
        }
        sb.append(kinds.isEmpty() ? "]" : "\n  ]");
        sb.append("\n}\n");
        return sb.toString();
    }

    private void appendKind(final StringBuilder sb, final DecisionKind kind) {
        final KindStats stats;
        final long[] sorted;
        synchronized (byKind) {
            stats = byKind.get(kind);
            sorted = stats.sortedDurations();
        }
        sb.append("\n    {\n      \"kind\": ").append(PerfJson.quote(kind.jsonName()));
        sb.append(",\n      \"count\": ").append(sorted.length);
        sb.append(",\n      \"latencyNanos\": {");
        sb.append("\n        \"min\": ").append(percentileOf(sorted, 0.0d));
        sb.append(",\n        \"median\": ").append(percentileOf(sorted, 50.0d));
        sb.append(",\n        \"p95\": ").append(percentileOf(sorted, 95.0d));
        sb.append(",\n        \"p99\": ").append(percentileOf(sorted, 99.0d));
        sb.append(",\n        \"max\": ").append(percentileOf(sorted, 100.0d));
        sb.append(",\n        \"total\": ").append(sum(sorted));
        sb.append("\n      }");

        sb.append(",\n      \"counters\": {");
        boolean firstCounter = true;
        for (final PerfCounter counter : PerfCounter.values()) {
            final long value = stats.totals.get(counter);
            if (value == 0L) {
                continue;
            }
            if (!firstCounter) {
                sb.append(',');
            }
            firstCounter = false;
            sb.append("\n        ").append(PerfJson.quote(counter.jsonName())).append(": ").append(value);
        }
        sb.append(firstCounter ? "}" : "\n      }");

        sb.append(",\n      \"timerNanos\": {");
        boolean firstTimer = true;
        for (final PerfTimer timer : PerfTimer.values()) {
            final long spans = stats.totals.getSpanCount(timer);
            if (spans == 0L) {
                continue;
            }
            if (!firstTimer) {
                sb.append(',');
            }
            firstTimer = false;
            sb.append("\n        ").append(PerfJson.quote(timer.jsonName())).append(": {\"totalNanos\": ")
                    .append(stats.totals.getNanos(timer)).append(", \"spans\": ").append(spans).append('}');
        }
        sb.append(firstTimer ? "}" : "\n      }");
        sb.append("\n    }");
    }

    private static long sum(final long[] values) {
        long total = 0L;
        for (final long value : values) {
            total += value;
        }
        return total;
    }

    /** Writes {@link #toJson()} to {@code file}, creating parent directories as needed. */
    public void writeJson(final File file) throws IOException {
        final File parent = file.getParentFile();
        if (parent != null) {
            Files.createDirectories(parent.toPath());
        }
        try (Writer writer = new BufferedWriter(
                new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8))) {
            writer.write(toJson());
        }
    }

    /** A short human-readable summary, one line per decision kind. */
    public String summary() {
        final StringBuilder sb = new StringBuilder();
        sb.append(label.isEmpty() ? "perf" : label).append(':');
        synchronized (byKind) {
            if (byKind.isEmpty()) {
                sb.append(" no decisions recorded");
                return sb.toString();
            }
        }
        for (final DecisionKind kind : DecisionKind.values()) {
            final int count = getDecisionCount(kind);
            if (count == 0) {
                continue;
            }
            sb.append(String.format("%n  %-8s n=%-6d median=%.3fms p95=%.3fms p99=%.3fms max=%.3fms",
                    kind.jsonName(), count,
                    getDurationPercentileNanos(kind, 50.0d) / 1e6d,
                    getDurationPercentileNanos(kind, 95.0d) / 1e6d,
                    getDurationPercentileNanos(kind, 99.0d) / 1e6d,
                    getDurationPercentileNanos(kind, 100.0d) / 1e6d));
        }
        return sb.toString();
    }
}
