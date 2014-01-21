package forge;

import forge.Dependencies.ImageCacheMethods;
import forge.item.InventoryItem;
import forge.properties.NewConstants;

public class ImageCacheProvider implements ImageCacheMethods {

    @Override
    public String getImageKey(InventoryItem cp, boolean altState) {
        return ImageCache.getImageKey(cp, altState);
    }

    @Override
    public String getTokenKey(String imageName) {
        return ImageCache.getTokenImageKey(imageName);
    }

    @Override
    public String getMorphImage() {
        return  NewConstants.CACHE_MORPH_IMAGE_FILE;
    }
}