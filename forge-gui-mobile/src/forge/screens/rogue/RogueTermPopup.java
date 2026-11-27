package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;
import forge.Graphics;
import forge.toolbox.FOverlay;

/** A short explanation, such as what a defined term means, beside where it was tapped. A tap anywhere closes it. */
public class RogueTermPopup extends FOverlay {
    private static final Color CARD = new Color(1f, 0.98f, 0.9f, 1);

    private final String title;
    private final String text;
    private final float px;
    private final float py;

    private RogueTermPopup(String title, String text, float px, float py) {
        super(null);
        this.title = title;
        this.text = text;
        this.px = px;
        this.py = py;
    }

    /** title may be null. px and py are the screen point it opens from. */
    public static void show(String title, String text, float px, float py) {
        if (text != null) {
            new RogueTermPopup(title, text, px, py).show();
        }
    }

    @Override
    protected void doLayout(float width, float height) {
    }

    @Override
    public boolean tap(float x, float y, int count) {
        hide();
        return true;
    }

    @Override
    protected void drawOverlay(Graphics g) {
        draw(g, title, text, px, py, getWidth(), getHeight());
    }

    /** The explanation below and right of the point, or on whichever side keeps it inside the area. title may be null. */
    static void draw(Graphics g, String title, String text, float px, float py, float areaW, float areaH) {
        float u = RogueLook.unit();
        float w = Math.min(220 * u, areaW - 8 * u);
        float textTop = title == null ? 8 * u : 26 * u;
        float h = RogueLook.wrappedHeight(text, w - 24 * u, 1) + textTop + 8 * u;
        float gap = 10 * u;
        float x = px + gap + w > areaW ? px - gap - w : px + gap;
        float y = py + gap + h > areaH ? py - gap - h : py + gap;
        x = MathUtils.clamp(x, 4 * u, areaW - w - 4 * u);
        y = MathUtils.clamp(y, 4 * u, areaH - h - 4 * u);
        float edge = 1.5f * u;
        g.fillRect(RogueLook.INK, x - edge, y - edge, w + 2 * edge, h + 2 * edge);
        g.fillRect(CARD, x, y, w, h);
        if (title != null) {
            RogueLook.text(g, title, x + 12 * u, y + 8 * u, 1.2f, RogueLook.DEEP);
        }
        RogueLook.wrap(g, text, x + 12 * u, y + textTop, w - 24 * u, 1, RogueLook.INK, Align.left);
    }
}
