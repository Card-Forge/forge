package forge.web.deck;

import forge.StaticData;
import forge.card.CardEdition;
import forge.card.MagicColor;
import forge.deck.ArchetypeDeckGenerator;
import forge.deck.CardPool;
import forge.deck.ColorDeckGenerator;
import forge.deck.CommanderDeckGenerator;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.deck.DeckUrlLoader;
import forge.deck.DeckgenUtil;
import forge.deck.NetDeckCategory;
import forge.deck.io.DeckPreferences;
import forge.game.GameFormat;
import forge.game.GameType;
import forge.gamemodes.quest.QuestController;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.util.Localizer;
import forge.util.MyRandom;
import forge.util.SleeveArt;
import forge.web.protocol.FromBrowser.DeckQueryKind;
import forge.web.protocol.ToBrowser.DeckDetails;
import forge.web.protocol.ToBrowser.DeckSummary;
import forge.web.protocol.ToBrowser.SavedSleeveArt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The decks a seat may choose from, and what the browser shows about each one. */
public final class DeckCatalog {
    /** The tag on a deck the host saved. The browser shows each row's source as a tag. */
    static final String MINE = "yours";
    /** A guest's decks, stored in the guest's browser and keyed by the id it stores them under. */
    static final String DEVICE = "device";
    static final String LINKED = "linked";
    static final String EVENT = "event";
    private static final String PRECON = "precons";
    /** Sources that build a deck when picked, instead of loading a saved deck. */
    private static final String GENERATED = "generated";
    private static final String COMMANDER_GEN = "gen:commander:";
    private static final String NET = "net";
    /** The labels of the colour generator's random choices. Its other choices are colours, which have their own names. */
    private static final Map<String, String> RANDOM_COLOURS = Map.of(
            "Random 1", "lblWebDeckCatalogRandomOne", "Random 2", "lblWebDeckCatalogRandomTwo", "Random 3", "lblWebDeckCatalogRandomThree");
    /** The finder lists a deck's cards without problem marks. The verdict line states the problems. */
    public static final Legality.Result NO_FLAGS = new Legality.Result(Map.of(), null);
    /** Hold this to read or change a deck. A deck loads its sections on first read, and that is not safe from two threads. */
    public static final Object DECKS = new Object();

    private final Map<String, Entry> byKey = new ConcurrentHashMap<>();
    /** The categories that NetDeckCategory loaded for this browser. They are kept so a refresh does not lose them. */
    private final List<NetDeckCategory> netCategories = new ArrayList<>();
    /** The card pool of the last refresh. Generators build from it, and every verdict checks against it. */
    private volatile GameFormat pool;
    /** Whether this catalogue serves a guest's browser. A guest's own decks are the ones on its device. */
    private boolean guest;

    /** A generator's deck is built when first requested and then kept, so the panel and the match see the same cards. */
    private record Entry(DeckProxy proxy, boolean generated, Deck built) {
        Entry(final DeckProxy proxy) {
            this(proxy, false, null);
        }
    }

    /** Asks NetDeckCategory for a net deck category. The host's browser picks one, then it is downloaded. */
    public void loadNetDecks(final GameType format) {
        // Picking waits for the host's answer, so the decks are locked only to record the pick
        final NetDeckCategory category = NetDeckCategory.selectAndLoad(format);
        synchronized (DECKS) {
            if (category != null && !netCategories.contains(category)) {
                netCategories.add(category);
            }
        }
    }

