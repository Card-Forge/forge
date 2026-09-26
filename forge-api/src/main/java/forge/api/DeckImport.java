package forge.api;

import forge.StaticData;
import forge.card.CardDb;
import forge.deck.Deck;
import forge.deck.DeckRecognizer;
import forge.deck.DeckSection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Paste/import preview using Forge's existing multi-format deck recognizer. */
public final class DeckImport {
    private static final Pattern NATIVE_LINE = Pattern.compile("^(\\d+)\\s+(.+\\|.+)$");
    private DeckImport() { }

    public record Problem(int line, String kind, String text) { }
    public record Preview(List<DeckEditor.Entry> entries, List<Problem> problems) {
        public Preview {
            entries = List.copyOf(entries);
            problems = List.copyOf(problems);
        }

        /** Unknown or unsupported lines must be resolved before creating a deck. */
        public DeckEditor open(String name, CardCatalog catalog) {
            if (!problems.isEmpty()) {
                throw new IllegalStateException("Resolve import problems before opening the deck");
            }
            Deck deck = new Deck(name);
            for (DeckEditor.Entry entry : entries) {
                if (entry.quantity() < 1 || entry.quantity() > 1000) {
                    throw new IllegalArgumentException("Quantity must be 1..1000");
                }
                var section = DeckSection.valueOf(entry.section());
                var card = catalog.resolve(entry.card().id());
                if (!section.validate(card)) { throw new IllegalArgumentException("Card does not belong in " + section); }
                var pool = deck.getOrCreate(section);
                int total = Math.addExact(pool.count(card), entry.quantity());
                if (total > 1000) { throw new IllegalArgumentException("Quantity exceeds 1000"); }
                pool.add(card, entry.quantity());
            }
            return new DeckEditor(catalog, deck);
        }
    }

    public static Preview preview(String text) {
        Objects.requireNonNull(text);
        if (text.length() > 1_000_000) { throw new IllegalArgumentException("Deck text is too large"); }
        var recognizer = new DeckRecognizer();
        List<DeckEditor.Entry> entries = new ArrayList<>();
        List<Problem> problems = new ArrayList<>();
        DeckSection section = DeckSection.Main;
        String[] lines = text.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            var nativeLine = NATIVE_LINE.matcher(lines[index].strip());
            if (nativeLine.matches()) {
                try {
                    int quantity = Integer.parseInt(nativeLine.group(1));
                    if (quantity < 1 || quantity > 1000) { throw new IllegalArgumentException("INVALID_QUANTITY"); }
                    var request = CardDb.CardRequest.fromString(nativeLine.group(2));
                    if (request.flags != null && !request.flags.isEmpty()) { throw new IllegalArgumentException("UNSUPPORTED_FLAGS"); }
                    var data = StaticData.instance();
                    var edition = data.getCardEdition(request.edition);
                    forge.item.PaperCard card = null;
                    for (var database : data.getAvailableDatabases().values()) {
                        card = database.getCardFromSet(request.cardName, edition, request.artIndex, request.collectorNumber, request.isFoil);
                        if (card != null) { break; }
                    }
                    if (card == null) { throw new IllegalArgumentException("UNKNOWN_PRINTING"); }
                    if (!section.validate(card)) { throw new IllegalArgumentException("INVALID_SECTION"); }
                    entries.add(new DeckEditor.Entry(section.name(), CardCatalog.describe(card), quantity));
                } catch (RuntimeException error) {
                    problems.add(new Problem(index + 1, "INVALID_FORGE_ROW", lines[index]));
                }
                continue;
            }
            var token = recognizer.recognizeLine(lines[index], section);
            if (token == null) { continue; }
            switch (token.getType()) {
                case DECK_SECTION_NAME -> section = DeckSection.valueOf(token.getText());
                case LEGAL_CARD -> {
                    if (token.getQuantity() < 1 || token.getQuantity() > 1000) {
                        problems.add(new Problem(index + 1, "INVALID_QUANTITY", lines[index]));
                    } else {
                        entries.add(new DeckEditor.Entry(token.getTokenSection().name(),
                                CardCatalog.describe(token.getCard()), token.getQuantity()));
                    }
                }
                case COMMENT, DECK_NAME, CARD_TYPE, CARD_RARITY, CARD_CMC, MANA_COLOUR -> { }
                default -> problems.add(new Problem(index + 1, token.getType().name(), lines[index]));
            }
        }
        return new Preview(entries, problems);
    }
}
