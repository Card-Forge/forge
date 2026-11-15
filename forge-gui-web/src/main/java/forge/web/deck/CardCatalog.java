package forge.web.deck;

import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.deck.DeckFormat;
import forge.deck.DeckSection;
import forge.deck.CardPool;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.itemmanager.AdvancedSearchParser;
import forge.itemmanager.SFilterUtil;
import forge.model.FModel;
import forge.util.Localizer;
import forge.web.images.Foil;
import forge.web.protocol.PropertyJson;
import forge.web.protocol.ToBrowser;
import forge.web.protocol.ToBrowser.CataloguePage;
import forge.web.protocol.ToBrowser.CatalogueRow;
import forge.web.protocol.ToBrowser.EditorCard;
import forge.web.protocol.ToBrowser.EditorPrinting;
import forge.web.protocol.ToBrowser.EditorGroup;
import forge.web.protocol.ToBrowser.Printing;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/** Every card that a deck can be built from. The catalogue is built once and shared by every browser. */
public final class CardCatalog {
    static final int PAGE = 60;
    /** The order in which a decklist is written. The headings and the type sort use this order. */
    static final List<String> HEADINGS = List.of("Creatures", "Planeswalkers", "Instants", "Sorceries", "Artifacts",
            "Enchantments", "Battles", "Lands");
    private static final Map<DeckSection, CardCatalog> SECTIONS = new ConcurrentHashMap<>();
    private static final Pattern NOT_NAME_CHARACTER = Pattern.compile("[^a-z0-9 ]");
    private final List<Row> rows;

    /** filters is the search syntax that the filter bar's chips add. It is read separately from the text, so a typed name is still ranked. */
    public record Query(String text, String colours, String type, String filters, String sort, int offset, boolean showAll) {
    }

    private record Row(PaperCard card, String normalised, String colours, String cost, int mv, String heading) {
    }

    private CardCatalog(final List<Row> rows) {
        this.rows = rows;
    }

    /** A sealed or draft pool as a catalogue. It holds only these cards, from any section. */
    public static CardCatalog of(final Iterable<PaperCard> cards) {
        final List<Row> rows = new ArrayList<>();
        cards.forEach(card -> rows.add(row(card)));
        return new CardCatalog(rows);
    }

    static CardCatalog get() {
        return Holder.INSTANCE;
    }

    /** The cards for a section other than the main deck, such as planes or schemes. Built the first time it is requested. */
    public static CardCatalog of(final DeckSection section) {
        return SECTIONS.computeIfAbsent(section, s -> {
            FModel.getMagicDb().ensureAllCardsLoaded();
            final List<Row> rows = new ArrayList<>();
            for (final CardDb db : List.of(StaticData.instance().getCommonCards(), StaticData.instance().getVariantCards())) {
                for (final PaperCard card : db.getUniqueCards()) {
                    if (!card.getRules().isUnsupported() && DeckSection.matchingSection(card) == s) {
                        rows.add(row(card));
                    }
                }
            }
            return new CardCatalog(rows);
        });
    }

    public static ToBrowser.CardSets sets() {
        return Sets.ALL;
    }

    private static CardCatalog build() {
        // Card scripts may load lazily, and every card must be read before it can be listed
        FModel.getMagicDb().ensureAllCardsLoaded();
        final List<Row> rows = new ArrayList<>();
        for (final PaperCard card : StaticData.instance().getCommonCards().getUniqueCards()) {
            final CardRules rules = card.getRules();
            // Planes, schemes, attractions and similar cards have their own sections and never go in a main deck
            if (rules.isUnsupported() || rules.isVariant() || rules.isCustom()
                    || DeckSection.matchingSection(card) != DeckSection.Main) {
                continue;
            }
            rows.add(row(card));
        }
        return new CardCatalog(rows);
    }

    private static Row row(final PaperCard card) {
        final CardRules rules = card.getRules();
        return new Row(card, normalize(card.getName()), letters(rules.getColor()), PropertyJson.manaCost(rules.getManaCost()),
                rules.getManaCost().getCMC(), heading(card));
    }

