package forge.deck;

import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;

public class NetDeckArchivePauper extends NetDeckStorageBase {
    public static final String PREFIX = "NET_ARCHIVE_PAUPER_DECK";

    public static NetDeckArchivePauper selectAndLoad(GameType gameType) {
        return selectAndLoad(gameType, null);
    }

    public static NetDeckArchivePauper selectAndLoad(GameType gameType, String name) {
        return selectAndLoadArchive(gameType, name,
                ForgeConstants.NET_ARCHIVE_PAUPER_DECKS_LIST_FILE, "Pauper", NetDeckArchivePauper::new);
    }

    public static NetDeckArchivePauper selectAndLoad(GameType gameType, String name, boolean forceDownload) {
        return selectAndLoadArchive(gameType, name, forceDownload,
                ForgeConstants.NET_ARCHIVE_PAUPER_DECKS_LIST_FILE, "Pauper", NetDeckArchivePauper::new);
    }

    private NetDeckArchivePauper(String name0, String url0) {
        super(name0, ForgeConstants.DECK_NET_ARCHIVE_DIR + name0, url0, "Net Archive Pauper Decks");
    }
}
