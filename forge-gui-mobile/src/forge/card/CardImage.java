package forge.card;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
    public static TextureRegion croppedBorderImage(Texture image) {
        float rscale = 0.96f;
        int rw = Math.round(image.getWidth()*rscale);
        int rh = Math.round(image.getHeight()*rscale);
        int rx = Math.round((image.getWidth() - rw)/2);
        int ry = Math.round((image.getHeight() - rh)/2)-2;
        TextureRegion rimage = new TextureRegion(image, rx, ry, rw, rh);
        return rimage;
    }
    public static Color borderColor(PaperCard c) {
        if (c == null)
            return Color.valueOf("#1d1d1d");

        CardEdition ed = FModel.getMagicDb().getEditions().get(c.getEdition());
        if (ed != null && ed.isWhiteBorder())
            return Color.valueOf("#fffffd");
        return Color.valueOf("#1d1d1d");
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
        boolean mask = isPreferenceEnabled(ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING);
        if (image == null) { //attempt to retrieve card image if needed
            image = ImageCache.getImage(card);
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
            if (mask) {
                float radius = (h - w)/8;
                g.drawRoundRect(3, borderColor(card), x, y, w, h, radius);
                g.fillRoundRect(borderColor(card), x, y, w, h, radius);
                g.drawImage(croppedBorderImage(image), x+radius/2.2f, y+radius/2, w*0.96f, h*0.96f);
            }
            else
                g.drawImage(image, x, y, w, h);
        }
    }
}
