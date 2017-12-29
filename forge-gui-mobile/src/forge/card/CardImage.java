package forge.card;

import com.badlogic.gdx.graphics.Texture;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.ImageCache;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.card.CardView;
import forge.item.PaperCard;
import forge.toolbox.FCardPanel;

public class CardImage implements FImage {
    private final PaperCard card;
    private Texture image;

    public CardImage(PaperCard card0) {
        card = card0;
    }

    @Override
    public float getWidth() {
        if (image != null) {
            return image.getWidth();
        }
        return ImageCache.defaultImage.getWidth();
    }

    @Override
    public float getHeight() {
        return getWidth() * FCardPanel.ASPECT_RATIO;
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        if (image == null) { //attempt to retrieve card image if needed
            image = ImageCache.getImage(card);
            if (image == null) {
                return; //can't draw anything if can't be loaded yet
            }
        }

        if (image == ImageCache.defaultImage) {
            CardImageRenderer.drawCardImage(g, CardView.getCardForUi(card), false, x, y, w, h, CardStackPosition.Top);
        }
        else {
            g.drawImage(image, x, y, w, h);
        }
    }
}
