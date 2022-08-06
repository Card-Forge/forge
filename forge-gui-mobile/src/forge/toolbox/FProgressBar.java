package forge.toolbox;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.gui.interfaces.IProgressBar;
import forge.util.Utils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FProgressBar extends FDisplayObject implements IProgressBar {
    public static Color BACK_COLOR, FORE_COLOR, SEL_BACK_COLOR, SEL_FORE_COLOR;
    private static FSkinFont MSG_FONT;
    private static float TRAIL_INTERVAL = 5000; //complete one trail round every 5 seconds

    private long startTime = 0;
    private int maximum = 0, value = 0;
    private String desc = "";
    private String message;
    private boolean showETA = true;
    private boolean showCount = true;
    private boolean showProgressTrail = false;
    private long progressTrailStart = -1;

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
    public void setValue(int progress) {
        value = progress;
        setShowProgressTrail(false); //can't show progress trail if value set

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

        if (showETA && value > 0) {
            long elapsed = new Date().getTime() - startTime;
            float timePerUnit = elapsed / value;
            int etaSecs = (int) ((float)(maximum - value) * timePerUnit / 1000f);
            sb.append(", ETA").append(String.format("%02d:%02d:%02d", etaSecs / 3600, (etaSecs % 3600) / 60, etaSecs % 60 + 1));
        }
        message = sb.toString();
    }

    /** Resets the various values required for this class. Must be called from EDT. */
    public void reset() {
        value = 0;
        startTime = new Date().getTime();
        setShowETA(true);
        setShowCount(true);
        setShowProgressTrail(false);
    }

    /** @param b0 &emsp; Boolean, show the ETA statistic or not */
    public void setShowETA(boolean b0) {
        showETA = b0;
    }

    /** @param b0 &emsp; Boolean, show the count or not */
    public void setShowCount(boolean b0) {
        showCount = b0;
    }

    /** @param b0 &emsp; Boolean, show the progress trail or not */
    public void setShowProgressTrail(boolean b0) {
        if (showProgressTrail == b0) { return; }
        showProgressTrail = b0;
        progressTrailStart = -1;
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
        List<Pair<Float, Float>> selTextRegions = new ArrayList<>();
        if (showProgressTrail) {
            long now = new Date().getTime();
            if (progressTrailStart == -1) {
                progressTrailStart = now;
            }
            float halfWidth = w / 2;
            float trailPercent = ((now - progressTrailStart) % TRAIL_INTERVAL) / TRAIL_INTERVAL;
            if (trailPercent == 0) {
                g.fillGradientRect(BACK_COLOR, SEL_BACK_COLOR, false, 0, 0, w, h);
                selTextRegions.add(Pair.of(halfWidth, halfWidth));
            }
            else {
                float trailX = trailPercent * w;
                g.startClip(0, 0, w, h);
                g.fillGradientRect(BACK_COLOR, SEL_BACK_COLOR, false, trailX - w, 0, w, h);
                g.fillGradientRect(BACK_COLOR, SEL_BACK_COLOR, false, trailX, 0, w, h);
                g.endClip();
                if (trailX >= halfWidth) {
                    selTextRegions.add(Pair.of(trailX - halfWidth, halfWidth));
                }
                else {
                    selTextRegions.add(Pair.of(0f, trailX));
                    float x = trailX + halfWidth;
                    selTextRegions.add(Pair.of(x, w - x));
                }
            }
        }
        else {
            g.fillRect(BACK_COLOR, 0, 0, w, h);

            float selWidth = Math.round(w * (float)value / (float)maximum);
            if (selWidth > 0) {
                g.fillRect(SEL_BACK_COLOR, 0, 0, selWidth, h);
                selTextRegions.add(Pair.of(0f, selWidth));
            }
        }

        //draw message
        if (MSG_FONT == null) { //must wait to initialize until after FSkin initialized
            MSG_FONT = FSkinFont.get(11);
        }

        g.drawText(message, MSG_FONT, FORE_COLOR, 0, 0, w, h, false, Align.center, true);

        //draw text using selection fore color in needed regions over top of regular text using clipping
        if (!SEL_FORE_COLOR.equals(FORE_COLOR)) {
            for (Pair<Float, Float> region : selTextRegions) {
                g.startClip(region.getLeft(), 0, region.getRight(), h);
                g.drawText(message, MSG_FONT, SEL_FORE_COLOR, 0, 0, w, h, false, Align.center, true);
                g.endClip();
            }
        }

        //draw border
        g.drawRect(Utils.scale(1), Color.BLACK, 0, 0, w, h);
    }
}
