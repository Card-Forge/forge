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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The measurement seam used by the AI and rules engines.
 *
 * <p>This class exists so that the performance work described in the AI performance plan can be
 * driven by evidence from this revision rather than by historical profiles. It records what work
 * each AI decision performed and how long the interesting phases of it took, and it does nothing
 * else: no probe may read game state, allocate on a hot path while disabled, or influence a
 * decision in any way.</p>
 *
 * <h2>Disabled by default</h2>
 * <p>Every entry point starts with a volatile boolean read and returns immediately when probing is
 * off, which is the shipped configuration. Enable it with {@code -Dforge.perf=true}, or
 * programmatically through {@link #setEnabled(boolean)} <em>before</em> a game starts — flipping the
 * flag in the middle of a decision is not supported and will simply produce a partial record.</p>
 *
 * <h2>Nesting</h2>
 * <p>Decisions nest: a full simulation branch resolves combat inside a copied game and calls back
 * into the block controller. A nested decision is deliberately <em>not</em> given its own record;
 * its work is attributed to the outermost decision, which is the unit a player actually waits for.
 * Re-entrant occurrences of the same {@link PerfTimer} are folded into the outermost span for the
 * same reason, so a recursive rules path cannot multiply its own elapsed time.</p>
 */
public final class PerfProbe {
    /** Returned by {@link #start(PerfTimer)} when probing is off; {@link #stop} then does nothing. */
    private static final long NOT_TIMING = Long.MIN_VALUE;
    /** Returned by {@link #start(PerfTimer)} for a re-entrant span; only the depth is unwound. */
    private static final long NESTED = Long.MIN_VALUE + 1L;

    /** Placeholder for a decision whose work is attributed to an enclosing decision. */
    private static final DecisionRecord NESTED_DECISION =
            new DecisionRecord(-1L, DecisionKind.PRIORITY, "", 0, "", 0L);

    private static volatile boolean enabled = Boolean.getBoolean("forge.perf");
    private static volatile boolean tracing = Boolean.getBoolean("forge.perf.trace");

    private static final AtomicLong nextId = new AtomicLong();
    private static final AtomicInteger decisionDepth = new AtomicInteger();
    private static final List<PerfSink> sinks = new CopyOnWriteArrayList<>();

    private static volatile DecisionRecord current;
    private static volatile PerfAccumulator global = new PerfAccumulator();

    /**
     * Nesting depth per timer, per thread. It is per-thread because the game thread and the
     * {@code "Game AI Eval"} thread both run rules code inside one decision, and folding one
     * thread's re-entrancy must not hide the other's outermost span.
     */
    private static final ThreadLocal<int[]> timerDepth =
            ThreadLocal.withInitial(() -> new int[PerfTimer.values().length]);

    private PerfProbe() {
    }

    // ------------------------------------------------------------------ configuration

    public static boolean isEnabled() {
        return enabled;
    }

    /** Call before a game starts. Turning probing off also stops tracing. */
    public static void setEnabled(final boolean value) {
        enabled = value;
        if (!value) {
            tracing = false;
        }
    }

    /**
     * Whether decision traces are being recorded. Tracing builds strings on decision paths and is
     * far more intrusive than counting, so timing runs must leave it off.
     */
    public static boolean isTracing() {
        return tracing;
    }

    /** Enabling tracing implies enabling probing. */
    public static void setTracing(final boolean value) {
        if (value) {
            enabled = true;
        }
        tracing = value;
    }

    public static void addSink(final PerfSink sink) {
        if (sink != null) {
            sinks.add(sink);
        }
    }

    public static void removeSink(final PerfSink sink) {
        sinks.remove(sink);
    }

    /** Totals across every probe since the last {@link #resetGlobal()}, decision-scoped or not. */
    public static PerfAccumulator getGlobal() {
        return global;
    }

    public static void resetGlobal() {
        global = new PerfAccumulator();
    }

    /**
     * Drops all probe state: sinks, the global accumulator, the decision counter and any decision
     * left open by a failed run. Intended for test isolation, not for use during a game.
     */
    public static void reset() {
        sinks.clear();
        global = new PerfAccumulator();
        current = null;
        decisionDepth.set(0);
        nextId.set(0L);
        enabled = false;
        tracing = false;
    }

    // ------------------------------------------------------------------ decisions

    /**
     * Opens a decision. The result must be passed to {@link #endDecision(DecisionRecord)} from a
     * {@code finally} block, and may be null.
     */
    public static DecisionRecord beginDecision(final DecisionKind kind, final String player, final int turn,
            final String phase) {
        if (!enabled) {
            return null;
        }
        if (decisionDepth.getAndIncrement() != 0) {
            return NESTED_DECISION;
        }
        final DecisionRecord record =
                new DecisionRecord(nextId.incrementAndGet(), kind, player, turn, phase, System.nanoTime());
        current = record;
        return record;
    }

    /** Closes a decision opened by {@link #beginDecision}. Safe to call with null. */
    public static void endDecision(final DecisionRecord record) {
        if (record == null) {
            return;
        }
        decisionDepth.decrementAndGet();
        if (record == NESTED_DECISION) {
            return;
        }
        record.finish(System.nanoTime());
        current = null;
        for (final PerfSink sink : sinks) {
            try {
                sink.onDecision(record);
            } catch (final RuntimeException e) {
                // A broken diagnostic sink must never take a game down with it.
                System.err.println("PerfProbe sink failed: " + e);
            }
        }
    }

    /** The decision currently being measured, or null when none is open. */
    public static DecisionRecord getCurrentDecision() {
        return current;
    }

    // ------------------------------------------------------------------ counters

    public static void count(final PerfCounter counter) {
        count(counter, 1L);
    }

    public static void count(final PerfCounter counter, final long amount) {
        if (!enabled) {
            return;
        }
        global.add(counter, amount);
        final DecisionRecord record = current;
        if (record != null) {
            record.add(counter, amount);
        }
    }

    // ------------------------------------------------------------------ timers

    /**
     * Starts a span. The returned token must be handed to {@link #stop(PerfTimer, long)} from a
     * {@code finally} block; its value is opaque and carries the "disabled" and "nested" cases.
     */
    public static long start(final PerfTimer timer) {
        if (!enabled) {
            return NOT_TIMING;
        }
        final int[] depths = timerDepth.get();
        final int ordinal = timer.ordinal();
        if (depths[ordinal]++ > 0) {
            return NESTED;
        }
        // System.nanoTime()'s origin is arbitrary and may be negative, so a real reading could in
        // principle collide with a sentinel. Nudging it costs 2ns of accuracy and removes the case
        // where stop() would mistake a live span for a disabled one and leak its depth.
        final long now = System.nanoTime();
        return now == NOT_TIMING || now == NESTED ? now + 2L : now;
    }

    /** Ends a span started by {@link #start(PerfTimer)}. */
    public static void stop(final PerfTimer timer, final long token) {
        if (token == NOT_TIMING) {
            return;
        }
        final int[] depths = timerDepth.get();
        final int ordinal = timer.ordinal();
        if (depths[ordinal] > 0) {
            depths[ordinal]--;
        }
        if (token == NESTED) {
            return;
        }
        final long elapsed = System.nanoTime() - token;
        global.addSpan(timer, elapsed);
        final DecisionRecord record = current;
        if (record != null) {
            record.addSpan(timer, elapsed);
        }
    }

    // ------------------------------------------------------------------ traces

    /**
     * Appends a trace entry. Callers must guard the argument construction with
     * {@link #isTracing()} so that disabled runs build no strings at all.
     */
    public static void trace(final TraceCategory category, final String detail) {
        if (!tracing) {
            return;
        }
        final DecisionRecord record = current;
        if (record == null) {
            // Entries recorded outside a decision have nothing to be compared against and no owner
            // to drain them, so accumulating them would grow without bound across a long batch.
            return;
        }
        record.addTrace(category.jsonName() + '\t' + detail);
    }
}
