package forge.deck;

import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;

public class NetDeckArchiveCommander extends NetDeckStorageBase {
    public static final String PREFIX = "NET_ARCHIVE_COMMANDER_DECK";

    public static NetDeckArchiveCommander selectAndLoad(GameType gameType) {
        return selectAndLoad(gameType, null);
    }

    public static NetDeckArchiveCommander selectAndLoad(GameType gameType, String name) {
        return selectAndLoad(gameType, name, false);
    }

    public static NetDeckArchiveCommander selectAndLoad(GameType gameType, String name, boolean forceDownload) {
        switch (gameType) {
        case Commander:
        case CommanderGauntlet:
            return loadArchive(name, forceDownload,
                    ForgeConstants.NET_ARCHIVE_COMMANDER_DECKS_LIST_FILE, "Commander", NetDeckArchiveCommander::new);
        default:
            return null;
        }
    }

    private NetDeckArchiveCommander(String name0, String url0) {
        super(name0, ForgeConstants.DECK_NET_ARCHIVE_DIR + name0, url0, "Net Archive Commander Decks");
    }
}
