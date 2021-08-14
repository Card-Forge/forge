package forge.deck;

import forge.deck.io.DeckSerializer;
import forge.deck.io.DeckStorage;
import forge.game.GameType;
import forge.gui.GuiBase;
import forge.gui.download.GuiDownloadZipService;
import forge.gui.util.SGuiChoose;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;
import forge.util.WaitCallback;
import forge.util.storage.StorageBase;

import java.io.*;
import java.util.*;

public class NetDeckCategory extends StorageBase<Deck> {
    public static final String PREFIX = "NET_DECK_";
    private static Map<String, NetDeckCategory> constructed, commander, brawl, oathbreaker, tinyleaders;

    private static Map<String, NetDeckCategory> loadCategories(String filename) {
        Map<String, NetDeckCategory> categories = new TreeMap<>();
        if (FileUtil.doesFileExist(filename)) {
            List<String> lines = FileUtil.readFile(filename);
            for (String line : lines) {
                int idx = line.indexOf('|');
                if (idx != -1) {
                    String name = line.substring(0, idx).trim();
                    String url = line.substring(idx + 1).trim();
                    categories.put(name, new NetDeckCategory(name, url));
                }
            }
        }
        return categories;
    }

    public static NetDeckCategory selectAndLoad(GameType gameType) {
        return selectAndLoad(gameType, null);
    }
    public static NetDeckCategory selectAndLoad(GameType gameType, String name) {
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

        if (name != null) {
            NetDeckCategory category = categories.get(name);
            if (category != null && category.map.isEmpty()) {
                //if name passed in, try to load decks from current cached files
                File downloadDir = new File(category.getFullPath());
                if (downloadDir.exists()) {
                    for (File file : getAllFilesList(downloadDir, DeckStorage.DCK_FILE_FILTER)) {
                        Deck deck = DeckSerializer.fromFile(file);
                        if (deck != null) {
                            category.map.put(deck.getName(), deck);
                        }
                    }
                }
            }
            return category;
        }

        final NetDeckCategory c = SGuiChoose.oneOrNone("Select a Net Deck category", categories.values());
        if (c == null) { return null; }

        if (c.map.isEmpty()) { //only download decks once per session
            WaitCallback<Boolean> callback = new WaitCallback<Boolean>() {
                @Override
                public void run() {
                    String downloadLoc = c.getFullPath();
                    GuiBase.getInterface().download(new GuiDownloadZipService(c.getName(), "decks", c.getUrl(), downloadLoc, downloadLoc, null) {
                        @Override
                        protected void copyInputStream(InputStream in, String outPath) throws IOException {
                            super.copyInputStream(in, outPath);
    
                            Deck deck = DeckSerializer.fromFile(new File(outPath));
                            if (deck != null) {
                                c.map.put(deck.getName(), deck);
                            }
                        }
                    }, this);
                }
            };
            if (!callback.invokeAndWait()) { return null; } //wait for download to finish
        }
        return c;
    }

    private final String url;

    private NetDeckCategory(String name0, String url0) {
        super(name0, ForgeConstants.DECK_NET_DIR + name0, new HashMap<>());
        url = url0;
    }

    public String getUrl() {
        return url;
    }

    public String getDeckType() {
        return "Net Decks - " + name;
    }

    @Override
    public String toString() {
        return name;
    }
}
