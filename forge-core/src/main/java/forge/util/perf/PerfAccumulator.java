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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Counter, timer and trace storage shared by per-decision records and whole-run aggregates.
 *
 * <p>Atomic arrays are used because a conventional priority decision performs most of its work on
 * the {@code "Game AI Eval"} watchdog thread while the game thread waits on the decision's future.
 * Both threads write to the same accumulator, and it has to survive that without tearing. This is
 * <em>not</em> a claim that the AI itself is thread-safe.</p>
 */
public class PerfAccumulator {
    private static final int COUNTER_COUNT = PerfCounter.values().length;
    private static final int TIMER_COUNT = PerfTimer.values().length;

    private final AtomicLongArray counters = new AtomicLongArray(COUNTER_COUNT);
    private final AtomicLongArray timerNanos = new AtomicLongArray(TIMER_COUNT);
    private final AtomicLongArray timerSpans = new AtomicLongArray(TIMER_COUNT);

    /** Guarded by itself; only populated when tracing is on, which is off in timing runs. */
    private final List<String> trace = new ArrayList<>();

    public final long get(final PerfCounter counter) {
        return counters.get(counter.ordinal());
    }

    public final long getNanos(final PerfTimer timer) {
        return timerNanos.get(timer.ordinal());
    }

    /** How many outermost spans of {@code timer} were recorded; nested ones are folded away. */
    public final long getSpanCount(final PerfTimer timer) {
        return timerSpans.get(timer.ordinal());
    }

    /** An unmodifiable snapshot of the trace entries recorded so far. */
    public final List<String> getTrace() {
        synchronized (trace) {
            return Collections.unmodifiableList(new ArrayList<>(trace));
        }
    }

    public final boolean hasTrace() {
        synchronized (trace) {
            return !trace.isEmpty();
        }
    }

    final void add(final PerfCounter counter, final long amount) {
        counters.addAndGet(counter.ordinal(), amount);
    }

    final void addSpan(final PerfTimer timer, final long nanos) {
        addSpans(timer, nanos, 1L);
    }

    /** Folds a whole record's worth of spans in at once, for aggregation. */
    final void addSpans(final PerfTimer timer, final long nanos, final long spans) {
        timerNanos.addAndGet(timer.ordinal(), nanos);
        timerSpans.addAndGet(timer.ordinal(), spans);
    }

    final void addTrace(final String entry) {
        synchronized (trace) {
            trace.add(entry);
        }
    }
}
