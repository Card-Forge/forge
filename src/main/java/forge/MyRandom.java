package forge;

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
    private static Random random = new Random();

    /**
     * <p>
     * percentTrue.<br>
     * If percent is like 30, then 30% of the time it will be true.
     * </p>
     * 
     * @param percent
     *            a int.
     * @return a boolean.
     */
    public static boolean percentTrue(final int percent) {
        return percent > getRandom().nextInt(100);
    }

    /**
     * @return the random
     */
    public static Random getRandom() {
        return random;
    }

    /**
     * @param random the random to set
     */
    public static void setRandom(Random random) {
        MyRandom.random = random; // TODO: Add 0 to parameter's name.
    }
}
