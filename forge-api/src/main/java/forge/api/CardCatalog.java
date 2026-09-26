package forge.api;

import forge.card.CardDb;
import forge.item.PaperCard;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** A stable, printing-aware catalog. Construct after Forge's card database has loaded. */
public final class CardCatalog {
    public record CardInfo(String id, String name, String edition, int artIndex, boolean foil,
                           String manaCost, int manaValue, String type, String oracleText,
                           int colors, int colorIdentity, String rarity, String collectorNumber) { }
    /** colors is an allowed-color mask (W=1 U=2 B=4 R=8 G=16); null allows any. */
    public record Query(String text, Integer colors, Integer maxManaValue, int offset, int limit) {
        public Query {
            text = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
            if (colors != null && (colors < 0 || colors > 31)) {
                throw new IllegalArgumentException("Invalid color mask");
            }
            if (maxManaValue != null && maxManaValue < 0) {
                throw new IllegalArgumentException("Mana value must be nonnegative");
            }
            if (offset < 0 || limit < 1 || limit > 200) {
                throw new IllegalArgumentException("Offset must be nonnegative; limit must be 1..200");
            }
        }
    }
    public record Page(int total, int offset, List<CardInfo> cards) {
        public Page { cards = List.copyOf(cards); }
    }

    private final Map<String, PaperCard> cards;
    private record IndexedCard(CardInfo info, String searchText) { }
    private final List<IndexedCard> index;

    public CardCatalog(CardDb database) {
        this(database.getAllCards());
    }

    public CardCatalog(Collection<PaperCard> source) {
        cards = source.stream().collect(Collectors.toUnmodifiableMap(CardCatalog::id,
                Function.identity(), (first, duplicate) -> first));
        index = cards.values().stream().map(CardCatalog::describe)
                .sorted(Comparator.comparing(CardInfo::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(CardInfo::id))
                .map(card -> new IndexedCard(card, (card.name() + "\n" + card.type() + "\n"
                        + card.oracleText()).toLowerCase(Locale.ROOT))).toList();
    }

    public Page search(Query query) {
        return browse(query, "", "name", false);
    }

    public Page browse(Query query, String type, String sort, boolean unique) {
        Objects.requireNonNull(query);
        String typeFilter = Objects.requireNonNullElse(type, "").toLowerCase(Locale.ROOT);
        var seen = new HashSet<String>();
        var ordering = Comparator.comparing(CardInfo::name, String.CASE_INSENSITIVE_ORDER).thenComparing(CardInfo::id);
        if ("mana".equals(sort)) { ordering = Comparator.comparingInt(CardInfo::manaValue).thenComparing(ordering); }
        if (!query.text().isEmpty()) {
            ordering = Comparator.comparingInt((CardInfo card) -> card.name().toLowerCase(Locale.ROOT)
                    .startsWith(query.text()) ? 0 : 1).thenComparing(ordering);
        }
        List<CardInfo> matches = index.stream().filter(card -> card.searchText().contains(query.text()))
                .map(IndexedCard::info)
                .filter(card -> card.type().toLowerCase(Locale.ROOT).contains(typeFilter))
                .filter(card -> query.colors() == null || (card.colors() & ~query.colors()) == 0)
                .filter(card -> query.maxManaValue() == null || card.manaValue() <= query.maxManaValue())
                .filter(card -> !unique || seen.add(card.name()))
                .sorted(ordering)
                .toList();
        return new Page(matches.size(), query.offset(), matches.stream()
                .skip(query.offset()).limit(query.limit()).toList());
    }

    public int size() { return cards.size(); }

    public static CardCatalog fromDatabase(CardDb database) {
        // Import accepts foil printings too; the browser groups them by name by default.
        var all = new ArrayList<>(database.getAllCards());
        all.addAll(database.getAllCards().stream().map(PaperCard::getFoiled).toList());
        return new CardCatalog(all);
    }

    PaperCard resolve(String id) {
        PaperCard card = cards.get(Objects.requireNonNull(id));
        if (card == null) {
            throw new IllegalArgumentException("Unknown printing: " + id);
        }
        return card;
    }

    public static String id(PaperCard card) {
        // Length prefixes avoid delimiter collisions in names and edition identifiers.
        return card.getName().length() + ":" + card.getName() + card.getEdition().length() + ":"
                + card.getEdition() + ":" + card.getArtIndex() + ":" + card.isFoil()
                + ":" + card.getCollectorNumber() + ":" + card.getFunctionalVariant();
    }

    public static CardInfo describe(PaperCard card) {
        var rules = card.getRules();
        return new CardInfo(id(card), card.getName(), card.getEdition(), card.getArtIndex(), card.isFoil(),
                rules.getManaCost().toString(), rules.getManaCost().getCMC(), rules.getType().toString(),
                Objects.requireNonNullElse(rules.getOracleText(), "").replace("\\n", "\n"),
                rules.getColor().getColor(), rules.getColorIdentity().getColor(),
                card.getRarity().name(), card.getCollectorNumber());
    }
}
