package forge.deck;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.DeckSection;
import forge.error.BugReporter;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.item.PreconDeck;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.util.BinaryUtil;
import forge.util.IHasName;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

// Adding a generic to this class creates compile problems in ItemManager (that I can not fix)
public class DeckProxy implements InventoryItem {
    protected IHasName deck;
    protected final String deckType;
    protected final IStorage<? extends IHasName> storage;

    public static final Function<DeckProxy, String> FN_GET_NAME = new Function<DeckProxy, String>() {
        @Override
        public String apply(DeckProxy arg0) {
            return arg0.getName();
        }
    };

    // cached values
    protected ColorSet color;
    protected ColorSet colorIdentity;
    protected Iterable<GameFormat> formats;
    private int mainSize = Integer.MIN_VALUE;
    private int sbSize = Integer.MIN_VALUE;
    private final String path;
    private final Function<IHasName, Deck> fnGetDeck;
    private CardEdition edition;
    private CardRarity highestRarity;

    protected DeckProxy() {
        this(null, "", null, "", null, null);
    }

    public DeckProxy(Deck deck, String deckType, GameType type, IStorage<? extends IHasName> storage) {
        this(deck, deckType, type, "", storage, null);
    }

    public DeckProxy(IHasName deck, String deckType, Function<IHasName, Deck> fnGetDeck, GameType type, IStorage<? extends IHasName> storage) {
        this(deck, deckType, type, "", storage, fnGetDeck);
    }

    private DeckProxy(IHasName deck, String deckType, GameType type, String path, IStorage<? extends IHasName> storage, Function<IHasName, Deck> fnGetDeck) {
        this.deck = deck;
        this.deckType = deckType;
        this.storage = storage;
        this.path = path;
        this.fnGetDeck = fnGetDeck;
        // gametype could give us a hint whether the storage is updateable and enable choice of right editor for this deck
    }

    @Override
    public String getName() {
        return deck.getName();
    }

    @Override
    public String getItemType() {
        // Could distinguish decks depending on gametype
        return "Deck";
    }

    public Deck getDeck() {
        return deck instanceof Deck && fnGetDeck == null ? (Deck) deck : fnGetDeck.apply(deck);
    }

    public String getPath() {
        return path;
    }

    public CardEdition getEdition() {
        if (edition == null) {
            if (deck instanceof PreconDeck) {
                edition = StaticData.instance().getEditions().get(((PreconDeck) deck).getEdition());
            }
            else if (!isGeneratedDeck()) {
                edition = StaticData.instance().getEditions().getEarliestEditionWithAllCards(getDeck().getAllCardsInASinglePool());
            }
        }
        return edition;
    }

    public String getUniqueKey() {
        if (deck == null) { return ""; }
        return deckType + "|" + path + "|" + deck.getName();
    }

    @Override
    public String toString() {
        if (deck == null) { return ""; }
        return getDeckString(path, deck.getName());
    }

    public static String getDeckString(String path, String name) {
        if (StringUtils.isEmpty(path)) {
            return name;
        }
        return path + "/" + name;
    }

    public void invalidateCache() {
        color = null;
        colorIdentity = null;
        highestRarity = null;
        formats = null;
        edition = null;
        mainSize = Integer.MIN_VALUE;
        sbSize = Integer.MIN_VALUE;
    }

    public ColorSet getColor() {
        if (color == null && !isGeneratedDeck()) {
            byte colorProfile = MagicColor.COLORLESS;
            for (Entry<DeckSection, CardPool> deckEntry : getDeck()) {
                switch (deckEntry.getKey()) {
                case Main:
                case Commander:
                    for (Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
                        colorProfile |= poolEntry.getKey().getRules().getColor().getColor();
                    }
                    break;
                default:
                    break; //ignore other sections
                }
            }
            color = ColorSet.fromMask(colorProfile);
        }
        return color;
    }

    public ColorSet getColorIdentity() {
        if (colorIdentity == null) {
            byte colorProfile = MagicColor.COLORLESS;

            for (Entry<DeckSection, CardPool> deckEntry : getDeck()) {
                switch (deckEntry.getKey()) {
                case Main:
                case Sideboard:
                case Commander:
                    for (Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
                        colorProfile |= poolEntry.getKey().getRules().getColorIdentity().getColor();
                    }
                    break;
                default:
                    break; //ignore other sections
                }
            }
            colorIdentity = ColorSet.fromMask(colorProfile);
        }
        return colorIdentity;
    }

