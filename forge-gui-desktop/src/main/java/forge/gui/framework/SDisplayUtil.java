package forge.gui.framework;

import java.awt.Color;
import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import forge.FThreads;
import forge.view.FFrame;

/**
 * Experimental static factory for generic operations carried out
 * onto specific members of the framework. Doublestrike 11-04-12
 *
 * <br><br><i>(S at beginning of class name denotes a static factory.)</i>
 */
public class SDisplayUtil {
    private static boolean remindIsRunning = false;
    private static int counter = 0;
    private static int[] newA = null, newR = null, newG = null, newB = null;
    private static Timer timer1 = null;

    /** Flashes animation on input panel if play is currently waiting on input.
     *
     * @param tab0 &emsp; {@link java.GuiBase.getInterface().framework.IVDoc}
     */
    public static void remind(final IVDoc<? extends ICDoc> tab0) {
        showTab(tab0);
        final JPanel pnl = tab0.getParentCell().getBody();

        // To adjust, only touch these two values.
        final int steps = 5;    // Number of delays
        final int delay = 80;  // Milliseconds between steps

        if (remindIsRunning) { return; }
        if (pnl == null) { return; }

        remindIsRunning = true;
        final int oldR = pnl.getBackground().getRed();
        final int oldG = pnl.getBackground().getGreen();
        final int oldB = pnl.getBackground().getBlue();
        final int oldA = pnl.getBackground().getAlpha();
        counter = 0;
        newR = new int[steps];
        newG = new int[steps];
        newB = new int[steps];
        newA = new int[steps];

        for (int i = 0; i < steps; i++) {
            newR[i] = ((255 - oldR) / steps * i);
            newG[i] = (oldG / steps * i);
            newB[i] = (oldB / steps * i);
            newA[i] = ((255 - oldA) / steps * i);
        }

        final TimerTask tt = new TimerTask() {
            @Override public final void run() {
                counter++;
                if (counter != (steps - 1)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                        final int r = newR == null ? oldR : newR[counter];
                        final int a = newA == null ? oldA : newR[counter];
                        pnl.setBackground(new Color(r, oldG, oldB, a));
                    }
                    });
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            pnl.setBackground(new Color(oldR, oldG, oldB, oldA));
                        }
                    });
                    remindIsRunning = false;
                    timer1.cancel();
                    newR = null;
                    newG = null;
                    newB = null;
                    newA = null;
                }
            }
        };

        timer1 = new Timer();
        timer1.scheduleAtFixedRate(tt, 0, delay);
    }

    /** @param tab0 &emsp; {@link java.GuiBase.getInterface().framework.IVDoc} */
    public static void showTab(final IVDoc<? extends ICDoc> tab0) {

        final Runnable showTabRoutine = new Runnable() {
            @Override public void run() {
                // FThreads.assertExecutedByEdt(true); - always true
                final Component c = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
                final DragCell dc = tab0.getParentCell();
                if (dc != null) {
                    dc.setSelected(tab0);
                }
                // set focus back to previous owner, if any
                if (null != c) {
                    c.requestFocusInWindow();
                }
            }
        };
        FThreads.invokeInEdtLater(showTabRoutine);
    }

    public static GraphicsDevice getGraphicsDevice(final Point point) {
        final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (final GraphicsDevice gd : env.getScreenDevices()) {
            if (gd.getDefaultConfiguration().getBounds().contains(point)) {
                return gd;
            }
        }
        return null;
    }

    public static GraphicsDevice getGraphicsDevice(final Rectangle rect) {
        return getGraphicsDevice(new Point(rect.x + (rect.width / 2), rect.y + (rect.height / 2)));
    }

    public static Rectangle getScreenBoundsForPoint(final Point point) {
        final GraphicsDevice gd = getGraphicsDevice(point);
        if (gd == null) {
            //return bounds of default monitor if point not on any screen
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        }
        return gd.getDefaultConfiguration().getBounds();
    }

    public static Rectangle getScreenMaximizedBounds(final Rectangle rect) {
        final GraphicsDevice gd = getGraphicsDevice(rect);
        if (gd == null) {
            //return bounds of default monitor if center of rect not on any screen
            return GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        }

        final GraphicsConfiguration config = gd.getDefaultConfiguration();
        final Rectangle bounds = config.getBounds();
        final Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(config);
        bounds.x += screenInsets.left;
        bounds.y += screenInsets.top;
        bounds.width -= screenInsets.left + screenInsets.right;
        bounds.height -= screenInsets.top + screenInsets.bottom;
        return bounds;
    }

    public static boolean setFullScreenWindow(final FFrame frame, final boolean fullScreen) {
        return setFullScreenWindow(getGraphicsDevice(frame.getNormalBounds()), frame, fullScreen);
    }

    private static boolean setFullScreenWindow(final GraphicsDevice gd, final Window window, final boolean fullScreen) {
        if (gd != null && gd.isFullScreenSupported()) {
            if (fullScreen) {
                gd.setFullScreenWindow(window);
            }
            else if (gd.getFullScreenWindow() == window) {
                gd.setFullScreenWindow(null);
            }
            return true;
        }
        return false;
    }
}
