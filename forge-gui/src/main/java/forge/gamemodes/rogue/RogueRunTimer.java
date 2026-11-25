package forge.gamemodes.rogue;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Tracks active play time for a Rogue Commander run.
 */
public class RogueRunTimer {
    private long elapsedMillis;

    @XStreamOmitField
    private transient Long startedAtMillis;

    public synchronized void start() {
        if (startedAtMillis == null) {
            startedAtMillis = System.currentTimeMillis();
        }
    }

    public synchronized void checkpoint() {
        if (startedAtMillis == null) {
            return;
        }

        long now = System.currentTimeMillis();
        elapsedMillis += Math.max(0, now - startedAtMillis);
        startedAtMillis = now;
    }

    public synchronized void stop() {
        checkpoint();
        startedAtMillis = null;
    }

    public synchronized boolean isRunning() {
        return startedAtMillis != null;
    }

    public synchronized long getElapsedMillis() {
        if (startedAtMillis == null) {
            return elapsedMillis;
        }

        return elapsedMillis + Math.max(0, System.currentTimeMillis() - startedAtMillis);
    }
}
