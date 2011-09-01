package forge;

import java.util.Random;

/**
 * <p>MyRandom class.<br>Preferably all Random numbers should be retrieved using this wrapper class</p>
 *
 * @author Forge
 * @version $Id$
 */
public class MyRandom {
    /** Constant <code>random</code>. */
    public static Random random = new Random();

    /**
     * <p>percentTrue.<br>If percent is like 30, then 30% of the time it will be true.</p>
     *
     * @param percent a int.
     * @return a boolean.
     */
    public static boolean percentTrue(final int percent) {
        return percent > random.nextInt(100);
    }
}
