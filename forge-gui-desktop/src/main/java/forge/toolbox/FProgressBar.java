package forge.toolbox;

import java.util.Date;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import forge.gui.interfaces.IProgressBar;

/**
 * A simple progress bar component using the Forge skin.
 */
@SuppressWarnings("serial")
public class FProgressBar extends JProgressBar implements IProgressBar {
    private long startMillis = 0;
    private int tempVal = 0, etaSecs = 0;
    private volatile String desc = "";
    private volatile boolean showETA = true;
    private volatile boolean showCount = true;

    private volatile boolean percentMode = false;

    public FProgressBar() {
        super();
        reset();
        setStringPainted(true);
    }

    /**
     * Runs {@code r} on the Swing event dispatch thread. Download services
     * drive this progress bar from background threads; mutating a
     * {@link JProgressBar} off-EDT races with painting and can NPE deep in
     * {@code BasicProgressBarUI}, so every mutator funnels through here.
     */
    private static void runOnEdt(final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }

    @Override
    public void setDescription(final String s0) {
        runOnEdt(() -> {
            desc = s0;
            setString(s0);
        });
    }

    /** Increments bar, thread safe. Calculations executed on separate thread. */
    public void setValueThreadSafe(final int value) {
        tempVal = value;
        SwingUtilities.invokeLater(barIncrementor);
    }

    private final Runnable barIncrementor = () -> {
        setValue(tempVal);
        tempVal = 0;
    };

    @Override
    public void setValue(final int progress) {
        if (!SwingUtilities.isEventDispatchThread()) {
            runOnEdt(() -> setValue(progress));
            return;
        }
        super.setValue(progress);

        // String.format leads to StringBuilder anyway. Direct calls will be faster
        final StringBuilder sb = new StringBuilder(desc);
        if (showCount) {
            sb.append(" ");
            final int maximum = getMaximum();
            if (percentMode) {
                sb.append(100 * progress / maximum).append("%");
            }
            else {
                sb.append(progress).append(" of ").append(maximum);
            }
        }

        if (showETA) {
            calculateETA(progress);
            sb.append(", ETA").append(String.format("%02d:%02d:%02d", etaSecs / 3600, (etaSecs % 3600) / 60, etaSecs % 60 + 1));
        }
        setString(sb.toString());
    }

    /** Resets the various values required for this class. */
    @Override
    public void reset() {
        if (!SwingUtilities.isEventDispatchThread()) {
            runOnEdt(this::reset);
            return;
        }
        setIndeterminate(true);
        setValue(0);
        tempVal = 0;
        startMillis = new Date().getTime();
        setIndeterminate(false);
        setShowETA(true);
        setShowCount(true);
    }

    @Override
    public void setMaximum(final int n) {
        if (!SwingUtilities.isEventDispatchThread()) {
            runOnEdt(() -> setMaximum(n));
            return;
        }
        super.setMaximum(n);
    }

    @Override
    public void setShowETA(final boolean b0) {
        showETA = b0;
    }

    @Override
    public void setShowCount(final boolean b0) {
        showCount = b0;
    }

    private void calculateETA(final int v0) {
        final float tempMillis = new Date().getTime();
        final float timePerUnit = (tempMillis - startMillis) / v0;
        etaSecs = (int) ((getMaximum() - v0) * timePerUnit) / 1000;
    }

    public boolean isPercentMode() {
        return percentMode;
    }
    @Override
    public void setPercentMode(final boolean value) {
        percentMode = value;
    }
}
