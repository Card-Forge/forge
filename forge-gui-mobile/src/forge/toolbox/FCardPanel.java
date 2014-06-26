package forge.toolbox;

import com.badlogic.gdx.math.Vector2;

import forge.Graphics;
import forge.card.CardRenderer;
import forge.game.card.Card;
import forge.util.Utils;

public class FCardPanel extends FDisplayObject {
    public static final float TAPPED_ANGLE = -90;
    public static final float ASPECT_RATIO = 3.5f / 2.5f;
    public static final float PADDING = Utils.scaleMin(2);
    public static final float TARGET_ORIGIN_FACTOR_X = 0.15f;
    public static final float TARGET_ORIGIN_FACTOR_Y = 0.5f;

    private Card card;
    private boolean tapped;
    private float tappedAngle = 0;
    private boolean highlighted;

    public FCardPanel() {
        this(null);
    }
    public FCardPanel(Card card0) {
        card = card0;
    }

    public Card getCard() {
        return card;
    }
    public void setCard(Card card0) {
        card = card0;
    }

    public boolean isHighlighted() {
        return highlighted;
    }
    public void setHighlighted(boolean highlighted0) {
        highlighted = highlighted0;
    }

    public boolean isTapped() {
        return tapped;
    }
    public void setTapped(final boolean tapped0) {
        tapped = tapped0;
    }

    public float getTappedAngle() {
        return tappedAngle;
    }
    public void setTappedAngle(float tappedAngle0) {
        tappedAngle = tappedAngle0;
    }

    protected boolean renderedCardContains(float x, float y) {
        float left = PADDING;
        float top = PADDING;
        float w = getWidth() - 2 * PADDING;
        float h = getHeight() - 2 * PADDING;
        if (w == h) { //adjust width if needed to make room for tapping
            w = h / ASPECT_RATIO;
        }

        if (tapped) { //rotate box if tapped
            top += h - w;
            float temp = w;
            w = h;
            h = temp;
        }

        return x >= left && x <= left + w && y >= top && y <= top + h;
    }

    protected float getPadding() {
        return PADDING;
    }

    public Vector2 getTargetingArrowOrigin() {
        Vector2 origin = new Vector2(getScreenPosition());

        float left = PADDING;
        float top = PADDING;
        float w = getWidth() - 2 * PADDING;
        float h = getHeight() - 2 * PADDING;
        if (w == h) { //adjust width if needed to make room for tapping
            w = h / ASPECT_RATIO;
        }

        if (tapped) { //rotate box if tapped
            top += h - w;
            float temp = w;
            w = h;
            h = temp;
        }

        origin.x += left + w * TARGET_ORIGIN_FACTOR_X;
        origin.y += top + h * TARGET_ORIGIN_FACTOR_Y;

        return origin;
    }

    @Override
    public void draw(Graphics g) {
        if (card == null) { return; }

        float padding = getPadding();
        float x = padding;
        float y = padding;
        float w = getWidth() - 2 * padding;
        float h = getHeight() - 2 * padding;
        if (w == h) { //adjust width if needed to make room for tapping
            w = h / ASPECT_RATIO;
        }

        if (tapped) {
            float edgeOffset = w / 2f;
            g.setRotateTransform(x + edgeOffset, y + h - edgeOffset, tappedAngle);
        }

        CardRenderer.drawCardWithOverlays(g, card, x, y, w, h);

        if (tapped) {
            g.clearTransform();
        }
    }
}
