package forge.deck;

import forge.game.GameType;
import forge.localinstance.properties.ForgeConstants;

import java.util.Map;

public class NetDeckCategory extends NetDeckStorageBase {
    public static final String PREFIX = "NET_DECK_";
    private static Map<String, NetDeckCategory> constructed, commander, brawl, oathbreaker, tinyleaders;

    private static Map<String, NetDeckCategory> loadCategories(String filename) {
        return loadCategories(filename, NetDeckCategory::new);
    }

    public static NetDeckCategory selectAndLoad(GameType gameType) {
        return selectAndLoad(gameType, null);
    }
    public static NetDeckCategory selectAndLoad(GameType gameType, String name) {
        return selectAndLoad(gameType, name, false);
    }

    public static Map<String, NetDeckCategory> getCategories(GameType gameType) {
        return getCategoriesForGameType(gameType);
    }

    public static NetDeckCategory selectAndLoad(GameType gameType, String name, boolean forceDownload) {
        Map<String, NetDeckCategory> categories = getCategoriesForGameType(gameType);
        return selectAndLoad(categories, name, forceDownload, "Select a Net Deck category");
    }

    private static Map<String, NetDeckCategory> getCategoriesForGameType(GameType gameType) {
        Map<String, NetDeckCategory> categories;
        switch (gameType) {
        case Constructed:
        case Gauntlet:
            if (constructed == null) {
                constructed = loadCategories(ForgeConstants.NET_DECKS_LIST_FILE);
            }
            categories = constructed;
            break;
        case Commander:
            if (commander == null) {
                commander = loadCategories(ForgeConstants.NET_DECKS_COMMANDER_LIST_FILE);
            }
            categories = commander;
            break;
            case Brawl:
                if (brawl == null) {
                    brawl = loadCategories(ForgeConstants.NET_DECKS_BRAWL_LIST_FILE);
                }
                categories = brawl;
                break;
            case Oathbreaker:
                if (oathbreaker == null) {
                    oathbreaker = loadCategories(ForgeConstants.NET_DECKS_OATHBREAKER_LIST_FILE);
                }
                categories = oathbreaker;
                break;
            case TinyLeaders:
                if (tinyleaders == null) {
                    tinyleaders = loadCategories(ForgeConstants.NET_DECKS_TINYLEADERS_LIST_FILE);
                }
                categories = tinyleaders;
                break;
        default:
            return null;
        }
        return categories;
    }

    private NetDeckCategory(String name0, String url0) {
        super(name0, ForgeConstants.DECK_NET_DIR + name0, url0);
    }

    public String getDeckType() {
        return "Net Decks - " + name;
    }

    @Override
    public String toString() {
        return name;
    }
}
