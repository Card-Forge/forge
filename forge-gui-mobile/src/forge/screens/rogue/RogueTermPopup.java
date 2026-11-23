package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Align;
import forge.Forge;
import forge.Graphics;
import forge.gamemodes.rogue.KeywordHint;
import forge.toolbox.FOverlay;

/** What a defined term means. A tap anywhere closes it. */
public class RogueTermPopup extends FOverlay {
    private static final Color CARD = new Color(1f, 0.98f, 0.9f, 1);

    private final KeywordHint hint;

    private RogueTermPopup(KeywordHint hint) {
        super(null);
        this.hint = hint;
    }

    public static void show(KeywordHint hint) {
        if (hint != null) {
            new RogueTermPopup(hint).show();
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
        float u = RogueLook.unit();
        float w = 220 * u;
        float h = RogueLook.wrappedHeight(hint.getHintText(), w - 24 * u, 1) + 34 * u;
        float x = Forge.isLandscapeMode() ? 270 * u : (getWidth() - w) / 2;
        float y = Forge.isLandscapeMode() ? 70 * u : getHeight() / 2 + 20 * u;
        float edge = 1.5f * u;
        g.fillRect(RogueLook.INK, x - edge, y - edge, w + 2 * edge, h + 2 * edge);
        g.fillRect(CARD, x, y, w, h);
        RogueLook.text(g, hint.getTitle(), x + 12 * u, y + 8 * u, 1.2f, RogueLook.DEEP);
        RogueLook.wrap(g, hint.getHintText(), x + 12 * u, y + 26 * u, w - 24 * u, 1, RogueLook.INK, Align.left);
    }
}
