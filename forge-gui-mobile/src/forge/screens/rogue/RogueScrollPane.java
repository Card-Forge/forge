package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import forge.Graphics;
import forge.toolbox.FScrollPane;

/**
 * A scrolling pane with adventure's thin ink scroll bar, and an arrow while there is more below. The arrow stays
 * still because classic screens only redraw on input.
 */
public abstract class RogueScrollPane extends FScrollPane {
    private static final Color BAR = new Color(RogueLook.INK.r, RogueLook.INK.g, RogueLook.INK.b, 0.7f);

    /** On parchment, how far in from the left a band of it starts under the arrow, keeping the arrow off the text; -1 for none. */
    protected float arrowBandInset() {
        return -1;
    }

    @Override
    protected void drawOverlay(Graphics g) {
        float max = getMaxScrollTop();
        if (max <= 0) {
            return;
        }
        float u = RogueLook.unit();
        float h = getHeight();
        float inset = arrowBandInset();
        if (inset >= 0 && getScrollTop() < max - u) {
            g.fillRect(RogueLook.PAPER, inset, h - 10 * u, getWidth() - inset - 5 * u, 10 * u);
        }
        float knob = Math.max(8 * u, h * h / getScrollHeight());
        float y = (h - knob) * getScrollTop() / max;
        g.fillRect(BAR, getWidth() - 3 * u, y, 1.5f * u, knob);
        if (getScrollTop() < max - u) {
            RogueLook.more(g, getWidth() / 2, h - 7 * u);
        }
    }
}
