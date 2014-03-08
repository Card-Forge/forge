package forge.toolbox;

import forge.Forge.Graphics;
import forge.assets.ImageCache;
import forge.game.card.Card;

public class FCardPanel extends FDisplayObject {
    public static final float ASPECT_RATIO = 3.5f / 2.5f;
    public static final float PADDING = 3; //scale to leave vertical space between

    private final Card card;

    public FCardPanel(Card card0) {
        card = card0;
    }

    @Override
    public void draw(Graphics g) {
        float x = PADDING;
        float y = PADDING;
        float w = getWidth() - 2 * PADDING;
        float h = getHeight() - 2 * PADDING;
        if (w == h) { //adjust width if needed to make room for tapping
            w = h / ASPECT_RATIO;
        }

        g.drawImage(ImageCache.getImage(card), x, y, w, h);
    }
}