    public CardRarity getHighestRarity() {
        if (highestRarity == null) {
            highestRarity = CardRarity.Common;
            for (Entry<DeckSection, CardPool> deckEntry : getDeck()) {
                switch (deckEntry.getKey()) {
                case Main:
                case Sideboard:
                case Commander:
                    for (Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
                        switch (poolEntry.getKey().getRarity()) {
                        case MythicRare:
                            highestRarity = CardRarity.MythicRare;
                            return highestRarity; //can return right away since nothing is higher
                        case Special:
                            highestRarity = CardRarity.Special; //can always set this since only mythic should be treated higher
                            break;
                        case Rare:
                            if (highestRarity != CardRarity.Special) {
                                highestRarity = CardRarity.Rare; //can set to rare unless deck contains Special rarity
                            }
                            break;
                        case Uncommon:
                            if (highestRarity != CardRarity.Rare && highestRarity != CardRarity.Special) {
                                highestRarity = CardRarity.Uncommon; //can set to uncommon unless deck contains rare or uncommon
                            }
                            break;
                        default:
                            break; //treat other rarities as equivalent to common
                        }
                    }
                    break;
                default:
                    break; //ignore other sections
                }
            }
        }
        return highestRarity;
    }

    public Iterable<GameFormat> getFormats() {
        if (formats == null) {
            formats = FModel.getFormats().getAllFormatsOfDeck(getDeck());
        }
        return formats;
    }

    public String getFormatsString() {
        return StringUtils.join(Iterables.transform(getFormats(), GameFormat.FN_GET_NAME), ", ");
    }

    public int getMainSize() {
        if (mainSize == Integer.MIN_VALUE) {
            if (deck == null) {
                mainSize = -1;
            }
            else {
                mainSize = getDeck().getMain().countAll();
            }
        }
        return mainSize;
    }

    public int getSideSize() {
        if (sbSize == Integer.MIN_VALUE) {
            CardPool sb = getDeck().get(DeckSection.Sideboard);
            sbSize = sb == null ? -1 : sb.countAll();
            if (sbSize == 0) {
                sbSize = -1;
            }
        }
        return sbSize;
    }

    public boolean isGeneratedDeck() {
        return false;
    }

    // TODO: The methods below should not take the decks collections from singletons, instead they are supposed to use data passed in parameters
    public static Iterable<DeckProxy> getAllConstructedDecks(IStorage<Deck> storageRoot) {
        List<DeckProxy> result = new ArrayList<DeckProxy>();
        addDecksRecursivelly("Constructed", GameType.Constructed, result, "", storageRoot);
        return result;
    }

    private static void addDecksRecursivelly(String deckType, GameType gameType, List<DeckProxy> list, String path, IStorage<Deck> folder) {
        for (IStorage<Deck> f : folder.getFolders()) {
            String subPath = (StringUtils.isBlank(path) ? "" : path) + "/" + f.getName();
            addDecksRecursivelly(deckType, gameType, list, subPath, f);
        }

        for (Deck d : folder) {
            list.add(new DeckProxy(d, deckType, gameType, path, folder, null));
        }
    }

    // Consider using a direct predicate to manage DeckProxies (not this tunnel to collection of paper cards)
    public static final Predicate<DeckProxy> createPredicate(final Predicate<PaperCard> cardPredicate) {
        return new Predicate<DeckProxy>() {
            @Override
            public boolean apply(DeckProxy input) {
                for (Entry<DeckSection, CardPool> deckEntry : input.getDeck()) {
                    switch (deckEntry.getKey()) {
                    case Main:
                    case Sideboard:
                    case Commander:
                        for (Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
                            if (!cardPredicate.apply(poolEntry.getKey())) {
                                return false; //all cards in deck must pass card predicate to pass deck predicate
                            }
                        }
                        break;
                    default:
                        break; //ignore other sections
                    }
                }
                return true;
            }
        };
    }

    public void reloadFromStorage() {
        if (storage != null) {
            deck = storage.get(getName());
        }
        invalidateCache();
    }

    @SuppressWarnings("unchecked")
    public void updateInStorage() {
        if (storage instanceof StorageImmediatelySerialized<?>) {
            ((StorageImmediatelySerialized<IHasName>)storage).add(deck);
        }
    }

    public void deleteFromStorage() {
        if (storage != null) {
            storage.delete(getName());
        }
    }

    private static class ThemeDeckGenerator extends DeckProxy {
        private final String name;
        public ThemeDeckGenerator(String name0) {
            super();
            name = name0;
        }

