package forge.deck;

import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;

public class NetDeckArchivePioneer extends NetDeckStorageBase {
    public static final String PREFIX = "NET_ARCHIVE_PIONEER_DECK";

    public static NetDeckArchivePioneer selectAndLoad(GameType gameType) {
        return selectAndLoad(gameType, null);
    }

    public static NetDeckArchivePioneer selectAndLoad(GameType gameType, String name) {
        return selectAndLoadArchive(gameType, name,
                ForgeConstants.NET_ARCHIVE_PIONEER_DECKS_LIST_FILE, "Pioneer", NetDeckArchivePioneer::new);
    }

    public static NetDeckArchivePioneer selectAndLoad(GameType gameType, String name, boolean forceDownload) {
        return selectAndLoadArchive(gameType, name, forceDownload,
                ForgeConstants.NET_ARCHIVE_PIONEER_DECKS_LIST_FILE, "Pioneer", NetDeckArchivePioneer::new);
    }

    private NetDeckArchivePioneer(String name0, String url0) {
        super(name0, ForgeConstants.DECK_NET_ARCHIVE_DIR + name0, url0, "Net Archive Pioneer Decks");
    }
}
