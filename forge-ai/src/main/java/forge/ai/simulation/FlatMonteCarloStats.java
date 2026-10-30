package forge.ai.simulation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Process-wide statistics of flat Monte Carlo decisions, for profiling and for tests. Nothing here influences a
 * decision; it only counts what {@link FlatMonteCarloPicker} did: how many decisions ran, why each ended, how many
 * rollouts and how much time they took (with percentiles), how often lines were pruned, and which cards could not be
 * simulated or made a rollout fail. The last two are what to look at when checking that the AI copes with a deck.
 * <p>
 * All methods are thread safe. The sample of decision times is capped, so a very long run cannot exhaust memory.
 */
public final class FlatMonteCarloStats {
    /** Decision times kept for percentiles; beyond this only the counters keep growing. */
    private static final int MAX_LATENCY_SAMPLES = 200_000;
    /** Distinct card names / failure messages tracked, so that a flood of different messages stays bounded. */
    private static final int MAX_KEYS = 500;

    private static final Object LOCK = new Object();
    private static final Map<String, int[]> BY_REASON = new TreeMap<>();
    private static final Map<String, int[]> UNSIMULATABLE = new TreeMap<>();
    private static final Map<String, int[]> FAILURES = new TreeMap<>();
    private static final Map<String, int[]> REJECTED = new TreeMap<>();
    /** Sorcery-timing decision points seen, by how many plays the heuristics approved (0, 1, 2, 3+) and budget cut-offs. */
    private static final long[] POINTS_BY_APPROVED = new long[4];
    private static long pointsOutOfBudget;
    private static long targetVariantCandidates;
    private static long[] latenciesNanos = new long[1024];
    private static int latencyCount;
    private static long decisions;
    private static long decidedByRollouts;
    private static long rollouts;
    private static long prunedLines;
    private static long totalNanos;
    private static long maxNanos;

    private FlatMonteCarloStats() {
    }

    /** Forgets everything, e.g. between the arms of a benchmark. */
    public static void reset() {
        synchronized (LOCK) {
            BY_REASON.clear();
            UNSIMULATABLE.clear();
            FAILURES.clear();
            SLOW.clear();
            PLANS.clear();
            REJECTED.clear();
            java.util.Arrays.fill(POINTS_BY_APPROVED, 0);
            pointsOutOfBudget = 0;
            targetVariantCandidates = 0;
            extraCandidatesTotal = 0;
            REACTIVE.clear();
            landDecisions = 0;
            landChanges = 0;
            latencyCount = 0;
            decisions = 0;
            decidedByRollouts = 0;
            rollouts = 0;
            prunedLines = 0;
            totalNanos = 0;
            maxNanos = 0;
        }
    }

    /** Called by the AI once per Monte Carlo decision. */
    public static void record(final FlatMonteCarloPicker.Result result) {
        if (result == null) {
            return;
        }
        synchronized (LOCK) {
            decisions++;
            if (result.decidedByRollouts) {
                decidedByRollouts++;
            }
            rollouts += result.rollouts;
            prunedLines += result.pruned;
            totalNanos += result.elapsedNanos;
            maxNanos = Math.max(maxNanos, result.elapsedNanos);
            bump(BY_REASON, result.reason);
            if (latencyCount < MAX_LATENCY_SAMPLES) {
                if (latencyCount == latenciesNanos.length) {
                    latenciesNanos = Arrays.copyOf(latenciesNanos, Math.min(MAX_LATENCY_SAMPLES, latencyCount * 2));
                }
                latenciesNanos[latencyCount++] = result.elapsedNanos;
            }
        }
    }

