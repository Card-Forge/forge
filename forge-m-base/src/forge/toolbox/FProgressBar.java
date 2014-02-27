package forge.toolbox;

import java.util.Date;

import com.badlogic.gdx.graphics.Color;

import forge.Forge.Graphics;

public class FProgressBar extends FDisplayObject {
    public static Color BACK_COLOR, FORE_COLOR, SEL_BACK_COLOR, SEL_FORE_COLOR;

    private long startMillis = 0;
    private int tempVal = 0, etaSecs = 0, maximum = 0, value = 0;
    private String desc = "";
    private String tempMsg, message;
    private boolean showETA = true;
    private boolean showCount = true;
    
    private boolean percentMode = false;

    /** */
    public FProgressBar() {
        super();
        reset();
    }

    /**
     * Sets description on bar.
     * 
     * @param s0 &emsp; A description to prepend before statistics.
     */
    public void setDescription(final String s0) {
        desc = s0;
    }

    private final Runnable barIncrementor = new Runnable() {
        @Override
        public void run() {
            value = tempVal;
            message = tempMsg;
        }
    };

    /** Increments bar, thread safe. Calculations executed on separate thread. */
    public void setValueThreadSafe(int value) {
        //GuiUtils.checkEDT("FProgressBar$increment", false);
        tempVal = value;

        // String.format leads to StringBuilder anyway. Direct calls will be faster
        StringBuilder sb = new StringBuilder(desc);
        if (showCount) {
            sb.append(" ");
            if (percentMode)
                sb.append(100 * tempVal / maximum).append("%");
            else
                sb.append(tempVal).append(" of ").append(maximum);
        }

        if (showETA) {
            calculateETA(tempVal);
            sb.append(", ETA").append(String.format("%02d:%02d:%02d", etaSecs / 3600, (etaSecs % 3600) / 60, etaSecs % 60 + 1));
        }
        tempMsg = sb.toString();

        // When calculations finished; EDT can be used.
        //SwingUtilities.invokeLater(barIncrementor);
        barIncrementor.run();
    }

    /** Resets the various values required for this class. Must be called from EDT. */
    public void reset() {
        //FThreads.assertExecutedByEdt(true);
        startMillis = new Date().getTime();
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
        etaSecs = (int) ((maximum - v0) * timePerUnit) / 1000;
    }

    public boolean isPercentMode() {
        return percentMode;
    }

    public void setPercentMode(boolean percentMode0) {
        percentMode = percentMode0;
    }
    
    public int getMaximum() {
        return maximum;
    }
    
    public void setMaximum(int maximum0) {
        maximum = maximum0;
    }

    @Override
    public void draw(Graphics g) {
        float w = getWidth();
        float h = getHeight();
        g.fillRect(BACK_COLOR, 0, 0, w, h);
        g.drawRect(Color.BLACK, 0, 0, w, h);
    }
}
