package forge.deck;

import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.game.GameType;
import forge.item.InventoryItem;
import forge.util.IHasName;
import forge.util.storage.IStorage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A deck-browser row. Some rows are real decks, while others are navigation or
 * virtual choices that intentionally leave deck-specific columns blank.
 */
public class DeckBrowserEntry extends DeckProxy {
    public enum Kind {
        PARENT_FOLDER(false, 0),
        NET_FOLDER(false, 1),
        GENERATED_GROUP(false, 1),
        GENERATED_FOLDER(false, 1),
        FOLDER(false, 2),
        GENERATED_OPTION(true, 3),
        DECK(true, 4);

        private final boolean deck;
        private final int sortGroup;

        Kind(final boolean deck0, final int sortGroup0) {
            deck = deck0;
            sortGroup = sortGroup0;
        }

        public boolean isDeck() {
            return deck;
        }

        public int getSortGroup() {
            return sortGroup;
        }
    }

    private final Kind kind;
    private final String name;
    private final String path;
    private final IStorage<Deck> folder;
    private final DeckProxy deckProxy;
    private final DeckType deckType;

    private static final Deck EMPTY_DECK = new Deck();

    private DeckBrowserEntry(final Kind kind0, final String name0, final String path0,
            final IStorage<Deck> folder0, final DeckProxy deckProxy0, final DeckType deckType0) {
        super();
        kind = kind0;
        name = name0;
        path = path0 == null ? "" : path0;
        folder = folder0;
        deckProxy = deckProxy0;
        deckType = deckType0;
    }

    public static DeckBrowserEntry deck(final DeckProxy deck) {
        return new DeckBrowserEntry(Kind.DECK, deck.getName(), deck.getPath(), null, deck, null);
    }

    public static DeckBrowserEntry deck(final Deck deck, final GameType gameType, final String path,
            final IStorage<Deck> storage) {
        return deck(new DeckProxy(deck, gameType.toString(), gameType, path, storage, null));
    }

    public static DeckBrowserEntry folder(final String name, final String path, final IStorage<Deck> folder, final DeckType deckType) {
        return new DeckBrowserEntry(Kind.FOLDER, name, path, folder, null, deckType);
    }

    public static DeckBrowserEntry parentFolder(final String path, final IStorage<Deck> folder) {
        return new DeckBrowserEntry(Kind.PARENT_FOLDER, "..", path, folder, null, null);
    }

    public static DeckBrowserEntry netFolder(final String name, final String path, final IStorage<Deck> folder, final DeckType deckType) {
        return new DeckBrowserEntry(Kind.NET_FOLDER, name, path, folder, null, deckType);
    }

    public static DeckBrowserEntry generatedFolder(final String name, final String path, final DeckType deckType) {
        return new DeckBrowserEntry(Kind.GENERATED_FOLDER, name, path, null, null, deckType);
    }

    public static DeckBrowserEntry generatedGroup(final String name, final String path) {
        return new DeckBrowserEntry(Kind.GENERATED_GROUP, name, path, null, null, null);
    }

    public static DeckBrowserEntry generatedOption(final DeckProxy deck) {
        return new DeckBrowserEntry(Kind.GENERATED_OPTION, deck.getName(), deck.getPath(), null, deck, null);
    }

    public static DeckProxy fromDeckProxy(final DeckProxy deck) {
        return deck instanceof DeckBrowserEntry ? deck
                : deck.isGeneratedDeck() ? generatedOption(deck) : deck(deck);
    }

    public static List<DeckProxy> fromDeckProxies(final Iterable<DeckProxy> decks) {
        final List<DeckProxy> rows = new ArrayList<>();
        for (final DeckProxy deck : decks) {
            rows.add(fromDeckProxy(deck));
        }
        return rows;
    }

    public static DeckProxy unwrap(final DeckProxy row) {
        if (!(row instanceof DeckBrowserEntry entry)) {
            return row;
        }
        return entry.isDeck() ? entry.deckProxy : null;
    }

