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

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A seeded {@link Random} that counts — and optionally traces — every primitive draw.
 *
 * <p>The plan requires an RNG draw trace before any branch-scoped RNG or parallel simulation work
 * can claim determinism parity, because replacing the process-global generator changes <em>which
 * stream consumes each draw</em> even when the seed is unchanged. This class makes the current draw
 * order observable without changing it.</p>
 *
 * <p>Only {@link Random#next(int)} is overridden, and it delegates to {@code super}. Every other
 * {@code Random} method is built on {@code next(int)}, so the produced sequence for a given seed is
 * bit-for-bit the same as {@code new Random(seed)} — the trace is an observation, not a
 * substitution. Installing this generator therefore cannot change a game's outcome.</p>
 *
 * <p>Install it with {@code MyRandom.setRandom(new TracingRandom(seed))} in a harness run. It is
 * never installed by normal play.</p>
 */
public final class TracingRandom extends Random {
    private static final long serialVersionUID = 1L;

    private final AtomicLong draws = new AtomicLong();

    public TracingRandom(final long seed) {
        super(seed);
    }

    /** How many primitive draws have been taken from this generator. */
    public long getDrawCount() {
        return draws.get();
    }

    @Override
    protected int next(final int bits) {
        final int value = super.next(bits);
        // The superclass constructor draws nothing, but be defensive: setSeed() is called from
        // Random's constructor before this instance's fields are assigned in some JDK builds.
        final AtomicLong counter = draws;
        if (counter == null) {
            return value;
        }
        final long ordinal = counter.incrementAndGet();
        PerfProbe.count(PerfCounter.RANDOM_DRAWS);
        if (PerfProbe.isTracing()) {
            PerfProbe.trace(TraceCategory.RANDOM, ordinal + "\t" + bits + "\t" + value);
        }
        return value;
    }
}
