package forge.deck;

import java.io.IOException;

interface DeckUrlProvider {
    RemoteDeck load(String normalizedUrl, Iterable<Deck> savedDecks) throws IOException;

    static void appendSection(final StringBuilder out, final DeckSection section, final StringBuilder cards) {
        if (!cards.isEmpty()) {
            out.append(section.name()).append('\n').append(cards);
        }
    }

    record RemoteDeck(String name, DeckFormat format, String sourceUrl, String importText, String providerName) {
    }
}
