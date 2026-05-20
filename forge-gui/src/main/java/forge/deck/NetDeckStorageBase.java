package forge.deck;

import forge.deck.io.DeckSerializer;
import forge.deck.io.DeckStorage;
import forge.game.GameType;
import forge.gui.GuiBase;
import forge.gui.download.GuiDownloadZipService;
import forge.gui.util.SGuiChoose;
import forge.util.FileUtil;
import forge.util.WaitCallback;
import forge.util.storage.StorageBase;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class NetDeckStorageBase extends StorageBase<Deck> {
    private static final Map<String, Map<String, ? extends NetDeckStorageBase>> categoryCache = new HashMap<>();

    private final String url;
    private final String deckTypePrefix;

    protected interface Factory<T extends NetDeckStorageBase> {
        T create(String name, String url);
    }

    protected NetDeckStorageBase(final String name0, final String fullPath0, final String url0) {
        this(name0, fullPath0, url0, null);
    }

    protected NetDeckStorageBase(final String name0, final String fullPath0, final String url0, final String deckTypePrefix0) {
        super(name0, fullPath0, new HashMap<>());
        url = url0;
        deckTypePrefix = deckTypePrefix0;
    }

    protected static <T extends NetDeckStorageBase> Map<String, T> loadCategories(final String filename, final Factory<T> factory) {
        Map<String, T> categories = new TreeMap<>();
        if (FileUtil.doesFileExist(filename)) {
            List<String> lines = FileUtil.readFile(filename);
            for (String line : lines) {
                int idx = line.indexOf('|');
                if (idx != -1) {
                    String name = line.substring(0, idx).trim();
                    String url = line.substring(idx + 1).trim();
                    categories.put(name, factory.create(name, url));
                }
            }
        }
        return categories;
    }

    protected static <T extends NetDeckStorageBase> T selectAndLoad(final Map<String, T> categories,
            final String name, final boolean forceDownload, final String chooserTitle) {
        if (categories == null) {
            return null;
        }

        if (name != null) {
            T category = categories.get(name);
            if (category != null && (forceDownload || category.map.isEmpty())) {
                category.map.clear();
                if (forceDownload) {
                    return download(category) ? category : null;
                }
                category.loadCachedDecks();
            }
            return category;
        }

        List<T> category = new ArrayList<>(categories.values());
        Collections.reverse(category);

        final T c = SGuiChoose.oneOrNone(chooserTitle, category);
        if (c == null) { return null; }

        if (c.map.isEmpty()) { //only load/download decks once per session
            c.loadCachedDecks();
            if (c.map.isEmpty() && !download(c)) {
                return null;
            }
        }
        return c;
    }

    protected static <T extends NetDeckStorageBase> T selectAndLoadArchive(final GameType gameType,
            final String name, final String listFile, final String formatName, final Factory<T> factory) {
        return selectAndLoadArchive(gameType, name, false, listFile, formatName, factory);
    }

    @SuppressWarnings("unchecked")
    protected static <T extends NetDeckStorageBase> T selectAndLoadArchive(final GameType gameType,
            final String name, final boolean forceDownload, final String listFile, final String formatName,
            final Factory<T> factory) {
        switch (gameType) {
        case Constructed:
        case Gauntlet:
            Map<String, T> categories = (Map<String, T>) categoryCache.computeIfAbsent(listFile,
                    file -> loadCategories(file, factory));
            return selectAndLoad(categories, name, forceDownload,
                    "Select a Net Deck Archive " + formatName + " category");
        default:
            return null;
        }
    }

    protected final void loadCachedDecks() {
        File downloadDir = new File(getFullPath());
        if (downloadDir.exists()) {
            for (File file : getAllFilesList(downloadDir, DeckStorage.DCK_FILE_FILTER)) {
                Deck deck = DeckSerializer.fromFile(file);
                if (deck != null) {
                    map.put(deck.getName(), deck);
                }
            }
        }
    }

    private static boolean download(final NetDeckStorageBase c) {
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
        final Boolean downloaded = callback.invokeAndWait(); //wait for download to finish
        return Boolean.TRUE.equals(downloaded) && !c.map.isEmpty();
    }

    public String getUrl() {
        return url;
    }

    public String getDeckType() {
        return deckTypePrefix == null ? name : deckTypePrefix + " - " + name;
    }

    @Override
    public String toString() {
        return name;
    }
}
