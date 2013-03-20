package forge.gui.toolbox;

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
    private long startMillis = 0;
    private int tempVal = 0, etaSecs = 0;
    private String desc = "";
    private String message;
    private boolean showETA = true;
    private boolean showCount = true;
    
    private boolean percentMode = false;

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

    private final Runnable barIncrementor = new Runnable() {
        @Override
        public void run() {
            FProgressBar.this.setValue(tempVal);
            FProgressBar.this.setString(message);
        }
    };
    
    /** Increments bar, thread safe. Calculations executed on separate thread. */
    public void increment() {
        //GuiUtils.checkEDT("FProgressBar$increment", false);
        tempVal++;

        // String.format leads to StringBuilder anyway. Direct calls will be faster
        StringBuilder sb = new StringBuilder(desc);
        if (showCount) {
            sb.append(" ");
            if (percentMode)
                sb.append(100 * tempVal / getMaximum()).append("%");
            else
                sb.append(tempVal).append(" of ").append(getMaximum());
        }

        if (showETA) {
            calculateETA(tempVal);
            sb.append(", ETA").append(String.format("%02d:%02d:%02d", etaSecs / 3600, (etaSecs % 3600) / 60, etaSecs % 60 + 1));
        }
        message = sb.toString();

        // When calculations finished; EDT can be used.
        SwingUtilities.invokeLater(barIncrementor);
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
    private void calculateETA(int v0) {
        float tempMillis = new Date().getTime();
        float timePerUnit = (tempMillis - startMillis) / v0;
        etaSecs = (int) ((this.getMaximum() - v0) * timePerUnit) / 1000;
    }

    public boolean isPercentMode() {
        return percentMode;
    }

    public void setPercentMode(boolean value) {
        this.percentMode = value;
    }

}
