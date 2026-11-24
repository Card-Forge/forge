package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import forge.Forge;
import forge.Graphics;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOverlay;

/** A parchment sheet over the page, rising from the bottom upright and the right sideways. A tap outside closes it. */
public class RogueSheet extends FOverlay {
    private static final Color DIM = new Color(0, 0, 0, 0.6f);

    private final FDisplayObject content;
    private final float uprightHeight;
    private float rise = 1;

    /** uprightHeight is in units. */
    public RogueSheet(FDisplayObject content, float uprightHeight) {
        super(null);
        this.content = add(content);
        this.uprightHeight = uprightHeight;
    }

    @Override
    public void show() {
        super.show();
        RogueMotion.play(0.3f, t -> {
            rise = Interpolation.pow3Out.apply(t);
            revalidate();
        }, null);
    }

    private float[] box() {
        float u = RogueLook.unit();
        float away = 1 - rise;
        // Never taller than the screen, so a short screen such as a tablet's keeps the sheet's top in view
        float h = Math.min(uprightHeight * u, getHeight() - 40 * u);
        return Forge.isLandscapeMode()
            ? new float[]{getWidth() - 300 * u + away * 300 * u, 3 * u, 297 * u, getHeight() - 41 * u}
            : new float[]{3 * u, getHeight() - h - 34 * u + away * (h + 34 * u), getWidth() - 6 * u, h};
    }

    @Override
    protected void doLayout(float width, float height) {
        float u = RogueLook.unit();
        float[] box = box();
        content.setBounds(box[0] + 10 * u, box[1] + 10 * u, box[2] - 20 * u, box[3] - 20 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        g.fillRect(DIM, 0, 0, getWidth(), getHeight());
        float[] box = box();
        RogueLook.paper(g, box[0], box[1], box[2], box[3]);
    }

    @Override
    public boolean tap(float x, float y, int count) {
        float[] box = box();
        if (x < box[0] || x > box[0] + box[2] || y < box[1] || y > box[1] + box[3]) {
            hide();
        }
        return true;
    }
}
