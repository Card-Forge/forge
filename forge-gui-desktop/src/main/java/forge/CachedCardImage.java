package forge;

import java.awt.image.BufferedImage;

import forge.game.card.CardView;
import forge.game.player.PlayerView;

public abstract class CachedCardImage {
    final CardView card;
    final Iterable<PlayerView> viewers;
    final int width;
    final int height;

    public CachedCardImage(final CardView card, final Iterable<PlayerView> viewers, final int width, final int height) {
        this.card = card;
        this.viewers = viewers;
        this.width = width;
        this.height = height;
        BufferedImage image = ImageCache.getImageNoDefault(card, viewers, width, height);
        if (image == null) {
            String key = card.getCurrentState().getImageKey(viewers);
            ImageFetcher.fetchImage(card, key, this);
        }
    }

    public BufferedImage getImage() {
        return ImageCache.getImage(card, viewers, width, height);
    }

    public abstract void onImageFetched();
}
