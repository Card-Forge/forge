package forge.toolbox;

import com.badlogic.gdx.graphics.Texture;

import forge.Forge.Graphics;
import forge.assets.ImageCache;
import forge.game.card.Card;
import forge.util.Utils;

public class FCardPanel extends FDisplayObject {
    public static final float TAPPED_ANGLE = -90;
    public static final float ASPECT_RATIO = 3.5f / 2.5f;
    public static final float PADDING = Utils.scaleMin(2);

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

    @Override
    public void draw(Graphics g) {
        if (card == null) { return; }

        float x = PADDING;
        float y = PADDING;
        float w = getWidth() - 2 * PADDING;
        float h = getHeight() - 2 * PADDING;
        if (w == h) { //adjust width if needed to make room for tapping
            w = h / ASPECT_RATIO;
        }

        Texture image = ImageCache.getImage(card);
        if (tapped) {
            float edgeOffset = w / 2f;
            g.drawRotatedImage(image, x, y, w, h, x + edgeOffset, y + h - edgeOffset, tappedAngle);
        }
        else {
            g.drawImage(image, x, y, w, h);
        }
    }
}
