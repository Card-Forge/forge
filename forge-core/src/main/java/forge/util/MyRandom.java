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
     * Per-thread random source. Thread-local so concurrent games in one JVM
     * (headless simulation workers, parallel AI harnesses) that seed their
     * stream via {@link #setRandom(Random)} cannot contaminate each other's
     * sequences. Inheritable so threads spawned from a seeded thread (AI
     * evaluation helpers) continue the parent's stream instead of silently
     * falling back to an unseeded one. Single-threaded behavior is unchanged.
     */
    private static final ThreadLocal<Random> random = new InheritableThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new SecureRandom();
        }
    };

    /**
     * <p>
     * percentTrue.<br>
     * If percent is like 30, then 30% of the time it will be true.
     * </p>
     *
     * @param percent an int.
     * @return a boolean.
     */
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
        MyRandom.random.set(random);
    }

    public static int[] splitIntoRandomGroups(final int value, final int numGroups) {
        int[] groups = new int[numGroups];

        for (int i = 0; i < value; i++) {
            groups[getRandom().nextInt(numGroups)]++;
        }

        return groups;
    }
}