        @Override
        public Deck getDeck() {
            final DeckGeneratorTheme gen = new DeckGeneratorTheme(FModel.getMagicDb().getCommonCards());
            final Deck deck = new Deck();
            gen.setSingleton(FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_SINGLETONS));
            gen.setUseArtifacts(!FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
            StringBuilder errorBuilder = new StringBuilder();
            deck.getMain().addAll(gen.getThemeDeck(this.getName(), 60, errorBuilder));
            if (errorBuilder.length() > 0) {
                BugReporter.reportBug(errorBuilder.toString());
            }
            return deck;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }

        public boolean isGeneratedDeck() {
            return true;
        }
    }

    public static Iterable<DeckProxy> getAllThemeDecks() {
        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (final String s : DeckGeneratorTheme.getThemeNames()) {
            decks.add(new ThemeDeckGenerator(s));
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static Iterable<DeckProxy> getAllPreconstructedDecks(IStorage<PreconDeck> iStorage) {
        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (final PreconDeck preconDeck : iStorage) {
            decks.add(new DeckProxy(preconDeck, "Precon", (Function<IHasName, Deck>)(Object)PreconDeck.FN_GET_DECK, null, iStorage));
        }
        return decks;
    }

    public static Iterable<DeckProxy> getAllQuestEventAndChallenges() {
        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        QuestController quest = FModel.getQuest();
        for (QuestEvent e : quest.getDuelsManager().getAllDuels()) {
            decks.add(new DeckProxy(e.getEventDeck(), "Quest Event", null, null));
        }
        for (QuestEvent e : quest.getChallenges()) {
            decks.add(new DeckProxy(e.getEventDeck(), "Quest Event", null, null));
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static Iterable<DeckProxy> getAllSealedDecks(IStorage<DeckGroup> sealed) {
        final List<DeckProxy> humanDecks = new ArrayList<DeckProxy>();

        // Since AI decks are tied directly to the human choice,
        // they're just mapped in a parallel map and grabbed when the game starts.
        for (final DeckGroup d : sealed) {
            humanDecks.add(new DeckProxy(d, "Sealed", (Function<IHasName, Deck>)(Object)DeckGroup.FN_HUMAN_DECK, GameType.Sealed, sealed));
        }
        return humanDecks;
    }

    public static Iterable<DeckProxy> getAllQuestDecks(IStorage<Deck> storage) {
        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        if (storage != null) {
            for (final Deck deck : storage) {
                decks.add(new DeckProxy(deck, "Quest", GameType.Quest, storage));
            }
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static Iterable<DeckProxy> getDraftDecks(IStorage<DeckGroup> draft) {
        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (DeckGroup d : draft) {
            decks.add(new DeckProxy(d, "Draft", ((Function<IHasName, Deck>)(Object)DeckGroup.FN_HUMAN_DECK), GameType.Draft, draft));
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static Iterable<DeckProxy> getWinstonDecks(IStorage<DeckGroup> draft) {
        ArrayList<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (DeckGroup d : draft) {
            decks.add(new DeckProxy(d, "Winston", ((Function<IHasName, Deck>)(Object)DeckGroup.FN_HUMAN_DECK), GameType.Winston, draft));
        }
        return decks;
    }

    public static final Predicate<DeckProxy> IS_WHITE = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            ColorSet cs = deck.getColor();
            return cs != null && cs.hasAnyColor(MagicColor.WHITE);
        }
    };
    public static final Predicate<DeckProxy> IS_BLUE = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            ColorSet cs = deck.getColor();
            return cs != null && cs.hasAnyColor(MagicColor.BLUE);
        }
    };
    public static final Predicate<DeckProxy> IS_BLACK = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            ColorSet cs = deck.getColor();
            return cs != null && cs.hasAnyColor(MagicColor.BLACK);
        }
    };
    public static final Predicate<DeckProxy> IS_RED = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            ColorSet cs = deck.getColor();
            return cs != null && cs.hasAnyColor(MagicColor.RED);
        }
    };
    public static final Predicate<DeckProxy> IS_GREEN = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            ColorSet cs = deck.getColor();
            return cs != null && cs.hasAnyColor(MagicColor.GREEN);
        }
    };
    public static final Predicate<DeckProxy> IS_COLORLESS = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            ColorSet cs = deck.getColor();
            return cs != null && cs.getColor() == 0;
        }
    };
    public static final Predicate<DeckProxy> IS_MULTICOLOR = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            ColorSet cs = deck.getColor();
            return cs != null && BinaryUtil.bitCount(cs.getColor()) > 1;
        }
    };
}
