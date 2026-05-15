package forge.deck;

import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;

public class NetDeckArchiveModern extends NetDeckStorageBase {
    public static final String PREFIX = "NET_ARCHIVE_MODERN_DECK";

    public static NetDeckArchiveModern selectAndLoad(GameType gameType) {
        return selectAndLoad(gameType, null);
    }

    public static NetDeckArchiveModern selectAndLoad(GameType gameType, String name) {
        return selectAndLoadArchive(gameType, name,
                ForgeConstants.NET_ARCHIVE_MODERN_DECKS_LIST_FILE, "Modern", NetDeckArchiveModern::new);
    }

    public static NetDeckArchiveModern selectAndLoad(GameType gameType, String name, boolean forceDownload) {
        return selectAndLoadArchive(gameType, name, forceDownload,
                ForgeConstants.NET_ARCHIVE_MODERN_DECKS_LIST_FILE, "Modern", NetDeckArchiveModern::new);
    }

    private NetDeckArchiveModern(String name0, String url0) {
        super(name0, ForgeConstants.DECK_NET_ARCHIVE_DIR + name0, url0, "Net Archive Modern Decks");
    }
}