    /**
     * Called by the AI at every main-phase decision point where Monte Carlo is switched on, before anything is
     * sampled. Together with the decisions this tells how often there was a real choice at all: a decision point
     * with fewer than two approved plays (and nothing to hold) has nothing to compare.
     *
     * @param approved      how many plays the heuristics approved (capped by the candidate limit)
     * @param outOfBudget   the time budget of the turn was already spent, so only one play was looked for
     */
    public static void noteDecisionPoint(final int approved, final boolean outOfBudget) {
        synchronized (LOCK) {
            POINTS_BY_APPROVED[Math.max(0, Math.min(3, approved))]++;
            if (outOfBudget) {
                pointsOutOfBudget++;
            }
        }
    }

    /** Decision points seen with exactly this many approved plays (3 stands for three or more). */
    public static long getDecisionPoints(final int approved) {
        synchronized (LOCK) {
            return POINTS_BY_APPROVED[Math.max(0, Math.min(3, approved))];
        }
    }

    /** How many other-target candidates one decision added to the comparison. */
    public static void noteTargetVariants(final int count) {
        if (count > 0) {
            synchronized (LOCK) {
                targetVariantCandidates += count;
            }
        }
    }

    private static long extraCandidatesTotal;
    private static long landDecisions;
    private static long landChanges;

    /** A land choice made with rollouts, and whether it differed from the heuristics' land. */
    public static void noteLandChoice(final boolean byRollouts, final boolean changed) {
        synchronized (LOCK) {
            landDecisions++;
            if (byRollouts && changed) {
                landChanges++;
            }
        }
    }

    public static long getLandChanges() {
        synchronized (LOCK) {
            return landChanges;
        }
    }
    /** Opponent-turn decisions by mode: {points with something to decide, decisions, changed from the heuristics, passes, rollouts, nanos}. */
    private static final Map<String, long[]> REACTIVE = new TreeMap<>();

    /** An opponent-turn priority where there was at least one candidate; {@code candidates} excludes passing. */
    public static void noteReactivePoint(final FlatMonteCarloPicker.Mode mode, final int candidates) {
        synchronized (LOCK) {
            final long[] row = REACTIVE.computeIfAbsent(mode.name(), k -> new long[7]);
            row[candidates > 0 ? 0 : 6]++;
        }
    }

    /** The result of a decision on the opponent's turn; {@code heuristicChoice} is what the heuristics alone would do. */
    public static void noteReactiveResult(final FlatMonteCarloPicker.Mode mode, final FlatMonteCarloPicker.Result result,
                                          final forge.game.spellability.SpellAbility heuristicChoice) {
        synchronized (LOCK) {
            final long[] row = REACTIVE.computeIfAbsent(mode.name(), k -> new long[7]);
            row[1]++;
            if (result.decidedByRollouts && (result.hold ? heuristicChoice != null : result.choice != heuristicChoice)) {
                row[2]++; // differs from what the heuristics would have done
            }
            if (result.hold) {
                row[3]++;
            }
            row[4] += result.rollouts;
            row[5] += result.elapsedNanos;
        }
    }

    public static long getReactiveDecisions(final FlatMonteCarloPicker.Mode mode) {
        synchronized (LOCK) {
            final long[] row = REACTIVE.get(mode.name());
            return row == null ? 0 : row[1];
        }
    }

    /** How many opponent-turn decisions in this mode ended differently from what the heuristics alone would do. */
    public static long getReactiveChanges(final FlatMonteCarloPicker.Mode mode) {
        synchronized (LOCK) {
            final long[] row = REACTIVE.get(mode.name());
            return row == null ? 0 : row[2];
        }
    }

    /** How many discovered plays one decision added to the comparison. */
    public static void noteExtraCandidates(final int count) {
        if (count > 0) {
            synchronized (LOCK) {
                extraCandidatesTotal += count;
            }
        }
    }

    public static long getExtraCandidates() {
        synchronized (LOCK) {
            return extraCandidatesTotal;
        }
    }

    public static long getTargetVariantCandidates() {
        synchronized (LOCK) {
            return targetVariantCandidates;
        }
    }

    /** The heuristics looked at a play at a Monte Carlo decision point and did not approve it; why (AiPlayDecision). */
    private static final List<String> SLOW = new ArrayList<>();

