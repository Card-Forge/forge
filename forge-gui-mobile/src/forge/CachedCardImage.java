package forge;

import com.badlogic.gdx.graphics.Texture;
import forge.assets.ImageCache;
import forge.game.card.CardView;
import forge.gui.GuiBase;
import forge.item.InventoryItem;
import forge.screens.match.MatchController;
import forge.util.ImageFetcher;

public abstract class CachedCardImage implements ImageFetcher.Callback {
    protected final String key;
    static final ImageFetcher fetcher = GuiBase.getInterface().getImageFetcher();

    public CachedCardImage(final CardView card) {
        key = card.getCurrentState().getImageKey(MatchController.instance.getLocalPlayers());
        fetch();
    }

    public CachedCardImage(final InventoryItem ii) {
        key = ii.getImageKey(false);
        fetch();
    }

    public CachedCardImage(String key) {
        this.key = key;
        fetch();
    }

    public void fetch() {
        if (!ImageCache.imageKeyFileExists(key)) {
            fetcher.fetchImage(key, this);
        }
    }

    public Texture getImage() {
        return ImageCache.getImage(key, true);
    }

    public Texture getImage(String mykey) {
        return ImageCache.getImage(mykey, true);
    }

    public abstract void onImageFetched();
}
