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
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

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
    /** Constant <code>random</code>. */
    private static final AtomicLong calls = new AtomicLong();
    private static Random random = new CountingRandom(new SecureRandom());

    /** Every Random method (nextInt, nextBoolean, shuffle, ...) funnels through next(bits). */
    private static final class CountingRandom extends Random {
        private final Random delegate;
        CountingRandom(Random delegate) { super(0); this.delegate = delegate; }
        @Override protected int next(int bits) {
            calls.incrementAndGet();
            if (TRACE) { trace(); }
            return delegate.nextInt() >>> (32 - bits);
        }
        @Override public void setSeed(long seed) {
            if (delegate != null) delegate.setSeed(seed);   // null during super(0)
        }
    }
    /** Number of random draws made so far. Used by undo to detect that randomness was consumed. */
    public static long callCount() { return calls.get(); }

    // Debug only (-Dforge.undo.debug=true): tally draws by thread and call site, to find who consumes randomness.
    private static final boolean TRACE = Boolean.getBoolean("forge.undo.debug");
    private static final Map<String, LongAdder> SITES = new ConcurrentHashMap<>();

    private static void trace() {
        final StringBuilder key = new StringBuilder(Thread.currentThread().getName()).append(" <- ");
        int shown = 0;
        for (final StackTraceElement e : new Throwable().getStackTrace()) {
            final String c = e.getClassName();
            if (c.startsWith("java.") || c.startsWith("jdk.") || c.startsWith("forge.util.MyRandom")) { continue; }
            key.append(c.substring(c.lastIndexOf('.') + 1)).append('.').append(e.getMethodName())
                    .append(':').append(e.getLineNumber()).append(shown < 3 ? " < " : "");
            if (++shown == 4) { break; }
        }
        SITES.computeIfAbsent(key.toString(), k -> new LongAdder()).increment();
    }

    /** Debug only: the {@code top} busiest draw sites since the last call, as text; clears the tally. */
    public static String drainTrace(final int top) {
        final StringBuilder sb = new StringBuilder();
        SITES.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().sum(), a.getValue().sum()))
                .limit(top)
                .forEach(e -> sb.append("\n    ").append(e.getValue().sum()).append("x ").append(e.getKey()));
        SITES.clear();
        return sb.toString();
    }

    public static boolean percentTrue(final int percent) {
        return percent > MyRandom.getRandom().nextInt(100);
    }

    /**
     * Gets the random.
     * 
     * @return the random
     */
    public static Random getRandom() {
        return MyRandom.random;
    }

    /**
     * Sets the random provider. Used for deterministic simulation.
     * @param random the random
     */
    public static void setRandom(Random random) {
        MyRandom.random = random instanceof CountingRandom ? random : new CountingRandom(random);
    }

    public static int[] splitIntoRandomGroups(final int value, final int numGroups) {
        int[] groups = new int[numGroups];

        for (int i = 0; i < value; i++) {
            groups[random.nextInt(numGroups)]++;
        }

        return groups;
    }
}