    /** Rebuilds the catalogue and returns its decks. At a Draft table these are event pools. When event is not null, only that event's pools are listed. */
    public List<DeckSummary> refresh(final GameType format, final GameFormat pool, final boolean guest, final Map<String, OnDevice> device,
            final String event) {
        synchronized (DECKS) {
            this.guest = guest;
            // A built generator deck stays built while the pool is unchanged, so the list agrees with the seat that holds it
            final Map<String, Entry> built = new HashMap<>();
            if (pool == this.pool) {
                byKey.forEach((key, e) -> {
                    if (e.generated() && e.built() != null) {
                        built.put(key, e);
                    }
                });
            }
            this.pool = pool;
            byKey.clear();
            final List<DeckSummary> out = new ArrayList<>();
            // Momir Basic and MoJhoSto deal their own decks, so there are none to list
            if (format.isAutoGenerated()) {
                return out;
            }
            if (format == GameType.Draft) {
                if (!guest) {
                    add(out, format, DeckProxy.getAllNetworkEventDecks().stream()
                            .filter(p -> event == null || event.equals(DeckProxy.getEventTag(p.getDeck(), "eventId"))).toList(), EVENT);
                }
                device.forEach((id, d) -> {
                    if (DeckStore.family(d.format()) == GameType.Draft
                            && (event == null || event.equals(DeckProxy.getEventTag(d.deck(), "eventId")))) {
                        addDevice(out, format, id, d.deck());
                    }
                });
                return out;
            }
            // Each commander format has its own decks. Only Commander has its own precons
            add(out, format, switch (format) {
                case Commander -> DeckProxy.getAllCommanderDecks();
                case Oathbreaker -> DeckProxy.getAllOathbreakerDecks();
                case Brawl -> DeckProxy.getAllBrawlDecks();
                case TinyLeaders -> DeckProxy.getAllTinyLeadersDecks();
                default -> DeckProxy.getAllConstructedDecks();
            }, MINE);
            if (format == GameType.Commander) {
                add(out, format, DeckProxy.getAllCommanderPreconDecks(), PRECON);
            }
            if (format == GameType.Commander || format == GameType.Oathbreaker || format == GameType.Brawl || format == GameType.TinyLeaders) {
                addCommanderGenerators(out, format);
            } else if (format == GameType.Constructed) {
                add(out, format, DeckProxy.getAllPreconstructedDecks(QuestController.getPrecons()), PRECON);
                addGenerators(out);
            }
            for (final NetDeckCategory category : netCategories) {
                add(out, format, DeckProxy.getNetDecks(category), NET + " " + category.getName());
            }
            if (!guest) {
                add(out, format, linkedDecks(format), LINKED);
            }
            device.forEach((id, d) -> {
                if (DeckStore.family(d.format()) == DeckStore.family(format)) {
                    addDevice(out, format, id, d.deck());
                }
            });
            built.forEach((key, e) -> byKey.computeIfPresent(key, (k, fresh) -> e));
            return out;
        }
    }

    public Deck deck(final String key) {
        synchronized (DECKS) {
            final Entry e = key == null ? null : byKey.get(key);
            if (e == null) {
                return null;
            }
            // A generator has no deck until one is requested, and it builds a different deck each time
            if (e.built() != null) {
                return e.built();
            }
            final Deck deck = e.proxy() != null ? e.proxy().getDeck() : key.startsWith(COMMANDER_GEN) ? buildCommander(key) : buildColours(key, pool);
            if (e.generated()) {
                byKey.put(key, new Entry(e.proxy(), true, deck));
            }
            return deck;
        }
    }

    /** Replaces the cached copy. Each save makes a new deck object, and a seat must be given that new object. */
    public String adopt(final String tag, final String path, final Deck deck) {
        final String key = DEVICE.equals(tag) ? DEVICE + ":" + path : tag + ":" + path + "/" + deck.getName();
        synchronized (DECKS) {
            byKey.put(key, new Entry(null, false, deck));
        }
        return key;
    }

    /** Whether the deck behind a key cannot be changed in place. A guest may change only the decks in its own browser. */
    static boolean readOnly(final String key, final boolean guest) {
        final String tag = key.substring(0, Math.max(0, key.indexOf(':')));
        return guest ? !DEVICE.equals(tag) : !MINE.equals(tag);
    }

    /** The folder path in a key, or "" for a deck at the top of its format's folder. */
    static String pathOf(final String key) {
        final int colon = key.indexOf(':');
        final int slash = key.lastIndexOf('/');
        return colon < 0 || slash <= colon ? "" : key.substring(colon + 1, slash);
    }

    /** Builds a deck around a random commander, as desktop's Random button picks from its list of every legal commander. */
    private static Deck buildCommander(final String key) {
        // gen:commander:<format>:<random or cardgen>
        final String[] parts = key.split(":");
        // Built so the computer can play it, because the key does not say who will play it. A person can play any deck the computer can
        final List<DeckProxy> legends = CommanderDeckGenerator.getCommanderDecks(GameType.valueOf(parts[2]).getDeckFormat(), true,
                "cardgen".equals(parts[3]));
        return legends.isEmpty() ? null : legends.get(MyRandom.getRandom().nextInt(legends.size())).getDeck();
    }

    /** Builds the colour generator deck for a "gen:color:" key, from the chosen card pool when there is one. */
    private static Deck buildColours(final String key, final GameFormat pool) {
        final List<String> selection = List.of(key.substring(key.lastIndexOf(':') + 1));
        return DeckgenUtil.colorCheck(selection)
                ? DeckgenUtil.buildColorDeck(selection, pool == null ? null : pool.getFilterPrinted(), false) : null;
    }

    /** Writes a card-art sleeve onto a deck and saves it, the way the desktop lobby does. */
    public boolean saveSleeveArt(final String key, final String imageKey, final int offset) {
        synchronized (DECKS) {
            final Entry e = key == null ? null : byKey.get(key);
            final Deck deck = e == null || e.proxy() == null ? null : e.proxy().getDeck();
            if (deck == null) {
                return false;
            }
            deck.setSleeveArtKey(imageKey);
            deck.setSleeveArtOffset(SleeveArt.clampOffset(offset));
            SleeveArt.remember(imageKey, offset);
            return e.proxy().saveDeck();
        }
    }

