package forge;

import forge.item.InventoryItem;

public class ImageCacheProvider implements ImageCacheBridge.Methods {

    @Override
    public String getImageKey(InventoryItem cp, boolean altState) {
        return ImageCache.getImageKey(cp, altState);
    }

    @Override
    public String getTokenKey(String imageName) {
        return ImageCache.getTokenImageKey(imageName);
    }
}