package forge.deck;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import forge.StaticData;
import forge.card.*;
import forge.card.mana.ManaCostShard;
import forge.deck.io.DeckPreferences;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.gamemodes.quest.QuestController;
import forge.gamemodes.quest.QuestEvent;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.item.PreconDeck;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.BinaryUtil;
import forge.util.IHasName;
import forge.util.storage.IStorage;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.Map.Entry;

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
    protected Set<GameFormat> formats;
    protected Set<GameFormat> exhaustiveFormats;
    private Integer mainSize = null;
    private Integer sbSize = null;
    private Integer avgCMC = null;
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
                edition = StaticData.instance().getEditions().getTheLatestOfAllTheOriginalEditionsOfCardsIn(getDeck().getAllCardsInASinglePool());
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

    public static String getDeckDirectory(final IStorage<?> currentFolder) {
        String directory = currentFolder.getFullPath();
        if (directory.startsWith(ForgeConstants.DECK_BASE_DIR)) {
            //trim deck base directory from start of directory path
            directory = directory.substring(ForgeConstants.DECK_BASE_DIR.length());
        }
        return directory;
    }

    public void invalidateCache() {
        color = null;
        colorIdentity = null;
        highestRarity = null;
        formats = null;
        exhaustiveFormats = null;
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
                                if (shard.isPhyrexian() || shard.isOr2Generic() || !shard.isMonoColor()) {
                                    if (nonReqColors == null) {
                                        nonReqColors = new HashSet<>();
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

    public PaperCard getHighestCMCCard() {
        PaperCard key = null;
        Map<PaperCard, Integer> keyCMC = new HashMap<>(64);

        for (final Entry <PaperCard, Integer> pc : getDeck().getAllCardsInASinglePool()) {
            if (pc.getKey().getRules().getManaCost() != null) {
                if (pc.getKey().getRules().getType().hasSubtype("Saga") || pc.getKey().getRules().getType().hasSubtype("Class") || CardSplitType.Split.equals(pc.getKey().getRules().getSplitType()))
                    continue;
                    keyCMC.put(pc.getKey(),pc.getKey().getRules().getManaCost().getCMC());
            }
        }

        if (!keyCMC.isEmpty()) {
            int max = Collections.max(keyCMC.values());
            //get any max cmc
            for (Entry<PaperCard, Integer> entry : keyCMC.entrySet()) {
                if (entry.getValue()==max) {
                    return entry.getKey();
                }
            }
        }
        return key;
    }

    public Set<GameFormat> getFormats() {
        if (formats == null) {
            formats = FModel.getFormats().getAllFormatsOfDeck(getDeck());
        }
        return formats;
    }

    public Set<GameFormat> getExhaustiveFormats() {
        if (exhaustiveFormats == null) {
            exhaustiveFormats = FModel.getFormats().getAllFormatsOfDeck(getDeck(), true);
        }
        return exhaustiveFormats;
    }

    public String getFormatsString() {
        Set<GameFormat> formats = getFormats();
        if (formats.size() > 1)
            return StringUtils.join(Iterables.transform(formats, GameFormat.FN_GET_NAME), ", ");
        Object[] formatArray = formats.toArray();
        GameFormat format = (GameFormat)formatArray[0];
        if (format != GameFormat.NoFormat)
            return format.getName();
        if (isCustomDeckFormat())
            return "Custom Cards Deck";
        return "No Format";
    }

    private boolean isCustomDeckFormat(){
        Deck deck = this.getDeck();
        CardPool cards = deck.getAllCardsInASinglePool();
        CardEdition.Collection customEditions = StaticData.instance().getCustomEditions();
        for (Entry<PaperCard, Integer> entry : cards){
            String setCode = entry.getKey().getEdition();
            if (customEditions.contains(setCode))
                return true;
        }
        return false;
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

    public static int getAverageCMC(Deck deck) {
        int totalCMC = 0;
        int totalCount = 0;
        for (final Entry<DeckSection, CardPool> deckEntry : deck) {
            switch (deckEntry.getKey()) {
            case Main:
            case Commander:
                for (final Entry<PaperCard, Integer> poolEntry : deckEntry.getValue()) {
                    CardRules rules = poolEntry.getKey().getRules();
                    CardType type = rules.getType();
                    if (!type.isLand() && (type.isArtifact() || type.isCreature() || type.isEnchantment() || type.isPlaneswalker() || type.isInstant() || type.isSorcery())) {
                        totalCMC += rules.getManaCost().getCMC();
                        totalCount++;
                    }
                }
                break;
            default:
                break; //ignore other sections
            }
        }
        return Math.round(totalCMC / totalCount);
    }

    public Integer getAverageCMC() {
        if (avgCMC == null) {
            avgCMC = getAverageCMC(getDeck());
        }
        return avgCMC;
    }

    public boolean isGeneratedDeck() {
        return false;
    }

    public boolean isFavoriteDeck() {
        return DeckPreferences.getPrefs(this).getStarCount() > 0;
    }

    public static Iterable<DeckProxy> getAllConstructedDecks() {
        return getAllConstructedDecks(null);
    }
    public static Iterable<DeckProxy> getAllConstructedDecks(final Predicate<Deck> filter) {
        final List<DeckProxy> result = new ArrayList<>();
        addDecksRecursivelly("Constructed", GameType.Constructed, result, "", FModel.getDecks().getConstructed(), filter);
        return result;
    }

    public static Iterable<DeckProxy> getAllCommanderDecks() {
        return getAllCommanderDecks(null);
    }
    public static Iterable<DeckProxy> getAllCommanderDecks(final Predicate<Deck> filter) {
        final List<DeckProxy> result = new ArrayList<>();
        addDecksRecursivelly("Commander", GameType.Commander, result, "", FModel.getDecks().getCommander(), filter);
        return result;
    }

    public static Iterable<DeckProxy> getAllOathbreakerDecks() {
        return getAllOathbreakerDecks(null);
    }
    public static Iterable<DeckProxy> getAllOathbreakerDecks(final Predicate<Deck> filter) {
        final List<DeckProxy> result = new ArrayList<>();
        addDecksRecursivelly("Oathbreaker", GameType.Oathbreaker, result, "", FModel.getDecks().getOathbreaker(), filter);
        return result;
    }

    public static Iterable<DeckProxy> getAllCommanderPreconDecks() {
        return getAllCommanderPreconDecks(null);
    }
    public static Iterable<DeckProxy> getAllCommanderPreconDecks(final Predicate<Deck> filter) {
        final List<DeckProxy> result = new ArrayList<DeckProxy>();
        addDecksRecursivelly("Commander Precon", GameType.Commander, result, "", FModel.getDecks().getCommanderPrecons(), filter);
        return result;
    }

    public static Iterable<DeckProxy> getAllTinyLeadersDecks() {
        return getAllTinyLeadersDecks(null);
    }
    public static Iterable<DeckProxy> getAllTinyLeadersDecks(Predicate<Deck> filter) {
        final List<DeckProxy> result = new ArrayList<>();
        if (filter == null) {
            filter = DeckFormat.TinyLeaders.hasLegalCardsPredicate();
        }
        else {
            filter = Predicates.and(DeckFormat.TinyLeaders.hasLegalCardsPredicate(), filter);
        }
        addDecksRecursivelly("Tiny Leaders", GameType.TinyLeaders, result, "", FModel.getDecks().getTinyLeaders(), filter);
        return result;
    }

    public static Iterable<DeckProxy> getAllBrawlDecks() {
        return getAllBrawlDecks(null);
    }
    public static Iterable<DeckProxy> getAllBrawlDecks(Predicate<Deck> filter) {
        final List<DeckProxy> result = new ArrayList<>();
        if (filter == null) {
            filter = DeckFormat.Brawl.hasLegalCardsPredicate();
        }
        else {
            filter = Predicates.and(DeckFormat.Brawl.hasLegalCardsPredicate(), filter);
        }
        addDecksRecursivelly("Brawl", GameType.Brawl, result, "", FModel.getDecks().getBrawl(), filter);
        return result;
    }

    public static Iterable<DeckProxy> getAllSchemeDecks() {
        return getAllSchemeDecks(null);
    }
    public static Iterable<DeckProxy> getAllSchemeDecks(final Predicate<Deck> filter) {
        final List<DeckProxy> result = new ArrayList<>();
        addDecksRecursivelly("Scheme", GameType.Archenemy, result, "", FModel.getDecks().getScheme(), filter);
        return result;
    }

    public static Iterable<DeckProxy> getAllPlanarDecks() {
        return getAllPlanarDecks(null);
    }
    public static Iterable<DeckProxy> getAllPlanarDecks(final Predicate<Deck> filter) {
        final List<DeckProxy> result = new ArrayList<>();
        addDecksRecursivelly("Plane", GameType.Planechase, result, "", FModel.getDecks().getPlane(), filter);
        return result;
    }

    private static void addDecksRecursivelly(final String deckType, final GameType gameType, final List<DeckProxy> list, final String path, final IStorage<Deck> folder, final Predicate<Deck> filter) {
        for (final IStorage<Deck> f : folder.getFolders()) {
            final String subPath = (StringUtils.isBlank(path) ? "" : path) + "/" + f.getName();
            addDecksRecursivelly(deckType, gameType, list, subPath, f, filter);
        }

        for (final Deck d : folder) {
            if (filter == null || filter.apply(d)) {
                list.add(new DeckProxy(d, deckType, gameType, path, folder, null));
            }
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
        final List<DeckProxy> decks = new ArrayList<>();
        for (final String s : DeckGeneratorTheme.getThemeNames()) {
            decks.add(new ThemeDeckGenerator(s));
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static List<DeckProxy> getAllPreconstructedDecks(final IStorage<PreconDeck> iStorage) {
        final List<DeckProxy> decks = new ArrayList<>();
        for (final PreconDeck preconDeck : iStorage) {
            decks.add(new DeckProxy(preconDeck, "Precon", (Function<IHasName, Deck>)(Object)PreconDeck.FN_GET_DECK, null, iStorage));
        }
        return decks;
    }

    public static List<DeckProxy> getAllQuestEventAndChallenges() {
        final List<DeckProxy> decks = new ArrayList<>();
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
        final List<DeckProxy> humanDecks = new ArrayList<>();
        final IStorage<DeckGroup> sealed = FModel.getDecks().getSealed();

        // Since AI decks are tied directly to the human choice,
        // they're just mapped in a parallel map and grabbed when the game starts.
        for (final DeckGroup d : sealed) {
            humanDecks.add(new DeckProxy(d, "Sealed", (Function<IHasName, Deck>)(Object)DeckGroup.FN_HUMAN_DECK, GameType.Sealed, sealed));
        }
        return humanDecks;
    }

    public static List<DeckProxy> getAllQuestDecks(final IStorage<Deck> storage) {
        final List<DeckProxy> decks = new ArrayList<>();
        if (storage != null) {
            for (final Deck deck : storage) {
                decks.add(new DeckProxy(deck, "Quest", GameType.Quest, storage));
            }
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static List<DeckProxy> getAllDraftDecks() {
        final List<DeckProxy> decks = new ArrayList<>();
        final IStorage<DeckGroup> draft = FModel.getDecks().getDraft();
        for (final DeckGroup d : draft) {
            decks.add(new DeckProxy(d, "Draft", ((Function<IHasName, Deck>)(Object)DeckGroup.FN_HUMAN_DECK), GameType.Draft, draft));
        }
        return decks;
    }

    @SuppressWarnings("unchecked")
    public static List<DeckProxy> getWinstonDecks(final IStorage<DeckGroup> draft) {
        final List<DeckProxy> decks = new ArrayList<>();
        for (final DeckGroup d : draft) {
            decks.add(new DeckProxy(d, "Winston", ((Function<IHasName, Deck>)(Object)DeckGroup.FN_HUMAN_DECK), GameType.Winston, draft));
        }
        return decks;
    }

    public static List<DeckProxy> getNetDecks(final NetDeckCategory category) {
        final List<DeckProxy> decks = new ArrayList<>();
        if (category != null) {
            addDecksRecursivelly("Constructed", GameType.Constructed, decks, "", category, null);
        }
        return decks;
    }

    public static List<DeckProxy> getNetArchiveStandardDecks(final NetDeckArchiveStandard category) {
        final List<DeckProxy> decks = new ArrayList<>();
        if (category != null) {
            addDecksRecursivelly("Constructed", GameType.Constructed, decks, "", category, null);
        }
        return decks;
    }

    public static List<DeckProxy> getNetArchiveModernDecks(final NetDeckArchiveModern category) {
        final List<DeckProxy> decks = new ArrayList<>();
        if (category != null) {
            addDecksRecursivelly("Constructed", GameType.Constructed, decks, "", category, null);
        }
        return decks;
    }
    public static List<DeckProxy> getNetArchivePioneerDecks(final NetDeckArchivePioneer category) {
        final List<DeckProxy> decks = new ArrayList<>();
        if (category != null) {
            addDecksRecursivelly("Constructed", GameType.Constructed, decks, "", category, null);
        }
        return decks;
    }

    public static List<DeckProxy> getNetArchivePauperDecks(final NetDeckArchivePauper category) {
        final List<DeckProxy> decks = new ArrayList<>();
        if (category != null) {
            addDecksRecursivelly("Constructed", GameType.Constructed, decks, "", category, null);
        }
        return decks;
    }

    public static List<DeckProxy> getNetArchiveLegacyDecks(final NetDeckArchiveLegacy category) {
        final List<DeckProxy> decks = new ArrayList<>();
        if (category != null) {
            addDecksRecursivelly("Constructed", GameType.Constructed, decks, "", category, null);
        }
        return decks;
    }

    public static List<DeckProxy> getNetArchiveVintageDecks(final NetDeckArchiveVintage category) {
        final List<DeckProxy> decks = new ArrayList<>();
        if (category != null) {
            addDecksRecursivelly("Constructed", GameType.Constructed, decks, "", category, null);
        }
        return decks;
    }

    public static List<DeckProxy> getNetArchiveBlockDecks(final NetDeckArchiveBlock category) {
        final List<DeckProxy> decks = new ArrayList<>();
        if (category != null) {
            addDecksRecursivelly("Constructed", GameType.Constructed, decks, "", category, null);
        }
        return decks;
    }

    public static CardEdition getDefaultLandSet(Deck deck) {
        List<CardEdition> availableEditions = new ArrayList<>();

        for (PaperCard c : deck.getAllCardsInASinglePool().toFlatList()) {
            availableEditions.add(FModel.getMagicDb().getEditions().get(c.getEdition()));
        }

        CardEdition randomLandSet = CardEdition.Predicates.getRandomSetWithAllBasicLands(availableEditions);
        return randomLandSet == null ? FModel.getMagicDb().getEditions().get("ZEN") : randomLandSet;
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

    @Override
    public String getImageKey(boolean altState) {
        return null;
    }
}
