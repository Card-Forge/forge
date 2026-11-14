package forge.gamemodes.rogue;

import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Tracks active play time for a Rogue Commander run.
 */
public class RogueRunTimer {
    private long elapsedMillis;

    @XStreamOmitField
    private transient Long startedAtMillis;

    public void start() {
        if (startedAtMillis == null) {
            startedAtMillis = System.currentTimeMillis();
        }
    }

    public void checkpoint() {
        if (startedAtMillis == null) {
            return;
        }

        long now = System.currentTimeMillis();
        elapsedMillis += Math.max(0, now - startedAtMillis);
        startedAtMillis = now;
    }

    public void stop() {
        checkpoint();
        startedAtMillis = null;
    }

    public boolean isRunning() {
        return startedAtMillis != null;
    }

    public long getElapsedMillis() {
        if (startedAtMillis == null) {
            return elapsedMillis;
        }

        return elapsedMillis + Math.max(0, System.currentTimeMillis() - startedAtMillis);
    }
}
