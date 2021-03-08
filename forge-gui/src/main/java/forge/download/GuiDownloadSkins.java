package forge.download;

import java.util.Map;
import java.util.TreeMap;

import forge.localinstance.properties.ForgeConstants;

public class GuiDownloadSkins extends GuiDownloadService {
    @Override
    public String getTitle() {
        return "Download Skins";
    }

    @Override
    protected final Map<String, String> getNeededFiles() {
        // read all card names and urls
        final Map<String, String> urls = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        addMissingItems(urls, ForgeConstants.SKINS_LIST_FILE,  ForgeConstants.CACHE_SKINS_DIR, true);

        return urls;
    }
}