    public static DeckProxy forMetadata(final DeckProxy row) {
        if (!(row instanceof DeckBrowserEntry entry)) {
            return row;
        }
        return !entry.isDeck() ? null : entry.isGeneratedDeck() ? entry : entry.deckProxy;
    }

    public static int getSortGroup(final InventoryItem item) {
        return item instanceof DeckBrowserEntry ? ((DeckBrowserEntry) item).getSortGroup()
                : Kind.GENERATED_OPTION.getSortGroup();
    }

    public static void sort(final List<DeckProxy> rows) {
        rows.sort(Comparator.comparingInt((DeckProxy row) -> getSortGroup(row))
                .thenComparing(DeckProxy::getName, String.CASE_INSENSITIVE_ORDER));
    }

    public static boolean containsDeckRows(final Iterable<DeckProxy> rows) {
        for (final DeckProxy row : rows) {
            if (!(row instanceof DeckBrowserEntry entry) || entry.isDeck()) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsCommanderDeckRows(final Iterable<DeckProxy> rows) {
        for (final DeckProxy row : rows) {
            final DeckProxy deck = forMetadata(row);
            if (deck != null && deck.hasCommanderSection()) {
                return true;
            }
        }
        return false;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isDeck() {
        return kind.isDeck();
    }

    public int getSortGroup() {
        return kind.getSortGroup();
    }

    public IStorage<Deck> getFolder() {
        return folder;
    }

    public DeckType getDeckType() {
        return deckType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    @Override
    public String getItemType() {
        return kind == Kind.DECK ? deckProxy.getItemType() : kind.name();
    }

    @Override
    public Deck getDeck() {
        return deckProxy == null ? null : deckProxy.getDeck();
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public IStorage<? extends IHasName> getStorage() {
        return deckProxy == null ? null : deckProxy.getStorage();
    }

    @Override
    public String getSourceFileName() {
        return deckProxy == null ? null : deckProxy.getSourceFileName();
    }

    @Override
    public String getSourceUrl() {
        return deckProxy == null ? null : deckProxy.getSourceUrl();
    }

    @Override
    public ColorSet getColor() {
        return deckProxy == null ? null : deckProxy.getColor();
    }

    @Override
    public CardEdition getEdition() {
        return deckProxy == null ? null : deckProxy.getEdition();
    }

    @Override
    public String getFormatsString() {
        return deckProxy == null || kind == Kind.GENERATED_OPTION ? null : deckProxy.getFormatsString();
    }

    @Override
    public int getMainSize() {
        return deckProxy == null ? -1 : deckProxy.getMainSize();
    }

    @Override
    public int getSideSize() {
        return deckProxy == null || kind == Kind.GENERATED_OPTION ? -1 : deckProxy.getSideSize();
    }

    @Override
    public Deck.UnplayableAICards getAI() {
        return deckProxy == null || kind == Kind.GENERATED_OPTION ? EMPTY_DECK.getUnplayableAICards() : deckProxy.getAI();
    }

    @Override
    public boolean isGeneratedDeck() {
        return kind == Kind.GENERATED_OPTION || (deckProxy != null && deckProxy.isGeneratedDeck());
    }

    @Override
    public boolean saveDeck() {
        return deckProxy != null && deckProxy.saveDeck();
    }

    @Override
    public void reloadFromStorage() {
        if (deckProxy != null) {
            deckProxy.reloadFromStorage();
        }
    }

    @Override
    public String getUniqueKey() {
        return deckProxy == null ? "DeckBrowser|" + kind + "|" + path + "|" + name : deckProxy.getUniqueKey();
    }

    @Override
    public void deleteFromStorage() {
        if (deckProxy != null) {
            deckProxy.deleteFromStorage();
        }
    }

    @Override
    public String toString() {
        if (deckProxy != null) {
            return deckProxy.toString();
        }
        return kind + "|" + path + "|" + name;
    }
}
