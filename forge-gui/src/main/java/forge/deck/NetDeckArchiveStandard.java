package forge.deck;

import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;

public class NetDeckArchiveStandard extends NetDeckStorageBase {
    public static final String PREFIX = "NET_ARCHIVE_STANDARD_DECK";

    public static NetDeckArchiveStandard selectAndLoad(GameType gameType) {
        return selectAndLoad(gameType, null);
    }

    public static NetDeckArchiveStandard selectAndLoad(GameType gameType, String name) {
        return selectAndLoadArchive(gameType, name,
                ForgeConstants.NET_ARCHIVE_STANDARD_DECKS_LIST_FILE, "Standard", NetDeckArchiveStandard::new);
    }

    public static NetDeckArchiveStandard selectAndLoad(GameType gameType, String name, boolean forceDownload) {
        return selectAndLoadArchive(gameType, name, forceDownload,
                ForgeConstants.NET_ARCHIVE_STANDARD_DECKS_LIST_FILE, "Standard", NetDeckArchiveStandard::new);
    }

    private NetDeckArchiveStandard(String name0, String url0) {
        super(name0, ForgeConstants.DECK_NET_ARCHIVE_DIR + name0, url0, "Net Archive Standard Decks");
    }
}
