package forge.gui.framework;

import java.awt.Color;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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
     * @param tab0 &emsp; {@link java.gui.framework.IVDoc}
     */
    public static void remind(final IVDoc tab0) {
        showTab(tab0);
        final JPanel pnl = tab0.getParentCell().getBody();

        // To adjust, only touch these two values.
        final int steps = 5;    // Number of delays
        final int delay = 80;  // Milliseconds between steps

        if (remindIsRunning) { return; }
        if (pnl.equals(null)) { return; }

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
            newR[i] = (int) ((255 - oldR) / steps * i);
            newG[i] = (int) (oldG / steps * i);
            newB[i] = (int) (oldB / steps * i);
            newA[i] = (int) ((255 - oldA) / steps * i);
        }

        final TimerTask tt = new TimerTask() {
            @Override
            public void run() {
                counter++;
                if (counter != (steps - 1)) {
                    SwingUtilities.invokeLater(new Runnable() { @Override
                        public void run() {
                            pnl.setBackground(new Color(newR[counter], oldG, oldB, newA[counter]));
                        }
                    });
                }
                else {
                    SwingUtilities.invokeLater(new Runnable() { @Override
                        public void run() { pnl.setBackground(new Color(oldR, oldG, oldB, oldA)); } });
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

    /** @param tab0 &emsp; {@link java.gui.framework.IVDoc} */
    public static void showTab(final IVDoc tab0) {
        tab0.getParentCell().setSelected(tab0);
    }
}
