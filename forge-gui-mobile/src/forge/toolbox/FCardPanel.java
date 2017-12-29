package forge.toolbox;

import forge.Graphics;
import forge.card.CardRenderer;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.card.CardView;
import forge.util.Utils;

public class FCardPanel extends FDisplayObject {
    public static final float ASPECT_RATIO = 3.5f / 2.5f;
    public static final float PADDING = Utils.scale(2);
    public static final float TARGET_ORIGIN_FACTOR_X = 0.15f;
    public static final float TARGET_ORIGIN_FACTOR_Y = 0.5f;

    private CardView card;
    private boolean tapped;
    private boolean highlighted;

    public FCardPanel() {
        this(null);
    }
    public FCardPanel(CardView card0) {
        card = card0;
    }

    public CardView getCard() {
        return card;
    }
    public void setCard(CardView card0) {
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

    protected float getTappedAngle() {
        return -90;
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

    //allow overriding stack position
    protected CardStackPosition getStackPosition() {
        return CardStackPosition.Top;
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
            g.startRotateTransform(x + edgeOffset, y + h - edgeOffset, getTappedAngle());
        }

        CardRenderer.drawCardWithOverlays(g, card, x, y, w, h, getStackPosition());

        if (tapped) {
            g.endTransform();
        }
    }

    public String toString() {
        return card == null ? "" : card.toString();
    }
}
