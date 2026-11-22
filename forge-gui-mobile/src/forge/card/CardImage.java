package forge.card;

import com.badlogic.gdx.graphics.Texture;

import forge.Forge;
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

    private CardView cachedCardViewInstance = null;

    public CardImage(PaperCard card0) {
        this.card = card0;
    }

    @Override
    public float getWidth() {
        if (image != null) {
            return image.getWidth();
        }
        return ImageCache.getInstance().getDefaultImage().getWidth();
    }

    @Override
    public float getHeight() {
        return getWidth() * FCardPanel.ASPECT_RATIO;
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        if (cachedCardViewInstance == null && card != null) {
            cachedCardViewInstance = CardView.getCardForUi(card);
        }

        final CardView cv = cachedCardViewInstance;
        if (cv == null) return; // Safeguard branch

        final String currentUiMask = Forge.enableUIMask != null ? Forge.enableUIMask : "Off";
        final boolean isOffMask = currentUiMask.equals("Off");
        final boolean isArtMask = currentUiMask.equals("Art");

        if (image == null) { // attempt to retrieve card image if needed
            image = ImageCache.getInstance().getImage(card);
            if (image == null) {
                if (!isOffMask) { // render this if mask is still loading
                    CardImageRenderer.drawCardImage(g, cv, false, x, y, w, h, CardStackPosition.Top, (isArtMask || cv.useCardArt()), true);
                }
                return; // can't draw anything if can't be loaded yet
            }
        }

        if (image == ImageCache.getInstance().getDefaultImage() || (isArtMask || cv.useCardArt())) {
            CardImageRenderer.drawCardImage(g, cv, false, x, y, w, h, CardStackPosition.Top, true, true);
        } else {
            switch (currentUiMask) {
                case "Full":
                    g.drawCardRoundRect(image, null, x, y, w, h, false, false, 0);
                    break;
                case "Crop":
                    g.drawImage(ImageCache.getInstance().croppedBorderImage(image), x, y, w, h);
                    break;
                default:
                    g.drawImage(image, x, y, w, h);
                    break;
            }
        }
    }
}
