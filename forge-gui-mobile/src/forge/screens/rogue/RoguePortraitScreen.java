package forge.screens.rogue;

import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.gui.GuiBase;
import forge.toolbox.FDisplayObject;
import forge.util.Localizer;
import java.util.List;

/** Choosing the portrait a commander plays with. */
public class RoguePortraitScreen extends RogueScreen {
    private final String title;
    private final Grid grid;
    private final RogueButton ok;
    private int chosen;

    public RoguePortraitScreen(String commanderName, int current, RogueQuestion<Integer> question) {
        super(null);
        title = Localizer.getInstance().getMessage("lblSelectAvatarFor").replace("%s", commanderName);
        int count = Math.min(126, GuiBase.getInterface().getAvatarCount());
        chosen = current >= 0 && current < count ? current : 0;
        grid = add(new Grid(count));
        ok = add(new RogueButton(Localizer.getInstance().getMessage("lblOK"), null, RogueButton.State.MAIN,
            () -> question.answer(chosen)));
    }

    @Override
    protected float backgroundDim() {
        return 0.25f;
    }

    @Override
    protected List<List<RogueButton>> buttonRows() {
        return List.of(List.of(ok));
    }

    @Override
    protected void layoutBody(float x, float y, float w, float h) {
        float u = RogueLook.unit();
        grid.setBounds(x + 8 * u, y + 32 * u, w - 16 * u, h - 38 * u);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float u = RogueLook.unit();
        float w = getWidth();
        RogueLook.paper(g, 3 * u, 3 * u, w - 6 * u, grid.getBottom() + 3 * u);
        RogueLook.fit(g, title, 15 * u, 11 * u, 1.3f, RogueLook.INK, w - 30 * u, Align.left);
    }

    private class Grid extends RogueScrollPane {
        Grid(int count) {
            for (int i = 0; i < count; i++) {
                int index = i;
                add(new FDisplayObject() {
                    @Override
                    public boolean tap(float x, float y, int c) {
                        chosen = index;
                        return true;
                    }

                    @Override
                    public void draw(Graphics g) {
                        float u = RogueLook.unit();
                        if (index == chosen) {
                            g.fillRect(RogueLook.GOLD, -4 * u, -4 * u, getWidth() + 8 * u, getHeight() + 8 * u);
                        }
                        RogueLook.avatar(g, index, 0, 0, getWidth());
                    }
                });
            }
        }

        @Override
        protected ScrollBounds layoutAndGetScrollBounds(float visibleWidth, float visibleHeight) {
            float u = RogueLook.unit();
            int cols = Forge.isLandscapeMode() ? 11 : 6;
            float gap = 5 * u;
            float s = (visibleWidth - 14 * u - (cols - 1) * gap) / cols;
            float bottom = 0;
            int i = 0;
            for (FDisplayObject child : getChildren()) {
                float y = 5 * u + (i / cols) * (s + gap);
                child.setBounds(7 * u + (i % cols) * (s + gap), y, s, s);
                bottom = y + s;
                i++;
            }
            return new ScrollBounds(visibleWidth, bottom + 5 * u);
        }
    }
}