    /** The chosen deck's card list, grouped as a decklist is read, for the panel next to the results. */
    public DeckDetails details(final String key, final GameType format) {
        synchronized (DECKS) {
            final Deck deck = deck(key);
            if (deck == null) {
                return null;
            }
            return new DeckDetails(key, deck.getName(), DeckFacts.problem(deck, format, pool), DeckFacts.colors(deck), DeckFacts.stats(deck),
                    CardCatalog.groups(deck.getMain(), NO_FLAGS), CardCatalog.cards(deck.get(DeckSection.Sideboard), NO_FLAGS),
                    deck.getSleeveArtKey(),
                    deck.getSleeveArtOffset(),
                    format == GameType.Commander ? DeckFacts.bracket(deck) : null, DeckFacts.aiSkipNames(deck));
        }
    }

    /** The keys of listed decks that hold a matching card. A card matches by name outside or inside the sideboard, or by set code or set name. */
    public List<String> matching(final DeckQueryKind kind, final String value) {
        final String words = value.trim().toLowerCase();
        final CardEdition set = kind == DeckQueryKind.set ? edition(value.trim()) : null;
        final List<String> out = new ArrayList<>();
        if (words.isEmpty() || kind == DeckQueryKind.set && set == null) {
            return out;
        }
        synchronized (DECKS) {
            byKey.forEach((key, e) -> {
                final Deck deck = e.generated() ? null : e.built() != null ? e.built() : e.proxy() == null ? null : e.proxy().getDeck();
                if (deck == null) {
                    return;
                }
                for (final Map.Entry<DeckSection, CardPool> section : deck) {
                    if ((kind == DeckQueryKind.sideboard) != (section.getKey() == DeckSection.Sideboard) && kind != DeckQueryKind.set) {
                        continue;
                    }
                    if (section.getValue().find(c -> set != null ? set.getCode().equals(c.getEdition())
                            : c.getName().toLowerCase().contains(words)) != null) {
                        out.add(key);
                        return;
                    }
                }
            });
        }
        return out;
    }

    /** A set by its code, any of its aliases, or its name. */
    private static CardEdition edition(final String codeOrName) {
        final CardEdition.Collection editions = StaticData.instance().getEditions();
        final CardEdition byCode = editions.get(codeOrName.toUpperCase());
        return byCode != null ? byCode
                : editions.stream().filter(e -> e.getName().equalsIgnoreCase(codeOrName)).findFirst().orElse(null);
    }

    /** Stars a listed deck the way desktop does, in the deck preferences that both clients read. */
    public void setFavourite(final String key, final boolean on) {
        final Entry e = key == null ? null : byKey.get(key);
        if (e == null || e.proxy() == null) {
            return;
        }
        // The stars are a static map that each refresh reads under the same lock
        synchronized (DECKS) {
            DeckPreferences.getPrefs(e.proxy()).setStarCount(on ? 1 : 0);
        }
    }

    /** Generators are listed by name only. Building a deck to fill each row would build hundreds of decks. */
    private void addGenerators(final List<DeckSummary> out) {
        final Localizer text = Localizer.getInstance();
        // The generator needs a deck list only when it builds, to read which colours are picked together
        for (final DeckProxy generator : ColorDeckGenerator.getColorDecks(null, null, false)) {
            final String choice = generator.getName();
            final String key = "gen:color:" + choice;
            // A colour choice's colour is known before it builds a deck. The other generators' colours are not
            final MagicColor.Color colour = MagicColor.Color.fromName(choice);
            byKey.put(key, new Entry(null, true, null));
            generated(out, key, colour != null ? colour.getTranslatedName() : text.getMessage(RANDOM_COLOURS.get(choice)),
                    text.getMessage("lblWebDeckCatalogBuiltOnPick"), colour != null ? colour.getShortName() : "");
        }
        // A theme deck is a fixed list, so it cannot be limited to a card pool
        if (pool == null) {
            for (final DeckProxy theme : DeckProxy.getAllThemeDecks()) {
                byKey.put("gen:theme:" + theme.getName(), new Entry(theme, true, null));
                generated(out, "gen:theme:" + theme.getName(), theme.getName(), text.getMessage("lblWebDeckCatalogThemeDeck"), "");
            }
        }
        if (FModel.isdeckGenMatrixLoaded()) {
            for (final GameFormat f : FModel.getFormats().getSanctionedList()) {
                if (pool != null && f != pool) {
                    continue;
                }
                for (final DeckProxy archetype : ArchetypeDeckGenerator.getMatrixDecks(f, false)) {
                    final String key = "gen:archetype:" + f.getName() + ":" + archetype.getName();
                    byKey.put(key, new Entry(archetype, true, null));
                    generated(out, key, archetype.getName(), text.getMessage("lblWebDeckCatalogArchetype", f.getName()), "");
                }
            }
        }
    }

