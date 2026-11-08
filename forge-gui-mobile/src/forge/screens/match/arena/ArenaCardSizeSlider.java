package forge.screens.match.arena;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.toolbox.FDisplayObject;

/**
 * A small slider under the corner buttons of the Arena board: how large the cards are, the battlefield's tiles and
 * the hand alike, from {@link ArenaBoard#CARD_SCALE_MIN} to full size. It can be dragged (or clicked, or turned with
 * the wheel) at any time; the board follows at once and the size is kept for the next match. A small card at the
 * left end and a large one at the right say what it does; while the pointer is on it a plate gives the percentage.
 */
public final class ArenaCardSizeSlider extends FDisplayObject {
    private static final Color S1 = new Color(), S2 = new Color();
    private final Runnable relayout;
    private final ArenaMotion.Fade hot = new ArenaMotion.Fade(0f);
    private boolean dragging;
    private String label;
    private int labelFor = -1;

    /** {@code relayout} lays the board out again after the size changed. */
    public ArenaCardSizeSlider(final Runnable relayout0) {
        relayout = relayout0;
    }

    /** The track's ends in local x: inside the two card icons. */
    private float trackLeft() {
        return getHeight() * 0.95f;
    }

    private float trackRight() {
        return getWidth() - getHeight() * 1.05f;
    }

    private void setFrom(final float x) {
        final float t = ArenaMotion.clamp01((x - trackLeft()) / Math.max(1f, trackRight() - trackLeft()));
        apply(ArenaBoard.CARD_SCALE_MIN + t * (1f - ArenaBoard.CARD_SCALE_MIN), false);
    }

    private void apply(final float scale, final boolean save) {
        if (ArenaBoard.setCardScale(scale, save)) {
            relayout.run();
            Gdx.graphics.requestRendering();
        }
    }

    @Override
    public boolean press(final float x, final float y) {
        dragging = true;
        setFrom(x);
        return true;
    }

    @Override
    public boolean pan(final float x, final float y, final float deltaX, final float deltaY, final boolean moreVertical) {
        if (!Gdx.input.isTouched()) { // the mouse wheel: a step of 5% a notch
            if (deltaY != 0f) {
                apply(ArenaBoard.cardScale() - Math.signum(deltaY) * 0.05f, true); // as the hand: up walks right
            }
            return true;
        }
        dragging = true;
        setFrom(x);
        return true;
    }

    @Override
    public boolean panStop(final float x, final float y) {
        return release(x, y);
    }

    @Override
    public boolean release(final float x, final float y) {
        dragging = false;
        ArenaBoard.saveCardScale();
        return true;
    }

    @Override
    public boolean tap(final float x, final float y, final int count) {
        setFrom(x);
        ArenaBoard.saveCardScale();
        return true;
    }

    @Override
    public void draw(final Graphics g) {
        final float w = getWidth(), h = getHeight();
        final float mx = Forge.mouseMovedX - screenPos.x, my = Forge.mouseMovedY - screenPos.y;
        final boolean over = Forge.magnify && mx >= 0 && my >= 0 && mx < w && my < h;
        final boolean lit = over || dragging;
        if (hot.goal() != (lit ? 1f : 0f)) {
            hot.to(lit ? 1f : 0f, ArenaMotion.HOVER_MS);
        }
        final float t = hot.get();
        // the plate it sits on, like the corner buttons'
        g.drawSoftShadow(ArenaTheme.alpha(ArenaTheme.BLACK, 0.45f, S1), 0, ArenaTheme.px(1), w, h, h / 2f, ArenaTheme.px(4));
        g.fillSmoothRoundRect(ArenaTheme.rgbaInto(6, 8, 14, ArenaMotion.lerp(170, 235, t), S1), 0, 0, w, h, h / 2f);
        g.drawSmoothRoundRect(Math.max(1f, ArenaTheme.px(1.2f)), ArenaTheme.alpha(ArenaTheme.WHITE, ArenaMotion.lerp(90, 200, t) / 255f, S1), 0, 0, w, h, h / 2f);
        // a small card on the left, a large one on the right
        final Color icon = ArenaTheme.alpha(ArenaTheme.WHITE, ArenaMotion.lerp(150, 255, t) / 255f, S2);
        final float line = Math.max(1f, ArenaTheme.px(1.2f));
        final float sh = h * 0.34f, lh = h * 0.56f;
        g.drawSmoothRoundRect(line, icon, h * 0.5f - sh * 0.36f, (h - sh) / 2f, sh * 0.72f, sh, sh * 0.12f);
        g.drawSmoothRoundRect(line, icon, w - h * 0.55f - lh * 0.36f, (h - lh) / 2f, lh * 0.72f, lh, lh * 0.12f);
        // the track, gold up to the knob
        final float l = trackLeft(), r = trackRight(), cy = h / 2f, th = Math.max(2f, Math.round(h * 0.12f));
        final float k = (ArenaBoard.cardScale() - ArenaBoard.CARD_SCALE_MIN) / (1f - ArenaBoard.CARD_SCALE_MIN);
        final float kx = l + (r - l) * ArenaMotion.clamp01(k);
        g.fillSmoothRoundRect(ArenaTheme.alpha(ArenaTheme.WHITE, 0.22f, S1), l, cy - th / 2f, r - l, th, th / 2f);
        g.fillSmoothRoundRect(ArenaTheme.GOLD, l, cy - th / 2f, Math.max(th, kx - l), th, th / 2f);
        final float kr = h * (0.2f + 0.04f * t);
        g.fillSmoothCircle(ArenaTheme.GOLD_BRIGHT, kx, cy, kr);
        g.drawSmoothCircle(line, ArenaTheme.alpha(ArenaTheme.BLACK, 0.6f, S1), kx, cy, kr);
        if (lit) { // under it, what it is and how large the cards are now
            final int pct = Math.round(ArenaBoard.cardScale() * 100f);
            if (pct != labelFor || label == null) {
                labelFor = pct;
                label = Forge.getLocalizer().getMessage("lblArenaCardSize", String.valueOf(pct));
            }
            final FSkinFont f = ArenaCards.evenFont(Math.max(ArenaTheme.px(16), h * 0.5f));
            ArenaArt.hint(g, label, f, w / 2f, h + ArenaTheme.px(6), -screenPos.x, ArenaBoard.windowW() - screenPos.x);
        }
    }
}
