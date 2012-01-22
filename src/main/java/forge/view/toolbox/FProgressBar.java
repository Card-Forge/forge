package forge.view.toolbox;


import java.util.Date;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/** 
 * A simple progress bar component using the Forge skin.
 * 
 * Can show
 *
 */
public class FProgressBar extends JProgressBar {
    private static final long serialVersionUID = 3479715871723156426L;
    private int tempVal = 0;
    private long startMillis = 0;
    private long tempMillis = 0;
    private float timePerUnit = 0;
    private int eta = 0;
    private boolean isIncrementing = false;
    private int hours, minutes, seconds;
    private String desc = "";
    private String str = "";
    private boolean showETA = true;
    private boolean showCount = true;

    /** */
    public FProgressBar() {
        super();
        this.reset();
        this.setStringPainted(true);
    }

    /** @param s0 &emsp; A description to prepend before statistics. */
    public void setDescription(final String s0) {
        this.desc = s0;
        this.setString(s0);
    }

    /** */
    public void increment() {
        if (isIncrementing) { System.out.println("Rejected."); return; }

        isIncrementing = true;
        tempVal++;
        this.setValue(tempVal);
        str = desc;
        if (showCount) { calculateCount(tempVal); }
        if (showETA) { calculateETA(tempVal); }
        updateString();
        isIncrementing = false;
    }

    private void calculateCount(int v0) {
        str += " " + v0 + " of " + this.getMaximum();
    }

    /** */
    private void calculateETA(int v0) {
        tempMillis = new Date().getTime();
        timePerUnit = (tempMillis - startMillis) / (float) v0;
        eta = (int) ((this.getMaximum() - v0) * timePerUnit) / 1000;

        seconds = eta;
        hours = seconds >= 3600 ? (seconds / 3600) : 0;
        seconds = eta % 3600;
        minutes = seconds >= 60 ? (seconds / 60) : 0;
        seconds = eta % 60 + 1;

        str += ", ETA " + String.format("%02d", hours) + ":"
                + String.format("%02d", minutes) + ":"
                + String.format("%02d", seconds);
    }

    private void updateString() {
        this.setString(str);
    }

    /** Resets the various values required for this class. */
    public void reset() {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException(
                    "FProgressBar > reset() must be accessed from an event dispatch thread.");
        }

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

    /** @return b0 &emsp; Boolean, show the ETA statistic or not */
    public boolean isShowETA() {
        return showETA;
    }

    /** @param b0 &emsp; Boolean, show the ETA statistic or not */
    public void setShowCount(boolean b0) {
        this.showCount = b0;
    }

    /** @return b0 &emsp; Boolean, show the ETA statistic or not */
    public boolean isShowCount() {
        return showCount;
    }
}