    /** A decision that took too long: kept (a few of them) with where the time went. */
    public static void noteSlow(final String text) {
        synchronized (LOCK) {
            if (SLOW.size() < 12) {
                SLOW.add(text);
            }
        }
    }

    private static final Map<String, int[]> PLANS = new TreeMap<>();

    /** Which deck plan a player ended up with (counted per game, for the benchmark report). */
    public static void notePlan(final String plan) {
        synchronized (LOCK) {
            PLANS.computeIfAbsent(plan, k -> new int[1])[0]++;
        }
    }

    /** An attack decision: how many were made by rollouts, and how many changed what the heuristics would attack with. */
    public static void noteAttackResult(final FlatMonteCarloPicker.Result result, final boolean changed) {
        synchronized (LOCK) {
            final long[] row = REACTIVE.computeIfAbsent("ATTACK", k -> new long[7]);
            row[0]++;
            row[1]++;
            if (changed) {
                row[2]++;
            }
            row[4] += result.rollouts;
            row[5] += result.elapsedNanos;
        }
    }

    public static void noteRejected(final String decision) {
        synchronized (LOCK) {
            bumpBounded(REJECTED, decision);
        }
    }

    /** A line of play (or the heuristic's own choice) could not be simulated, so it was left out of the comparison. */
    public static void noteUnsimulatable(final String cardName) {
        synchronized (LOCK) {
            bumpBounded(UNSIMULATABLE, cardName);
        }
    }

    /** A rollout or a whole decision ended in an unexpected exception. */
    public static void noteFailure(final String message) {
        synchronized (LOCK) {
            bumpBounded(FAILURES, message == null ? "null" : message.length() > 160 ? message.substring(0, 160) : message);
        }
    }

    private static void bump(final Map<String, int[]> map, final String key) {
        map.computeIfAbsent(key == null ? "null" : key, k -> new int[1])[0]++;
    }

    private static void bumpBounded(final Map<String, int[]> map, final String key) {
        final String k = key == null ? "null" : key;
        if (map.containsKey(k) || map.size() < MAX_KEYS) {
            bump(map, k);
        }
    }

    public static long getDecisions() {
        synchronized (LOCK) {
            return decisions;
        }
    }

    public static long getRollouts() {
        synchronized (LOCK) {
            return rollouts;
        }
    }

    public static long getPrunedLines() {
        synchronized (LOCK) {
            return prunedLines;
        }
    }

    /** How many decisions ended for this reason ("override", "kept-heuristic", "hold", "wins", "too-slow", ...). */
    public static int getReasonCount(final String reason) {
        synchronized (LOCK) {
            final int[] n = BY_REASON.get(reason);
            return n == null ? 0 : n[0];
        }
    }

    /** How many different cards were left out of a comparison because they could not be simulated. */
    public static int getUnsimulatableCardCount() {
        synchronized (LOCK) {
            return UNSIMULATABLE.size();
        }
    }

    /** Total number of rollouts or decisions that ended in an exception. */
    public static int getFailureCount() {
        synchronized (LOCK) {
            int total = 0;
            for (final int[] n : FAILURES.values()) {
                total += n[0];
            }
            return total;
        }
    }

    /** Decision time at the given percentile (0..100), in milliseconds; 0 when nothing was recorded. */
    public static double getPercentileMillis(final double percentile) {
        final long[] copy;
        synchronized (LOCK) {
            if (latencyCount == 0) {
                return 0;
            }
            copy = Arrays.copyOf(latenciesNanos, latencyCount);
        }
        Arrays.sort(copy);
        final int index = (int) Math.min(copy.length - 1, Math.max(0, Math.ceil(percentile / 100.0 * copy.length) - 1));
        return copy[index] / 1_000_000.0;
    }

