package forge.gui.download;

import java.util.Map;
import java.util.TreeMap;

import forge.localinstance.properties.ForgeConstants;

public class GuiDownloadMusic extends GuiDownloadService {
    @Override
    public String getTitle() {
        return "Download optional Music for Adventure Mode 200 MB ";
    }

    @Override
    protected final Map<String, String> getNeededFiles() {
        final Map<String, String> urls = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        addMissingItems(urls, ForgeConstants.MUSIC_LIST_ADVENTURE_FILE,  ForgeConstants.ADVENTURE_MUSIC_DIR );
        return urls;
    }
}
