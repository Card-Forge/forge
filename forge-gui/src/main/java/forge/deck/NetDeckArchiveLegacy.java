package forge.deck;

import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;

public class NetDeckArchiveLegacy extends NetDeckStorageBase {
    public static final String PREFIX = "NET_ARCHIVE_LEGACY_DECK";

    public static NetDeckArchiveLegacy selectAndLoad(GameType gameType) {
        return selectAndLoad(gameType, null);
    }

    public static NetDeckArchiveLegacy selectAndLoad(GameType gameType, String name) {
        return selectAndLoadArchive(gameType, name,
                ForgeConstants.NET_ARCHIVE_LEGACY_DECKS_LIST_FILE, "Legacy", NetDeckArchiveLegacy::new);
    }

    public static NetDeckArchiveLegacy selectAndLoad(GameType gameType, String name, boolean forceDownload) {
        return selectAndLoadArchive(gameType, name, forceDownload,
                ForgeConstants.NET_ARCHIVE_LEGACY_DECKS_LIST_FILE, "Legacy", NetDeckArchiveLegacy::new);
    }

    private NetDeckArchiveLegacy(String name0, String url0) {
        super(name0, ForgeConstants.DECK_NET_ARCHIVE_DIR + name0, url0, "Net Archive Legacy Decks");
    }
}
