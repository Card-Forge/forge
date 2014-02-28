package forge.toolbox;

import java.util.Date;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.Forge.Graphics;
import forge.assets.FSkinFont;

public class FProgressBar extends FDisplayObject {
    public static Color BACK_COLOR, FORE_COLOR, SEL_BACK_COLOR, SEL_FORE_COLOR;
    private static FSkinFont MSG_FONT;

    private long startMillis = 0;
    private int etaSecs = 0, maximum = 0, value = 0;
    private String desc = "";
    private String message;
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
        message = s0;
    }

    /** Increments bar. */
    public void setValue(int value0) {
        value = value0;

        // String.format leads to StringBuilder anyway. Direct calls will be faster
        StringBuilder sb = new StringBuilder(desc);
        if (showCount) {
            sb.append(" ");
            if (percentMode) {
                sb.append(100 * value / maximum).append("%");
            }
            else {
                sb.append(value).append(" of ").append(maximum);
            }
        }

        if (showETA) {
            calculateETA(value);
            sb.append(", ETA").append(String.format("%02d:%02d:%02d", etaSecs / 3600, (etaSecs % 3600) / 60, etaSecs % 60 + 1));
        }
        message = sb.toString();
    }

    /** Resets the various values required for this class. Must be called from EDT. */
    public void reset() {
        value = 0;
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

        //draw background and progress
        g.fillRect(BACK_COLOR, 0, 0, w, h);
        float selWidth = w * (float)value / (float)maximum;
        if (selWidth > 0) {
            g.fillRect(SEL_BACK_COLOR, 0, 0, selWidth, h);
        }

        //draw message
        if (MSG_FONT == null) { //must wait to initialize until after FSkin initialized
            MSG_FONT = FSkinFont.get(11);
        }
        g.drawText(message, MSG_FONT, FORE_COLOR, 0, 0, w, h, false, HAlignment.CENTER, true);
        if (selWidth > 0 && !SEL_FORE_COLOR.equals(FORE_COLOR)) {
            g.startClip(0, 0, selWidth, h);
            g.drawText(message, MSG_FONT, SEL_FORE_COLOR, 0, 0, w, h, false, HAlignment.CENTER, true);
            g.endClip();
        }

        //draw border
        g.drawRect(Color.BLACK, 0, 0, w, h);
    }
}
