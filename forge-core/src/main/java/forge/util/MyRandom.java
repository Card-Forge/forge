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
    /** Constant <code>random</code>. */
    private static Random random = new SecureRandom();

    /**
     * Changes into a non-CSPRNG, which can be seeded for use in tests/repeatable experiments.
     */
    public static void setSeed(int seed) {
	System.out.println("Setting the RNG seed to: " + seed);
	random = new Random(seed);
    }
    
    /**
     * Returns True with Percent probability.
     * 
     * TODO: My guess is no one is passing in a number scaled to 100. This API should probably be cut.
     */
    public static boolean percentTrue(final long percent) {
        return percent > MyRandom.getRandom().nextDouble() * 100;
    }

    /**
     * Gets the random.
     * 
     * TODO: Make this private, and instead add a robust set of APIs here.
     * 
     * @return the random
     */
    public static Random getRandom() {
        return MyRandom.random;
    }

    public static int[] splitIntoRandomGroups(final int value, final int numGroups) {
        int[] groups = new int[numGroups];
        
        for (int i = 0; i < value; i++) {
            groups[MyRandom.getRandom().nextInt(numGroups)]++;
        }

        return groups;
    }
}
