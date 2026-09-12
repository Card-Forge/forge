package forge.gui;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.Timer;

import forge.toolbox.FSkin;

/**
 * Thin horizontal progress bar drawn at the top of {@link FDraftOverlay},
 * draining smoothly across the pick countdown. The outer border is always
 * drawn while the component is visible; the inner fill is drawn only while
 * a countdown is running.
 *
 * Fill color mirrors the timer label: CLR_ACTIVE above 15s, yellow at
 * 15s, red at 5s. Border color is a darkened shade of the current fill
 * color so the rope reads as a single unit through transitions.
 */
@SuppressWarnings("serial")
public final class DraftTimerRope extends JComponent {

    private static final int   REPAINT_INTERVAL_MS = 33;
    private static final float BORDER_BRIGHTEN     = 2.0f;
    private static final Color FALLBACK_ACTIVE     = new Color(0x3A6EA5);

    private final Timer repaintTimer = new Timer(REPAINT_INTERVAL_MS, e -> repaint());

    private long startMillis;
    private int  durationSeconds;

    public DraftTimerRope() {
        setOpaque(false);
        repaintTimer.setRepeats(true);
    }

    public void start(int seconds) {
        if (seconds <= 0) {
            stop();
            return;
        }
        durationSeconds = seconds;
        startMillis     = System.currentTimeMillis();
        repaintTimer.start();
        repaint();
    }

    public void stop() {
        repaintTimer.stop();
        startMillis = 0;
        repaint();
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
        FSkin.SkinColor c = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
        return (c != null) ? c.getColor() : FALLBACK_ACTIVE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        Color stage  = colorForStage();
        // Border stays constant as the fill transitions through yellow and red
        Color active = activeColor();
        Color border = new Color(
                Math.min(255, Math.round(active.getRed()   * BORDER_BRIGHTEN)),
                Math.min(255, Math.round(active.getGreen() * BORDER_BRIGHTEN)),
                Math.min(255, Math.round(active.getBlue()  * BORDER_BRIGHTEN)));

        g.setColor(border);
        g.drawRect(0, 0, w - 1, h - 1);

        if (isRunning()) {
            int innerW = w - 2;
            int fillWidth = (int) Math.round(innerW * remainingExact() / durationSeconds);
            if (fillWidth > 0) {
                // Anchor fill to the right edge so the rope drains left -> right
                int x = 1 + (innerW - fillWidth);
                g.setColor(stage);
                g.fillRect(x, 1, fillWidth, h - 2);
            }
        }
    }
}
