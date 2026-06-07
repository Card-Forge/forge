package forge.deck;

import forge.util.Localizer;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class MoxfieldDeckUrlProvider implements DeckUrlProvider {
    private static final Pattern DECK_URL = Pattern.compile("(?i)(?:^|/)decks/([^/?#]+)");
    private static final String API_BASE = "https://api.moxfield.com/v2/decks/all/";
    private static final Localizer localizer = Localizer.getInstance();

    @Override
    public RemoteDeck load(final String normalizedUrl, final Iterable<Deck> savedDecks) throws IOException {
        final String deckId = getDeckId(normalizedUrl);
        final Map<?, ?> root = DeckUrlLoader.readJsonObject(API_BASE + deckId, "Moxfield", "lblMoxfieldUnexpectedResponse");

        final DeckUrlImportTextBuilder builder = new DeckUrlImportTextBuilder();
        addSection(builder, root.get("commanders"), DeckSection.Commander);
        addSection(builder, root.get("mainboard"), DeckSection.Main);
        addSection(builder, root.get("sideboard"), DeckSection.Sideboard);
        addSection(builder, root.get("companions"), DeckSection.Sideboard);

        return new RemoteDeck(
                DeckUrlLoader.getDeckName(root, deckId, normalizedUrl, localizer.getMessage("lblMoxfieldDeck"), savedDecks),
                DeckUrlLoader.getDeckFormat(root.get("format")),
                normalizedUrl,
                builder.toString(),
                "lblMoxfieldCardNotFound",
                "lblNoPlayableCardsInMoxfieldDeck");
    }

    static String getDeckId(final String deckUrl) throws IOException {
        final Matcher matcher = DECK_URL.matcher(deckUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        final int lastSlash = deckUrl.lastIndexOf('/');
        final String id = lastSlash >= 0 ? deckUrl.substring(lastSlash + 1) : deckUrl;
        final int query = id.indexOf('?');
        final String cleanId = query >= 0 ? id.substring(0, query) : id;
        if (cleanId.isBlank()) {
            throw new IOException(localizer.getMessage("lblCouldNotFindMoxfieldDeckId"));
        }
        return cleanId;
    }

    private static void addSection(final DeckUrlImportTextBuilder builder, final Object sectionValue, final DeckSection section) {
        if (!(sectionValue instanceof Map<?, ?> cards)) {
            return;
        }
        for (final Map.Entry<?, ?> entry : cards.entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> cardEntry)) {
                continue;
            }

            final int quantity = DeckUrlLoader.getInt(cardEntry.get("quantity"), 1);
            String cardName = DeckUrlLoader.getNestedString(cardEntry, "card", "name");
            if (cardName == null) {
                cardName = String.valueOf(entry.getKey());
            }
            if (!cardName.isBlank() && quantity > 0) {
                builder.add(section, quantity, cardName,
                        DeckUrlLoader.getNestedString(cardEntry, "card", "set"),
                        DeckUrlLoader.getNestedString(cardEntry, "card", "cn"));
            }
        }
    }
}
