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

    @Override
    protected void drawOverlay(Graphics g) {
        float max = getMaxScrollTop();
        if (max <= 0) {
            return;
        }
        float u = RogueLook.unit();
        float h = getHeight();
        float knob = Math.max(8 * u, h * h / getScrollHeight());
        float y = (h - knob) * getScrollTop() / max;
        g.fillRect(BAR, getWidth() - 3 * u, y, 1.5f * u, knob);
        if (getScrollTop() < max - u) {
            RogueLook.more(g, getWidth() / 2, h - 7 * u);
        }
    }
}