    /** A card that problemOf finds a problem with is left out, unless the query asks for everything. commanderOnly may be null. */
    public CataloguePage query(final int request, final Query q, final Function<PaperCard, String> problemOf,
            final Predicate<PaperCard> commanderOnly, final ToIntFunction<String> inDeck) {
        final String typed = q.text() == null ? "" : q.text().trim();
        // Desktop's own parser reads search syntax such as c:bg or mv<=3. Plain text is a name, ranked the way desktop ranks names
        final Predicate<PaperCard> syntax = usesSyntax(typed) ? SFilterUtil.buildTextFilter(typed, false, true, false, false, false) : null;
        final String text = syntax == null ? normalize(typed) : "";
        final Predicate<PaperCard> chips = q.filters() == null || q.filters().isBlank() ? null
                : SFilterUtil.buildTextFilter(q.filters(), false, true, false, false, false);
        final List<Row> startsWith = new ArrayList<>();
        final List<Row> matches = new ArrayList<>();
        final List<String> problems = new ArrayList<>();
        int hidden = 0;
        for (final Row row : rows) {
            if ((syntax != null && !syntax.test(row.card())) || (chips != null && !chips.test(row.card())) || (!text.isEmpty() && !row.normalised().contains(text)) || !passes(row, q)
                    || (commanderOnly != null && !commanderOnly.test(row.card()))) {
                continue;
            }
            if (!q.showAll() && problemOf.apply(row.card()) != null) {
                hidden++;
                continue;
            }
            (!text.isEmpty() && row.normalised().startsWith(text) ? startsWith : matches).add(row);
        }
        final List<Row> ordered;
        if (text.isEmpty()) {
            matches.sort(order(q.sort()));
            ordered = matches;
        } else {
            // The same rule as the browser's own search boxes: names that start with the text come first, shortest first
            startsWith.sort(Comparator.comparingInt(r -> r.card().getName().length()));
            startsWith.addAll(matches);
            ordered = startsWith;
        }
        final int from = Math.min(Math.max(0, q.offset()), ordered.size());
        final List<CatalogueRow> page = new ArrayList<>();
        for (final Row row : ordered.subList(from, Math.min(from + PAGE, ordered.size()))) {
            page.add(toBrowser(row, problemOf.apply(row.card()), inDeck.applyAsInt(row.card().getName())));
        }
        return new CataloguePage(request, page, ordered.size(), from, ordered.isEmpty() ? hidden : 0, !text.isEmpty());
    }

    /** Whether a query uses Forge's search syntax. That means a term the advanced parser reads, an "or", a bracket, or a term excluded with a minus. */
    static boolean usesSyntax(final String text) {
        for (final String term : text.split("\\s+")) {
            if ((term.startsWith("-") && term.length() > 1) || term.contains("|") || term.contains("(") || term.contains(")")
                    || "or".equalsIgnoreCase(term) || AdvancedSearchParser.parseAdvancedRulesToken(term) != null
                    || AdvancedSearchParser.parseAdvancedPaperCardToken(term) != null) {
                return true;
            }
        }
        return false;
    }

    /** The card name closest to a misspelt one: at most three edits away, and no more than a third of its length. */
    String closestName(final String misspelt) {
        final String wanted = normalize(misspelt);
        final int most = Math.min(3, wanted.length() / 3);
        String best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (final Row row : rows) {
            final int distance = StringUtils.getLevenshteinDistance(wanted, row.normalised(), most);
            if (distance >= 0 && distance < bestDistance) {
                best = row.card().getName();
                bestDistance = distance;
            }
        }
        return best;
    }

    private static boolean passes(final Row row, final Query q) {
        final String colours = q.colours() == null ? "" : q.colours();
        if (!colours.isEmpty()) {
            final boolean any = row.colours().chars().anyMatch(c -> colours.indexOf(c) >= 0);
            if (!any) {
                return false;
            }
        }
        if (!"any".equals(q.type()) && q.type() != null && !isType(row.card().getRules().getType(), q.type())) {
            return false;
        }
        return true;
    }

    private static boolean isType(final CardType type, final String wanted) {
        return switch (wanted) {
            case "creature" -> type.isCreature();
            case "planeswalker" -> type.isPlaneswalker();
            case "instant" -> type.isInstant();
            case "sorcery" -> type.isSorcery();
            case "artifact" -> type.isArtifact();
            case "enchantment" -> type.isEnchantment();
            case "battle" -> type.isBattle();
            case "land" -> type.isLand();
            default -> true;
        };
    }

