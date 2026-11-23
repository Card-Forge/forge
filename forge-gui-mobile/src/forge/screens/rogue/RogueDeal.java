package forge.screens.rogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Interpolation;
import forge.Graphics;
import forge.item.PaperCard;

/** Cards dealt face down from below and turned over, one after another. A rare card drops in from above onto a halo. */
final class RogueDeal {
    private static final Color BACK = new Color(0x1d2640ff);
    private static final Color STRIPE = new Color(0x26304fff);
    private static final float MOVE = 0.5f;
    private static final float TURN = 0.35f;
    private static final float HALO = 0.9f;

    private final float step;
    private final float total;
    private float time = Float.MAX_VALUE;

    RogueDeal(int count) {
        step = Math.min(0.1f, 1.1f / Math.max(1, count));
        total = MOVE + TURN + HALO + step * count;
    }

    void start() {
        RogueMotion.play(total, t -> time = t * total, null);
    }

    boolean arrived(int i) {
        return time - i * step >= MOVE + TURN;
    }

    /** Draws card i, not yet arrived, on its way to its place 0, 0, w by h from ox, oy in the same coordinates. */
    void draw(Graphics g, int i, PaperCard card, float w, float h, float ox, float oy, boolean rare) {
        float t = time - i * step;
        if (t <= 0) {
            return;
        }
        float move = Math.min(1, t / MOVE);
        float dx = rare ? 0 : (ox - w / 2) * (1 - Interpolation.pow3Out.apply(move));
        float dy = rare ? -h * (1 - Interpolation.swingOut.apply(move)) : oy * (1 - Interpolation.pow3Out.apply(move));
        float turn = Math.max(0, Math.min(1, (t - MOVE) / TURN));
        float k = Math.abs(1 - 2 * turn);
        float kw = Math.max(w * k, 1);
        float kx = dx + (w - kw) / 2;
        if (turn < 0.5f) {
            back(g, kx, dy, kw, h);
        } else {
            RogueLook.card(g, card, kx, dy, kw, h, 1);
        }
    }

    /** A rare card's halo, pulsing as it lands and fading after; drawn behind the card. */
    void halo(Graphics g, int i, float w, float h) {
        float t = time - i * step - MOVE * 0.5f;
        float span = MOVE + TURN + HALO;
        if (t <= 0 || t >= span) {
            return;
        }
        float fade = Math.min(1, t / 0.2f) * Math.min(1, (span - t) / 0.4f);
        RogueLook.halo(g, w / 2, h / 2, Math.max(w, h) * 0.6f, t, fade);
    }

    private static void back(Graphics g, float x, float y, float w, float h) {
        float u = RogueLook.unit();
        g.fillRect(BACK, x, y, w, h);
        for (float s = 0; s < h; s += 6 * u) {
            g.fillRect(STRIPE, x, y + s, w, 3 * u);
        }
        RogueLook.box(g, x, y, w, h, 1.5f, RogueLook.GOLD);
    }
}