    /**
     * One machine readable line (key=value pairs after the FLATMC-STATS marker, which the benchmark script parses),
     * followed by human readable lines for the reasons, unsimulatable cards and failures.
     */
    public static String summary() {
        final double p50 = getPercentileMillis(50);
        final double p95 = getPercentileMillis(95);
        final double p99 = getPercentileMillis(99);
        final StringBuilder sb = new StringBuilder();
        synchronized (LOCK) {
            final double avgMs = decisions == 0 ? 0 : TimeUnit.NANOSECONDS.toMicros(totalNanos) / 1000.0 / decisions;
            final double rolloutsPerDecision = decisions == 0 ? 0 : (double) rollouts / decisions;
            final double rolloutsPerSecond = totalNanos == 0 ? 0 : rollouts / (totalNanos / 1e9);
            sb.append(String.format(java.util.Locale.ROOT,
                    "FLATMC-STATS decisions=%d byRollouts=%d rollouts=%d rolloutsPerDecision=%.2f rolloutsPerSec=%.1f"
                            + " pruned=%d avgMs=%.2f p50Ms=%.2f p95Ms=%.2f p99Ms=%.2f maxMs=%.2f failures=%d unsimulatableCards=%d",
                    decisions, decidedByRollouts, rollouts, rolloutsPerDecision, rolloutsPerSecond, prunedLines, avgMs,
                    p50, p95, p99, maxNanos / 1e6, failureTotalLocked(), UNSIMULATABLE.size()));
            sb.append('\n').append(String.format(java.util.Locale.ROOT,
                    "FLATMC-POINTS approved0=%d approved1=%d approved2=%d approved3plus=%d outOfBudget=%d targetVariants=%d extras=%d",
                    POINTS_BY_APPROVED[0], POINTS_BY_APPROVED[1], POINTS_BY_APPROVED[2], POINTS_BY_APPROVED[3], pointsOutOfBudget,
                    targetVariantCandidates, extraCandidatesTotal));
            sb.append('\n').append(String.format(java.util.Locale.ROOT, "FLATMC-LANDS decisions=%d changed=%d", landDecisions, landChanges));
            for (final Map.Entry<String, long[]> e : REACTIVE.entrySet()) {
                final long[] r = e.getValue();
                sb.append('\n').append(String.format(java.util.Locale.ROOT,
                        "FLATMC-REACTIVE mode=%s points=%d empty=%d decisions=%d changed=%d passes=%d rollouts=%d avgMs=%.2f",
                        e.getKey(), r[0], r[6], r[1], r[2], r[3], r[4], r[1] == 0 ? 0 : r[5] / 1e6 / r[1]));
            }
            sb.append('\n').append("FLATMC-REJECTED ").append(render(REJECTED, Integer.MAX_VALUE));
            sb.append('\n').append("FLATMC-REASONS ").append(render(BY_REASON, Integer.MAX_VALUE));
            if (!UNSIMULATABLE.isEmpty()) {
                sb.append('\n').append("FLATMC-UNSIMULATABLE ").append(render(UNSIMULATABLE, 15));
            }
            if (!FAILURES.isEmpty()) {
                sb.append('\n').append("FLATMC-FAILURES ").append(render(FAILURES, 10));
            }
            if (!PLANS.isEmpty()) {
                sb.append('\n').append("FLATMC-PLANS ").append(render(PLANS, 10));
            }
            if (!SLOW.isEmpty()) {
                sb.append('\n').append("FLATMC-SLOW ").append(String.join(" | ", SLOW));
            }
        }
        return sb.toString();
    }

    private static int failureTotalLocked() {
        int total = 0;
        for (final int[] n : FAILURES.values()) {
            total += n[0];
        }
        return total;
    }

    /** "key=count; key=count" with the most frequent keys first, at most {@code limit} of them. */
    private static String render(final Map<String, int[]> map, final int limit) {
        final List<Map.Entry<String, int[]>> entries = new ArrayList<>(map.entrySet());
        Collections.sort(entries, (a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]));
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size() && i < limit; i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(entries.get(i).getKey()).append('=').append(entries.get(i).getValue()[0]);
        }
        return sb.toString();
    }
}
