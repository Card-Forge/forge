package forge.deck;

import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;

public class NetDeckArchiveBlock extends NetDeckStorageBase {
    public static final String PREFIX = "NET_ARCHIVE_BLOCK_DECK";

    public static NetDeckArchiveBlock selectAndLoad(GameType gameType) {
        return selectAndLoad(gameType, null);
    }

    public static NetDeckArchiveBlock selectAndLoad(GameType gameType, String name) {
        return selectAndLoadArchive(gameType, name,
                ForgeConstants.NET_ARCHIVE_BLOCK_DECKS_LIST_FILE, "Block", NetDeckArchiveBlock::new);
    }

    public static NetDeckArchiveBlock selectAndLoad(GameType gameType, String name, boolean forceDownload) {
        return selectAndLoadArchive(gameType, name, forceDownload,
                ForgeConstants.NET_ARCHIVE_BLOCK_DECKS_LIST_FILE, "Block", NetDeckArchiveBlock::new);
    }

    private NetDeckArchiveBlock(String name0, String url0) {
        super(name0, ForgeConstants.DECK_NET_ARCHIVE_DIR + name0, url0, "Net Archive Block Decks");
    }
}
