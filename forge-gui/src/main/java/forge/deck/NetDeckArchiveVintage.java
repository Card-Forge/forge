package forge.deck;

import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;

public class NetDeckArchiveVintage extends NetDeckStorageBase {
    public static final String PREFIX = "NET_ARCHIVE_VINTAGE_DECK";

    public static NetDeckArchiveVintage selectAndLoad(GameType gameType) {
        return selectAndLoad(gameType, null);
    }

    public static NetDeckArchiveVintage selectAndLoad(GameType gameType, String name) {
        return selectAndLoadArchive(gameType, name,
                ForgeConstants.NET_ARCHIVE_VINTAGE_DECKS_LIST_FILE, "Vintage", NetDeckArchiveVintage::new);
    }

    public static NetDeckArchiveVintage selectAndLoad(GameType gameType, String name, boolean forceDownload) {
        return selectAndLoadArchive(gameType, name, forceDownload,
                ForgeConstants.NET_ARCHIVE_VINTAGE_DECKS_LIST_FILE, "Vintage", NetDeckArchiveVintage::new);
    }

    private NetDeckArchiveVintage(String name0, String url0) {
        super(name0, ForgeConstants.DECK_NET_ARCHIVE_DIR + name0, url0, "Net Archive Vintage Decks");
    }
}
