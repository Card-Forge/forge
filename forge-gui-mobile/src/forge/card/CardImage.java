package forge.card;

import com.badlogic.gdx.graphics.Texture;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.ImageCache;
import forge.card.CardRenderer.CardStackPosition;
import forge.game.card.CardView;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgePreferences;
import forge.toolbox.FCardPanel;

public class CardImage implements FImage {
    private final PaperCard card;
    private Texture image;

    public CardImage(PaperCard card0) {
        card = card0;
    }
    private static boolean isPreferenceEnabled(ForgePreferences.FPref preferenceName) {
        return FModel.getPreferences().getPrefBoolean(preferenceName);
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
            boolean mask = isPreferenceEnabled(ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING);
            image = ImageCache.getImage(card, mask);
            if (image == null) {
                if (mask) //render this if mask is still loading
                    CardImageRenderer.drawCardImage(g, CardView.getCardForUi(card), false, x, y, w, h, CardStackPosition.Top);

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