    private void addCommanderGenerators(final List<DeckSummary> out, final GameType format) {
        final Localizer text = Localizer.getInstance();
        final List<String> kinds = new ArrayList<>(List.of("random"));
        // The card-based generator reads the deck generation matrix, which loads at startup only when the settings ask for it
        if (FModel.isdeckGenMatrixLoaded()) {
            kinds.add("cardgen");
        }
        for (final String kind : kinds) {
            final String key = COMMANDER_GEN + format.name() + ":" + kind;
            byKey.put(key, new Entry(null, true, null));
            generated(out, key, text.getMessage("random".equals(kind) ? "lblWebDeckCatalogRandomCommander" : "lblWebDeckCatalogCardBasedCommander"),
                    text.getMessage("lblWebDeckCatalogBuiltOnPick"), "");
        }
    }

    /** Colours are empty when they are not known until the deck is built. A colour filter treats empty colours as no match. */
    private static void generated(final List<DeckSummary> out, final String key, final String name, final String note,
            final String colours) {
        out.add(new DeckSummary(key, name, GENERATED, colours, true, note, null, null, null, null, null, null, null, true,
                null, null, null, null, null));
    }

    private void add(final List<DeckSummary> out, final GameType format, final Iterable<DeckProxy> source, final String tag) {
        for (final DeckProxy proxy : source) {
            final String key = tag + ":" + proxy.getPath() + "/" + proxy.getName();
            byKey.put(key, new Entry(proxy));
            final Deck deck = proxy.getDeck();
            final DeckFacts.Facts facts = DeckFacts.factsOf(deck, proxy);
            // An illegal deck is listed with its problem, and the finder's legal-only filter decides whether it is shown
            out.add(new DeckSummary(key, proxy.getName(), tag, facts.colors(), null, null, facts.played(),
                    facts.sideboard(), DeckFacts.problemAt(deck, format, pool), facts.legalIn(), facts.formats(),
                    deck.getSleeveArtKey(), deck.getSleeveArtOffset(), readOnly(key, guest),
                    LINKED.equals(tag) ? deck.getSourceUrl() : null, format == GameType.Commander ? DeckFacts.bracketOf(deck) : null, facts.averageMana(),
                    proxy.isFavoriteDeck() ? true : null, DeckFacts.aiSkips(deck)));
        }
    }

    private void addDevice(final List<DeckSummary> out, final GameType format, final String id, final Deck deck) {
        final String key = DEVICE + ":" + id;
        byKey.put(key, new Entry(null, false, deck));
        final DeckFacts.Facts facts = DeckFacts.factsOf(deck, null);
        out.add(new DeckSummary(key, deck.getName(), DEVICE, facts.colors(), null, null, facts.played(),
                facts.sideboard(), DeckFacts.problemAt(deck, format, pool), facts.legalIn(), null,
                deck.getSleeveArtKey(), deck.getSleeveArtOffset(), false, null, format == GameType.Commander ? DeckFacts.bracketOf(deck) : null,
                facts.averageMana(), null, DeckFacts.aiSkips(deck)));
    }

    /** The decks loaded from links that belong to this format. A deck uses the format it records, or Commander when it has a commander. */
    private static List<DeckProxy> linkedDecks(final GameType format) {
        final List<DeckProxy> out = new ArrayList<>();
        for (final DeckProxy proxy : DeckUrlLoader.getUrlDecks()) {
            final Deck deck = proxy.getDeck();
            final GameType family = deck.getDeckFormat() != null ? DeckStore.family(deck.getDeckFormat())
                    : deck.has(DeckSection.Commander) ? GameType.Commander : GameType.Constructed;
            if (family == DeckStore.family(format)) {
                out.add(proxy);
            }
        }
        return out;
    }

    /** The card-art sleeves already saved, shared with the desktop client. */
    public static List<SavedSleeveArt> savedSleeveArt() {
        final List<SavedSleeveArt> out = new ArrayList<>();
        final Map<String, Integer> library = SleeveArt.parseLibrary(
                FModel.getPreferences().getPref(FPref.UI_SLEEVE_ART_LIBRARY));
        for (final Map.Entry<String, Integer> e : library.entrySet()) {
            out.add(new SavedSleeveArt(e.getKey(), e.getValue()));
        }
        return out;
    }

    /** A deck that a guest stores in its browser, and the format it was built for. */
    public record OnDevice(Deck deck, GameType format) {
    }
}
