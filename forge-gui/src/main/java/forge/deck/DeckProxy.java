package forge.deck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCostShard;
import forge.deck.io.DeckPreferences;
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

// Adding a generic to this class creates compile problems in ItemManager (that I can not fix)
public class DeckProxy implements InventoryItem {
    protected IHasName deck;
    protected final String deckType;
    protected final IStorage<? extends IHasName> storage;

    public static final Function<DeckProxy, String> FN_GET_NAME = new Function<DeckProxy, String>() {
        @Override
        public String apply(final DeckProxy arg0) {
            return arg0.getName();
        }
    };

    // cached values
    protected ColorSet color;
    protected ColorSet colorIdentity;
    protected Iterable<GameFormat> formats;
    private Integer mainSize = null;
    private Integer sbSize = null;
    private final String path;
    private final Function<IHasName, Deck> fnGetDeck;
    private CardEdition edition;
    private CardRarity highestRarity;

    protected DeckProxy() {
        this(null, "", null, "", null, null);
    }

    public DeckProxy(final Deck deck, final String deckType, final GameType type, final IStorage<? extends IHasName> storage) {
        this(deck, deckType, type, "", storage, null);
    }

    public DeckProxy(final IHasName deck, final String deckType, final Function<IHasName, Deck> fnGetDeck, final GameType type, final IStorage<? extends IHasName> storage) {
        this(deck, deckType, type, "", storage, fnGetDeck);
    }

