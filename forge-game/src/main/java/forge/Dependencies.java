package forge;

import forge.item.InventoryItem;

public class Dependencies {

    public static PreferencesMethods preferences;
    public interface PreferencesMethods {
        public abstract boolean getEnableAiCheats();
        public abstract boolean getCloneModeSource();
        public abstract String getLogEntryType();
        public abstract String getCurrentAiProfile();
        public abstract boolean canRandomFoil();
        public abstract boolean isManaBurnEnabled();
        public abstract boolean areBlocksFree();
    }

    public static ImageCacheMethods imagecache;
    public interface ImageCacheMethods {
        String getImageKey(InventoryItem cp, boolean altState);
        String getTokenKey(String imageName);
        String getMorphImage();
    }
}