package forge.deck;

import forge.util.Localizer;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ArchidektDeckUrlProvider implements DeckUrlProvider {
    private static final Pattern DECK_URL = Pattern.compile("(?i)(?:^|/)decks/(\\d+)(?:[/?#]|$)");
    private static final String API_BASE = "https://archidekt.com/api/decks/";
    private static final String PROVIDER_NAME = "Archidekt";
    private static final Localizer localizer = Localizer.getInstance();

    @Override
    public RemoteDeck load(final String normalizedUrl, final Iterable<Deck> savedDecks) throws IOException {
        final String deckId = getDeckId(normalizedUrl);
        final Map<?, ?> root = DeckUrlLoader.readJsonObject(API_BASE + deckId + "/", PROVIDER_NAME);

        final DeckUrlImportTextBuilder builder = new DeckUrlImportTextBuilder();
        addCards(builder, root);

        return new RemoteDeck(
                DeckUrlLoader.getDeckName(root, deckId, normalizedUrl,
                        localizer.getMessage("lblDeckUrlDefaultDeckName", PROVIDER_NAME), savedDecks),
                getDeckFormat(root.get("deckFormat")),
                normalizedUrl,
                builder.toString(),
                PROVIDER_NAME);
    }

    static String getDeckId(final String deckUrl) throws IOException {
        final Matcher matcher = DECK_URL.matcher(deckUrl);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IOException(localizer.getMessage("lblCouldNotFindDeckUrlId", PROVIDER_NAME));
    }

    private static void addCards(final DeckUrlImportTextBuilder builder, final Map<?, ?> root) throws IOException {
        if (!(root.get("cards") instanceof List<?> cards)) {
            throw new IOException(localizer.getMessage("lblDeckUrlUnexpectedResponse", PROVIDER_NAME));
        }

        final Set<String> excludedCategories = getExcludedCategories(root);
        for (final Object cardValue : cards) {
            if (!(cardValue instanceof Map<?, ?> cardEntry) || cardEntry.get("deletedAt") != null) {
                continue;
            }

            final int quantity = DeckUrlLoader.getInt(cardEntry.get("quantity"), 1);
            if (quantity <= 0 || isExcludedCard(cardEntry, excludedCategories)) {
                continue;
            }

            final String cardName = getCardName(cardEntry);
            if (cardName == null) {
                continue;
            }

            builder.add(getSection(cardEntry), quantity, cardName,
                    DeckUrlLoader.getNestedString(cardEntry, "card", "edition", "editioncode"),
                    DeckUrlLoader.getNestedString(cardEntry, "card", "collectorNumber"));
        }
    }

    private static Set<String> getExcludedCategories(final Map<?, ?> root) {
        final Set<String> excludedCategories = new HashSet<>();
        if (!(root.get("categories") instanceof List<?> categories)) {
            return excludedCategories;
        }
        for (final Object categoryValue : categories) {
            if (categoryValue instanceof Map<?, ?> category && Boolean.FALSE.equals(category.get("includedInDeck"))) {
                final String name = DeckUrlLoader.getString(category.get("name"), null);
                if (name != null && !"Sideboard".equalsIgnoreCase(name)) {
                    excludedCategories.add(name);
                }
            }
        }
        return excludedCategories;
    }

    private static boolean isExcludedCard(final Map<?, ?> cardEntry, final Set<String> excludedCategories) {
        if (!(cardEntry.get("categories") instanceof List<?> categories)) {
            return false;
        }
        for (final Object categoryValue : categories) {
            if (categoryValue instanceof String category && excludedCategories.contains(category)) {
                return true;
            }
        }
        return false;
    }

    private static DeckSection getSection(final Map<?, ?> cardEntry) {
        if (Boolean.TRUE.equals(cardEntry.get("companion"))) {
            return DeckSection.Sideboard;
        }
        if (cardEntry.get("categories") instanceof List<?> categories) {
            for (final Object categoryValue : categories) {
                if (!(categoryValue instanceof String category)) {
                    continue;
                }
                if ("Commander".equalsIgnoreCase(category)) {
                    return DeckSection.Commander;
                }
                if ("Sideboard".equalsIgnoreCase(category)) {
                    return DeckSection.Sideboard;
                }
            }
        }
        return DeckSection.Main;
    }

    private static String getCardName(final Map<?, ?> cardEntry) {
        String cardName = DeckUrlLoader.getNestedString(cardEntry, "card", "displayName");
        if (cardName == null) {
            cardName = DeckUrlLoader.getNestedString(cardEntry, "card", "oracleCard", "name");
        }
        return cardName;
    }

    private static DeckFormat getDeckFormat(final Object formatValue) {
        if (!(formatValue instanceof Number number)) {
            return DeckFormat.Constructed;
        }
        return switch (number.intValue()) {
            case 3, 11, 12 -> DeckFormat.Commander;
            case 6 -> DeckFormat.Pauper;
            case 13 -> DeckFormat.Brawl;
            default -> DeckFormat.Constructed;
        };
    }
}