    private static Comparator<Row> order(final String sort) {
        final Comparator<Row> byName = Comparator.comparing(r -> r.card().getName());
        return switch (sort == null ? "name" : sort) {
            case "mv" -> Comparator.comparingInt(Row::mv).thenComparing(byName);
            case "colour" -> Comparator.comparing(Row::colours).thenComparing(byName);
            case "type" -> Comparator.<Row>comparingInt(r -> HEADINGS.indexOf(r.heading())).thenComparing(byName);
            default -> byName;
        };
    }

    private static CatalogueRow toBrowser(final Row row, final String problem, final int inDeck) {
        final CardRules rules = row.card().getRules();
        return new CatalogueRow(row.card().getName(), Foil.key(row.card()), row.cost(), row.mv(),
                row.colours(), rules.getType().toString(), pt(rules), row.heading(), inDeck, problem, ownLimit(row.card()));
    }

    /** The copy limit a card sets for itself, as DeckFormat reads it. A basic land is left out, because the page knows those have none. */
    private static Integer ownLimit(final PaperCard card) {
        if (card.getRules().getType().isBasicLand()) {
            return null;
        }
        final Integer limit = DeckFormat.canHaveSpecificNumberInDeck(card);
        if (limit != null) {
            return limit;
        }
        return DeckFormat.canHaveAnyNumberOf(card) ? Integer.MAX_VALUE : null;
    }

    public static String pt(final CardRules rules) {
        if (rules.getType().isCreature()) {
            return rules.getPower() + "/" + rules.getToughness();
        }
        return rules.getType().isPlaneswalker() ? rules.getInitialLoyalty() : null;
    }

    /** A card's colours as WUBRG letters, or "C" for a colourless card. */
    public static String letters(final ColorSet colours) {
        final String out = wubrg(colours);
        return out.isEmpty() ? "C" : out;
    }

    /** Colours as WUBRG letters, or an empty string for colourless. */
    public static String wubrg(final ColorSet colours) {
        return (colours.hasWhite() ? "W" : "") + (colours.hasBlue() ? "U" : "") + (colours.hasBlack() ? "B" : "")
                + (colours.hasRed() ? "R" : "") + (colours.hasGreen() ? "G" : "");
    }

    /** Lower case, with accents stripped and only letters, digits and spaces kept. Desktop's ListChooser compares names the same way. */
    static String normalize(final String s) {
        return NOT_NAME_CHARACTER.matcher(StringUtils.stripAccents(s.toLowerCase())).replaceAll("");
    }

    /** A card with several types goes under the first match in the order below, so an artifact land is a land. */
    static String heading(final PaperCard card) {
        final CardType type = card.getRules().getType();
        if (type.isLand()) {
            return "Lands";
        }
        if (type.isCreature()) {
            return "Creatures";
        }
        if (type.isPlaneswalker()) {
            return "Planeswalkers";
        }
        if (type.isInstant()) {
            return "Instants";
        }
        if (type.isSorcery()) {
            return "Sorceries";
        }
        if (type.isBattle()) {
            return "Battles";
        }
        return type.isEnchantment() ? "Enchantments" : "Artifacts";
    }

    public static List<EditorGroup> groups(final CardPool pool, final Legality.Result legality) {
        final Map<String, List<EditorCard>> byHeading = new LinkedHashMap<>();
        HEADINGS.forEach(h -> byHeading.put(h, new ArrayList<>()));
        final Map<String, String> headings = new LinkedHashMap<>();
        for (final Map.Entry<PaperCard, Integer> e : pool) {
            headings.putIfAbsent(e.getKey().getName(), heading(e.getKey()));
        }
        for (final EditorCard card : cards(pool, legality)) {
            byHeading.get(headings.get(card.name())).add(card);
        }
        final List<EditorGroup> out = new ArrayList<>();
        byHeading.forEach((heading, cards) -> {
            if (!cards.isEmpty()) {
                out.add(new EditorGroup(heading, cards));
            }
        });
        return out;
    }

