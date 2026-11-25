package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import forge.Forge;
import forge.Graphics;
import forge.toolbox.FOverlay;
import java.util.ArrayList;
import java.util.List;

/** The run menu: upright it opens upward from the run bar, sideways upward from the menu button. */
public class RogueRunMenu extends FOverlay {
    public record Item(String label, Runnable command) { }

    private static final Color DIM = new Color(0, 0, 0, 0.35f);

    private final List<RogueButton> buttons = new ArrayList<>();
    private final float barTop;

    private RogueRunMenu(List<Item> items, float barTop) {
        super(null);
        this.barTop = barTop;
        for (Item item : items) {
            buttons.add(add(new RogueButton(item.label(), null, RogueButton.State.NORMAL, () -> {
                hide();
                item.command().run();
            })));
        }
    }

    /** barTop is where the upright run bar starts. */
    public static void show(List<Item> items, float barTop) {
        if (!items.isEmpty()) {
            new RogueRunMenu(items, barTop).show();
        }
    }

    private float[] box() {
        float u = RogueLook.unit();
        float w = 150 * u;
        float h = (buttons.size() * 31 + 16) * u;
        float x = Forge.isLandscapeMode() ? 3 * u : getWidth() - w - 3 * u;
        float y = Forge.isLandscapeMode() ? getHeight() - 36 * u - h : barTop - h - 2 * u;
        return new float[]{x, y, w, h};
    }

    @Override
    protected void doLayout(float width, float height) {
        float u = RogueLook.unit();
        float[] box = box();
        for (int i = 0; i < buttons.size(); i++) {
            buttons.get(i).setBounds(box[0] + 8 * u, box[1] + (8 + i * 31) * u, box[2] - 16 * u, 30 * u);
        }
    }

    @Override
    protected void drawBackground(Graphics g) {
        g.fillRect(DIM, 0, 0, getWidth(), getHeight());
        float[] box = box();
        RogueLook.paper(g, box[0], box[1], box[2], box[3]);
    }

    @Override
    public boolean tap(float x, float y, int count) {
        hide();
        return true;
    }
}
