package forge;

import java.awt.image.BufferedImage;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.screens.match.CMatchUI;
import forge.util.ImageFetcher;
import forge.util.SwingImageFetcher;
import org.tinylog.Logger;

public abstract class CachedCardImage implements ImageFetcher.Callback {
    final CardView card;
    final Iterable<PlayerView> viewers;
    final int width;
    final int height;
    /** When non-null, hidden-zone cards the match allows viewing use real card art. */
    final CMatchUI matchUI;

    static final SwingImageFetcher fetcher = new SwingImageFetcher();

    public CachedCardImage(final CardView card, final Iterable<PlayerView> viewers, final int width, final int height) {
        this(card, viewers, width, height, null);
    }

    public CachedCardImage(final CardView card, final Iterable<PlayerView> viewers, final int width, final int height,
            final CMatchUI matchUI) {
        this.card = card;
        this.viewers = viewers;
        this.width = width;
        this.height = height;
        this.matchUI = matchUI;
        if (ImageCache.isSupportedImageSize(width, height)) {
            BufferedImage image = ImageCache.getImageNoDefault(card, viewers, width, height, matchUI);
            if (image == null) {
                String key = ImageCache.imageKeyForCardDisplay(card, viewers, matchUI);
                Logger.debug("Fetch due to missing key: " + key + " for " + card);
                fetcher.fetchImage(key, this);
            }
        }
    }

    public BufferedImage getImage() {
        return ImageCache.getImage(card, viewers, width, height, matchUI);
    }

    public abstract void onImageFetched();
}