    /** A pool's cards, one per name, in mana value then name order. */
    public static List<EditorCard> cards(final CardPool pool, final Legality.Result legality) {
        final List<EditorCard> out = new ArrayList<>();
        if (pool == null) {
            return out;
        }
        final Map<String, List<Map.Entry<PaperCard, Integer>>> byName = new LinkedHashMap<>();
        for (final Map.Entry<PaperCard, Integer> e : pool) {
            byName.computeIfAbsent(e.getKey().getName(), n -> new ArrayList<>()).add(Map.entry(e.getKey(), e.getValue()));
        }
        byName.forEach((name, printings) -> {
            printings.sort(Map.Entry.<PaperCard, Integer>comparingByValue().reversed());
            final PaperCard shown = printings.get(0).getKey();
            final CardRules rules = shown.getRules();
            final List<EditorPrinting> split = printings.stream()
                    .map(p -> new EditorPrinting(p.getKey().getImageKey(false), p.getValue())).toList();
            out.add(new EditorCard(name, printings.stream().mapToInt(Map.Entry::getValue).sum(), Foil.key(shown),
                    PropertyJson.manaCost(rules.getManaCost()), rules.getManaCost().getCMC(), letters(rules.getColor()),
                    printings.size(), split, legality.flags().get(name), printings.stream().allMatch(p -> p.getKey().isFoil())));
        });
        out.sort(Comparator.comparingInt(EditorCard::mv).thenComparing(EditorCard::name));
        return out;
    }

    /** Card names that start with the text come first, shortest first, then names that contain it. */
    public static List<String> searchCardNames(final String query, final int limit) {
        final String text = normalize(query == null ? "" : query);
        if (text.isEmpty()) {
            return new ArrayList<>();
        }
        final List<String> startsWith = new ArrayList<>();
        final List<String> contains = new ArrayList<>();
        for (final PaperCard card : StaticData.instance().getCommonCards().getUniqueCards()) {
            final String name = normalize(card.getName());
            if (name.startsWith(text)) {
                startsWith.add(card.getName());
            } else if (name.contains(text)) {
                contains.add(card.getName());
            }
        }
        startsWith.sort(Comparator.comparingInt(String::length));
        startsWith.addAll(contains);
        return new ArrayList<>(startsWith.subList(0, Math.min(limit, startsWith.size())));
    }

    /** Every printing of one card, newest first as in desktop's printing dialog. A specific printing can then be picked for a deck or a sleeve. */
    public static List<Printing> printings(final String name, final GameFormat pool) {
        final CardEdition.Collection editions = StaticData.instance().getEditions();
        final List<PaperCard> cards = new ArrayList<>(StaticData.instance().getCommonCards().getAllCardsNoAlt(name));
        cards.sort(Comparator.comparing((PaperCard card) -> editions.get(card.getEdition()) == null ? null : editions.get(card.getEdition()).getDate(),
                Comparator.nullsLast(Comparator.reverseOrder())).thenComparing(PaperCard::getCollectorNumber, Comparator.nullsLast(Comparator.naturalOrder())));
        final List<Printing> out = new ArrayList<>();
        for (final PaperCard card : cards) {
            final CardEdition edition = editions.get(card.getEdition());
            final Calendar date = Calendar.getInstance();
            if (edition != null) {
                date.setTime(edition.getDate());
            }
            out.add(new Printing(card.getName(), card.getEdition(), card.getImageKey(false),
                    edition == null ? card.getEdition() : edition.getName(), edition == null ? 0 : date.get(Calendar.YEAR),
                    edition == null ? null : edition.getSectionForCollectorNumber(card.getCollectorNumber()),
                    pool != null && !pool.getFilterPrinted().test(card) ? Localizer.getInstance().getMessage("lblWebDeckCatalogNotIn", pool.getName()) : null));
        }
        return out;
    }

    // The holder class builds the catalogue once, on first use, on whichever thread asks first
    private static final class Holder {
        static final CardCatalog INSTANCE = build();
    }

    private static final class Sets {
        static final ToBrowser.CardSets ALL = build();

        private static ToBrowser.CardSets build() {
            final List<CardEdition> editions = new ArrayList<>();
            for (final CardEdition e : StaticData.instance().getEditions()) {
                if (!e.getAllCardsInSet().isEmpty()) {
                    editions.add(e);
                }
            }
            editions.sort(Comparator.reverseOrder());
            return new ToBrowser.CardSets(editions.stream().map(e -> new ToBrowser.LimitedEdition(e.getCode(), e.getName())).toList());
        }
    }
}
