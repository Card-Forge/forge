package forge.view.toolbox;

import java.util.Date;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import forge.gui.GuiUtils;

/** 
 * A simple progress bar component using the Forge skin.
 * 
 * Can show
 *
 */
@SuppressWarnings("serial")
public class FProgressBar extends JProgressBar {
    private long startMillis = 0, tempMillis = 0;
    private float timePerUnit = 0;
    private int tempVal = 0, etaMillis = 0;
    private int hours, minutes, seconds;
    private String desc = "", count = "", eta = "";
    private boolean showETA = true;
    private boolean showCount = true;

    /** */
    public FProgressBar() {
        super();
        this.reset();
        this.setStringPainted(true);
    }

    /**
     * Sets description on bar. Must be called from EDT.
     * 
     * @param s0 &emsp; A description to prepend before statistics.
     */
    public void setDescription(final String s0) {
        GuiUtils.checkEDT("FProgressBar$setDescription", true);
        this.desc = s0;
        this.setString(s0);
    }

    /** Increments bar, thread safe. Calculations executed on separate thread. */
    public void increment() {
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                tempVal++;
                count = (showCount ? " " + tempVal + " of " + getMaximum() : "");
                eta = (showETA ? calculateETA(tempVal) : "");

                // When calculations finished; EDT can be used.
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        FProgressBar.this.setValue(tempVal);
                        updateString();
                    }
                });
            }
        };

        r.run();
    }

    /** Resets the various values required for this class. Must be called from EDT. */
    public void reset() {
        GuiUtils.checkEDT("FProgressBar$reset", true);
        this.setIndeterminate(true);
        this.setValue(0);
        this.tempVal = 0;
        this.startMillis = new Date().getTime();
        this.setIndeterminate(false);
        this.setShowETA(true);
        this.setShowCount(true);
    }

    /** @param b0 &emsp; Boolean, show the ETA statistic or not */
    public void setShowETA(boolean b0) {
        this.showETA = b0;
    }

    /** @param b0 &emsp; Boolean, show the ETA statistic or not */
    public void setShowCount(boolean b0) {
        this.showCount = b0;
    }

    /** */
    private String calculateETA(int v0) {
        GuiUtils.checkEDT("FProgressBar$calculateETA", false);
        tempMillis = new Date().getTime();
        timePerUnit = (tempMillis - startMillis) / (float) v0;
        etaMillis = (int) ((this.getMaximum() - v0) * timePerUnit) / 1000;

        seconds = etaMillis;
        hours = seconds >= 3600 ? (seconds / 3600) : 0;
        seconds = etaMillis % 3600;
        minutes = seconds >= 60 ? (seconds / 60) : 0;
        seconds = etaMillis % 60 + 1;

        return ", ETA " + String.format("%02d", hours) + ":"
                + String.format("%02d", minutes) + ":"
                + String.format("%02d", seconds);
    }

    private void updateString() {
        this.setString(desc + count + eta);
    }
}
