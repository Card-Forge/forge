package forge.card;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.ImageCache;
import forge.item.PaperCard;

public class CardAvatarImage implements FImage {
    private final String imageKey;
    private FImage image;

    public CardAvatarImage(PaperCard card0) {
        this(card0.getImageKey(false));
    }
    public CardAvatarImage(String imageKey0) {
        imageKey = imageKey0;
    }

    @Override
    public float getWidth() {
        return getHeight(); //image will be drawn at its height
    }

    @Override
    public float getHeight() {
        if (image != null) {
            return image.getHeight();
        }
        return ImageCache.defaultImage.getHeight() * CardRenderer.CARD_ART_HEIGHT_PERCENTAGE;
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        //force to get the avatar since the the cardartcache & loadingcache is always cleared on screen change or the battle bar will display black
        image = CardRenderer.getCardArt(imageKey, false, false, false, false, false, false, false, false, true);
        if (image == null) {
            return; //can't draw anything if can't be loaded yet
        }

        //draw scaled image into clipped region so it fills box while maintain aspect ratio
        g.startClip(x, y, w, h);

        float aspectRatio = w / h;
        float imageAspectRatio = image.getWidth() / image.getHeight();
        if (imageAspectRatio > aspectRatio) {
            float w0 = w * imageAspectRatio / aspectRatio;
            x -= (w0 - w) / 2;
            w = w0;
        }
        else {
            float h0 = h * aspectRatio / imageAspectRatio;
            y -= (h0 - h) / 2;
            h = h0;
        }

        image.draw(g, x, y, w, h);

        g.endClip();
    }
}
