package forge.toolbox;

import forge.FThreads;
import forge.GuiBase;
import forge.interfaces.IProgressBar;

import javax.swing.*;

import java.util.Date;

/** 
 * A simple progress bar component using the Forge skin.
 * 
 * Can show
 *
 */
@SuppressWarnings("serial")
public class FProgressBar extends JProgressBar implements IProgressBar {
    private long startMillis = 0;
    private int tempVal = 0, etaSecs = 0;
    private String desc = "";
    private boolean showETA = true;
    private boolean showCount = true;
    
    private boolean percentMode = false;

    /** */
    public FProgressBar() {
        super();
        reset();
        setStringPainted(true);
    }

    /**
     * Sets description on bar. Must be called from EDT.
     * 
     * @param s0 &emsp; A description to prepend before statistics.
     */
    public void setDescription(final String s0) {
        FThreads.assertExecutedByEdt(GuiBase.getInterface(), true);
        desc = s0;
        setString(s0);
    }
    
    /** Increments bar, thread safe. Calculations executed on separate thread. */
    public void setValueThreadSafe(int value) {
        tempVal = value;
        SwingUtilities.invokeLater(barIncrementor);
    }

    private final Runnable barIncrementor = new Runnable() {
        @Override
        public void run() {
            setValue(tempVal);
            tempVal = 0;
        }
    };

    @Override
    public void setValue(int value0) {
        super.setValue(value0);

        // String.format leads to StringBuilder anyway. Direct calls will be faster
        StringBuilder sb = new StringBuilder(desc);
        if (showCount) {
            sb.append(" ");
            int maximum = getMaximum();
            if (percentMode) {
                sb.append(100 * value0 / maximum).append("%");
            }
            else {
                sb.append(value0).append(" of ").append(maximum);
            }
        }

        if (showETA) {
            calculateETA(value0);
            sb.append(", ETA").append(String.format("%02d:%02d:%02d", etaSecs / 3600, (etaSecs % 3600) / 60, etaSecs % 60 + 1));
        }
        setString(sb.toString());
    }

    /** Resets the various values required for this class. Must be called from EDT. */
    public void reset() {
        FThreads.assertExecutedByEdt(GuiBase.getInterface(), true);
        setIndeterminate(true);
        setValue(0);
        tempVal = 0;
        startMillis = new Date().getTime();
        setIndeterminate(false);
        setShowETA(true);
        setShowCount(true);
    }

    /** @param b0 &emsp; Boolean, show the ETA statistic or not */
    public void setShowETA(boolean b0) {
        showETA = b0;
    }

    /** @param b0 &emsp; Boolean, show the ETA statistic or not */
    public void setShowCount(boolean b0) {
        showCount = b0;
    }

    /** */
    private void calculateETA(int v0) {
        float tempMillis = new Date().getTime();
        float timePerUnit = (tempMillis - startMillis) / v0;
        etaSecs = (int) ((getMaximum() - v0) * timePerUnit) / 1000;
    }

    public boolean isPercentMode() {
        return percentMode;
    }

    public void setPercentMode(boolean value) {
        percentMode = value;
    }

}
