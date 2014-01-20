package forge;

import forge.item.InventoryItem;

public class ImageCacheBridge {
    
    public static Methods instance;
    
    public interface Methods {
        String getImageKey(InventoryItem cp, boolean altState);
        String getTokenKey(String imageName);
    }
}