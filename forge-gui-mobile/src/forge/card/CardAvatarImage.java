package forge.card;

import forge.Graphics;
import forge.assets.FImage;
import forge.assets.ImageCache;
import forge.item.PaperCard;

public class CardAvatarImage implements FImage {
    private final String imageKey;
    // crop offset along the slack axis: 0 = left/top, 1000 = right/bottom, 500 = centre
    private final int cropOffset;
    private FImage image;

    public CardAvatarImage(PaperCard card0) {
        this(card0.getImageKey(false));
    }
    public CardAvatarImage(String imageKey0) {
        this(imageKey0, 500);
    }
    public CardAvatarImage(String imageKey0, int cropOffset0) {
        imageKey = imageKey0;
        cropOffset = Math.max(0, Math.min(1000, cropOffset0));
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
        return ImageCache.getInstance().getDefaultImage().getHeight() * CardRenderer.CARD_ART_HEIGHT_PERCENTAGE;
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        //force to get the avatar since the the cardartcache & loadingcache is always cleared on screen change or the battle bar will display black
        image = CardRenderer.getCardArt(imageKey, false, false, false, false, false, false, false, false, true, false);
        if (image == null) {
            return; //can't draw anything if can't be loaded yet
        }

        //draw scaled image into clipped region so it fills box while maintain aspect ratio
        g.startClip(x, y, w, h);

        float aspectRatio = w / h;
        float imageAspectRatio = image.getWidth() / image.getHeight();
        float f = cropOffset / 1000f;
        if (imageAspectRatio > aspectRatio) {
            float w0 = w * imageAspectRatio / aspectRatio;
            x -= (w0 - w) * f;
            w = w0;
        }
        else {
            float h0 = h * aspectRatio / imageAspectRatio;
            y -= (h0 - h) * f;
            h = h0;
        }

        image.draw(g, x, y, w, h);

        g.endClip();
    }
}
