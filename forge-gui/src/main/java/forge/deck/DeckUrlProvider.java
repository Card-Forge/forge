package forge.deck;

import java.io.IOException;

interface DeckUrlProvider {
    RemoteDeck load(String normalizedUrl, Iterable<Deck> savedDecks) throws IOException;

    record RemoteDeck(String name, DeckFormat format, String sourceUrl, String importText, String providerName) {
    }
}