    private DeckProxy(final IHasName deck, final String deckType, final GameType type, final String path, final IStorage<? extends IHasName> storage, final Function<IHasName, Deck> fnGetDeck) {
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

    public static String getDeckString(final String path, final String name) {
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
        mainSize = null;
        sbSize = null;
    }

    public ColorSet getColor() {
        if (color == null && !isGeneratedDeck()) {
            byte colorProfile = MagicColor.COLORLESS;
            byte landProfile = MagicColor.COLORLESS;
            Set<Byte> nonReqColors = null;

            for (final Entry<DeckSection, CardPool> deckEntry : getDeck()) {
                switch (deckEntry.getKey()) {
                case Main:
                case Commander:
                    for (final Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
                        final CardRules rules = poolEntry.getKey().getRules();
                        if (rules.getType().isLand()) { //track color identity of lands separately
                            landProfile |= rules.getColorIdentity().getColor();
                        }
                        else {
                            for (final ManaCostShard shard : rules.getManaCost()) {
                                //track phyrexian and hybrid costs separately as they won't always affect color
                                if (shard.isPhyrexian() || shard.isOr2Colorless() || !shard.isMonoColor()) {
                                    if (nonReqColors == null) {
                                        nonReqColors = new HashSet<Byte>();
                                    }
                                    nonReqColors.add(shard.getColorMask());
                                }
                                else {
                                    colorProfile |= shard.getColorMask();
                                }
                            }
                        }
                    }
                    break;
                default:
                    break; //ignore other sections
                }
            }
            if (nonReqColors != null) {
                //if any non-required mana colors present, determine which colors, if any,
                //need to be accounted for in color profile of deck
                for (final Byte colorMask : nonReqColors) {
                    colorProfile |= (colorMask & landProfile);
                }
            }
            color = ColorSet.fromMask(colorProfile);
        }
        return color;
    }

    public ColorSet getColorIdentity() {
        if (colorIdentity == null) {
            byte colorProfile = MagicColor.COLORLESS;

            for (final Entry<DeckSection, CardPool> deckEntry : getDeck()) {
                switch (deckEntry.getKey()) {
                case Main:
                case Sideboard:
                case Commander:
                    for (final Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
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
            for (final Entry<DeckSection, CardPool> deckEntry : getDeck()) {
                switch (deckEntry.getKey()) {
                case Main:
                case Sideboard:
                case Commander:
                    for (final Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
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
        if (mainSize == null) {
            if (deck == null) {
                mainSize = -1;
            }
            else {
                final Deck d = getDeck();
                mainSize = d.getMain().countAll();

                //account for commander as part of main deck size
                final CardPool commander = d.get(DeckSection.Commander);
                if (commander != null) {
                    mainSize += commander.countAll();
                }
            }
        }
        return mainSize;
    }

    public int getSideSize() {
        if (sbSize == null) {
            final CardPool sb = getDeck().get(DeckSection.Sideboard);
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

    public boolean isFavoriteDeck() {
        return DeckPreferences.getPrefs(this).getStarCount() > 0;
    }

    // TODO: The methods below should not take the decks collections from singletons, instead they are supposed to use data passed in parameters
    public static Iterable<DeckProxy> getAllConstructedDecks() {
        final List<DeckProxy> result = new ArrayList<DeckProxy>();
        addDecksRecursivelly("Constructed", GameType.Constructed, result, "", FModel.getDecks().getConstructed());
        return result;
    }

    public static Iterable<DeckProxy> getAllCommanderDecks() {
        final List<DeckProxy> result = new ArrayList<DeckProxy>();
        addDecksRecursivelly("Commander", GameType.Commander, result, "", FModel.getDecks().getCommander());
        return result;
    }

    public static Iterable<DeckProxy> getAllSchemeDecks() {
        final List<DeckProxy> result = new ArrayList<DeckProxy>();
        addDecksRecursivelly("Scheme", GameType.Archenemy, result, "", FModel.getDecks().getScheme());
        return result;
    }

    public static Iterable<DeckProxy> getAllPlanarDecks() {
        final List<DeckProxy> result = new ArrayList<DeckProxy>();
        addDecksRecursivelly("Plane", GameType.Planechase, result, "", FModel.getDecks().getPlane());
        return result;
    }

    private static void addDecksRecursivelly(final String deckType, final GameType gameType, final List<DeckProxy> list, final String path, final IStorage<Deck> folder) {
        for (final IStorage<Deck> f : folder.getFolders()) {
            final String subPath = (StringUtils.isBlank(path) ? "" : path) + "/" + f.getName();
            addDecksRecursivelly(deckType, gameType, list, subPath, f);
        }

        for (final Deck d : folder) {
            list.add(new DeckProxy(d, deckType, gameType, path, folder, null));
        }
    }

    // Consider using a direct predicate to manage DeckProxies (not this tunnel to collection of paper cards)
    public static final Predicate<DeckProxy> createPredicate(final Predicate<PaperCard> cardPredicate) {
        return new Predicate<DeckProxy>() {
            @Override
            public boolean apply(final DeckProxy input) {
                for (final Entry<DeckSection, CardPool> deckEntry : input.getDeck()) {
                    switch (deckEntry.getKey()) {
                    case Main:
                    case Sideboard:
                    case Commander:
                        for (final Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
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

    public void deleteFromStorage() {
        if (storage != null) {
            storage.delete(getName());
        }
    }

    private static class ThemeDeckGenerator extends DeckProxy {
        private final String name;
        public ThemeDeckGenerator(final String name0) {
            super();
            name = name0;
        }

        @Override
        public Deck getDeck() {
            final DeckGeneratorTheme gen = new DeckGeneratorTheme(FModel.getMagicDb().getCommonCards());
            final Deck deck = new Deck();
            gen.setSingleton(FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_SINGLETONS));
            gen.setUseArtifacts(!FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
            final StringBuilder errorBuilder = new StringBuilder();
            deck.getMain().addAll(gen.getThemeDeck(this.getName(), 60, errorBuilder));
            if (errorBuilder.length() > 0) {
                throw new RuntimeException(errorBuilder.toString());
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

        @Override
        public boolean isGeneratedDeck() {
            return true;
        }
    }

    public static List<DeckProxy> getAllThemeDecks() {
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (final String s : DeckGeneratorTheme.getThemeNames()) {
            decks.add(new ThemeDeckGenerator(s));
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static List<DeckProxy> getAllPreconstructedDecks(final IStorage<PreconDeck> iStorage) {
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (final PreconDeck preconDeck : iStorage) {
            decks.add(new DeckProxy(preconDeck, "Precon", (Function<IHasName, Deck>)(Object)PreconDeck.FN_GET_DECK, null, iStorage));
        }
        return decks;
    }

    public static List<DeckProxy> getAllQuestEventAndChallenges() {
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();
        final QuestController quest = FModel.getQuest();
        for (final QuestEvent e : quest.getDuelsManager().getAllDuels()) {
            decks.add(new DeckProxy(e.getEventDeck(), "Quest Event", null, null));
        }
        for (final QuestEvent e : quest.getChallenges()) {
            decks.add(new DeckProxy(e.getEventDeck(), "Quest Event", null, null));
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static List<DeckProxy> getAllSealedDecks() {
        final List<DeckProxy> humanDecks = new ArrayList<DeckProxy>();
        final IStorage<DeckGroup> sealed = FModel.getDecks().getSealed();

        // Since AI decks are tied directly to the human choice,
        // they're just mapped in a parallel map and grabbed when the game starts.
        for (final DeckGroup d : sealed) {
            humanDecks.add(new DeckProxy(d, "Sealed", (Function<IHasName, Deck>)(Object)DeckGroup.FN_HUMAN_DECK, GameType.Sealed, sealed));
        }
        return humanDecks;
    }

    public static List<DeckProxy> getAllQuestDecks(final IStorage<Deck> storage) {
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();
        if (storage != null) {
            for (final Deck deck : storage) {
                decks.add(new DeckProxy(deck, "Quest", GameType.Quest, storage));
            }
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static List<DeckProxy> getAllDraftDecks() {
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();
        final IStorage<DeckGroup> draft = FModel.getDecks().getDraft();
        for (final DeckGroup d : draft) {
            decks.add(new DeckProxy(d, "Draft", ((Function<IHasName, Deck>)(Object)DeckGroup.FN_HUMAN_DECK), GameType.Draft, draft));
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static List<DeckProxy> getWinstonDecks(final IStorage<DeckGroup> draft) {
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();
        for (final DeckGroup d : draft) {
            decks.add(new DeckProxy(d, "Winston", ((Function<IHasName, Deck>)(Object)DeckGroup.FN_HUMAN_DECK), GameType.Winston, draft));
        }
        return decks;
    }

    public static List<DeckProxy> getNetDecks(final NetDeckCategory category) {
        final List<DeckProxy> decks = new ArrayList<DeckProxy>();
        if (category != null) {
            addDecksRecursivelly("Constructed", GameType.Constructed, decks, "", category);
        }
        return decks;
    }

    public static final Predicate<DeckProxy> IS_WHITE = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            final ColorSet cs = deck.getColor();
            return cs != null && cs.hasAnyColor(MagicColor.WHITE);
        }
    };
    public static final Predicate<DeckProxy> IS_BLUE = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            final ColorSet cs = deck.getColor();
            return cs != null && cs.hasAnyColor(MagicColor.BLUE);
        }
    };
    public static final Predicate<DeckProxy> IS_BLACK = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            final ColorSet cs = deck.getColor();
            return cs != null && cs.hasAnyColor(MagicColor.BLACK);
        }
    };
    public static final Predicate<DeckProxy> IS_RED = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            final ColorSet cs = deck.getColor();
            return cs != null && cs.hasAnyColor(MagicColor.RED);
        }
    };
    public static final Predicate<DeckProxy> IS_GREEN = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            final ColorSet cs = deck.getColor();
            return cs != null && cs.hasAnyColor(MagicColor.GREEN);
        }
    };
    public static final Predicate<DeckProxy> IS_COLORLESS = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            final ColorSet cs = deck.getColor();
            return cs != null && cs.getColor() == 0;
        }
    };
    public static final Predicate<DeckProxy> IS_MULTICOLOR = new Predicate<DeckProxy>() {
        @Override
        public boolean apply(final DeckProxy deck) {
            final ColorSet cs = deck.getColor();
            return cs != null && BinaryUtil.bitCount(cs.getColor()) > 1;
        }
    };
}
