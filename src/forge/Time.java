package forge;

/**
 * <p>Time class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public class Time {
    private long startTime;
    private long stopTime;

    /**
     * <p>Constructor for Time.</p>
     */
    public Time() {
        start();
    }

    /**
     * <p>start.</p>
     */
    public void start() {
        startTime = System.currentTimeMillis();
    }

    /**
     * <p>stop.</p>
     *
     * @return a double.
     */
    public double stop() {
        stopTime = System.currentTimeMillis();
        return getTime();
    }

    /**
     * <p>getTime.</p>
     *
     * @return a double.
     */
    public double getTime() {
        return (stopTime - startTime) / 1000.0;
    }
}
