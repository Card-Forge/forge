package forge.toolbox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;

/**
 * Thin horizontal progress bar drained right-to-left across the pick countdown.
 * Colour stages: CLR_ACTIVE above 15s, yellow at 15s, red at 5s. Border is a
 * brightened shade of CLR_ACTIVE so it stays constant through transitions.
 */
public final class DraftTimerRope extends FDisplayObject {

    private static final float BORDER_BRIGHTEN = 2.0f;
    private static final Color FALLBACK_ACTIVE = new Color(0x3A6EA5FF);

    private long startMillis;
    private int durationSeconds;
    private boolean wasContinuousRendering;

    public void start(int seconds) {
        if (seconds <= 0) {
            stop();
            return;
        }
        if (!isRunning()) {
            wasContinuousRendering = Gdx.graphics.isContinuousRendering();
            Gdx.graphics.setContinuousRendering(true);
        }
        durationSeconds = seconds;
        startMillis = System.currentTimeMillis();
    }

    public void stop() {
        if (!isRunning()) {
            return; // never started, or already stopped — don't clobber the render mode
        }
        startMillis = 0;
        Gdx.graphics.setContinuousRendering(wasContinuousRendering);
    }

    /** Returns remaining seconds, rounded up; 0 when stopped or expired. */
    public int getRemainingSeconds() {
        return (int) Math.ceil(remainingExact());
    }

    private boolean isRunning() {
        return startMillis > 0 && durationSeconds > 0;
    }

    private double remainingExact() {
        double elapsed = (System.currentTimeMillis() - startMillis) / 1000.0;
        return Math.max(0.0, durationSeconds - elapsed);
    }

    private Color colorForStage() {
        if (!isRunning()) return activeColor();
        int whole = (int) Math.ceil(remainingExact());
        if (whole <= 5)  return Color.RED;
        if (whole <= 15) return Color.YELLOW;
        return activeColor();
    }

    private static Color activeColor() {
        FSkinColor c = FSkinColor.get(Colors.CLR_ACTIVE);
        return (c != null) ? c.getColor() : FALLBACK_ACTIVE;
    }

    private static Color brighten(Color c, float factor) {
        return new Color(
                Math.min(1f, c.r * factor),
                Math.min(1f, c.g * factor),
                Math.min(1f, c.b * factor),
                c.a);
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        if (w <= 0 || h <= 0) return;

        Color active = activeColor();
        Color border = brighten(active, BORDER_BRIGHTEN);

        g.drawRect(1f, border, 0, 0, w, h);

        if (isRunning()) {
            float innerW = w - 2;
            float fillWidth = (float) (innerW * remainingExact() / durationSeconds);
            if (fillWidth > 0) {
                // Right-anchored: rope drains left as time runs out
                float x = 1 + (innerW - fillWidth);
                g.fillRect(colorForStage(), x, 1, fillWidth, h - 2);
            }
        }
    }
}
