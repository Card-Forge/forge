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
package forge.util;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * <p>
 * MyRandom class.<br>
 * Preferably all Random numbers should be retrieved using this wrapper class
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class MyRandom {
    /**
     * Every Random method (nextInt, nextBoolean, shuffle, ...) funnels through next(bits).
     * The draw counter lives on the stream, so a game's undo detection only sees its own draws.
     */
    private static final class CountingRandom extends Random {
        private final Random delegate;
        private final AtomicLong calls = new AtomicLong();
        CountingRandom(Random delegate) { super(0); this.delegate = delegate; }
        @Override protected int next(int bits) {
            calls.incrementAndGet();
            return delegate.nextInt() >>> (32 - bits);
        }
        @Override public void setSeed(long seed) {
            if (delegate != null) delegate.setSeed(seed);   // null during super(0)
        }
        long callCount() { return calls.get(); }
    }

    /**
     * Per-thread random source. Thread-local so concurrent games in one JVM
     * (headless simulation workers, parallel AI harnesses) that seed their
     * stream via {@link #setRandom(Random)} cannot contaminate each other's
     * sequences. Inheritable so threads spawned from a seeded thread (AI
     * evaluation helpers) continue the parent's stream instead of silently
     * falling back to an unseeded one. Single-threaded behavior is unchanged.
     */
    private static final ThreadLocal<CountingRandom> random = new InheritableThreadLocal<CountingRandom>() {
        @Override
        protected CountingRandom initialValue() {
            return new CountingRandom(new SecureRandom());
        }
    };

    /** Number of random draws made so far on the current thread's stream. Used by undo to detect that randomness was consumed. */
    public static long callCount() { return random.get().callCount(); }

    public static boolean percentTrue(final int percent) {
        return percent > MyRandom.getRandom().nextInt(100);
    }

    /**
     * Gets the random.
     * 
     * @return the random for the current thread
     */
    public static Random getRandom() {
        return random.get();
    }

    /**
     * Sets the random provider for the current thread (and threads it spawns
     * afterwards). Used for deterministic simulation.
     * @param random the random
     */
    public static void setRandom(Random random) {
        MyRandom.random.set(random instanceof CountingRandom ? (CountingRandom) random : new CountingRandom(random));
    }

    public static int[] splitIntoRandomGroups(final int value, final int numGroups) {
        int[] groups = new int[numGroups];

        for (int i = 0; i < value; i++) {
            groups[getRandom().nextInt(numGroups)]++;
        }

        return groups;
    }
}
