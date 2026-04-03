package forge.net;

import forge.card.CardDb;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.model.FModel;

/**
 * Utility for creating test decks.
 */
public class TestDeckLoader {

    /**
     * Create a minimal deck with basic lands only.
     * Used for fast CI testing where game completion matters more than strategic play.
     * Games with these decks end quickly as players can only play lands and eventually deck out.
     *
     * Note: Deck legality checking must be disabled (ENFORCE_DECK_LEGALITY=false) to use
     * decks smaller than 60 cards.
     *
     * @param landName Basic land name: "Plains", "Island", "Swamp", "Mountain", or "Forest"
     * @param count Number of copies (10 for fast tests, 60 for legal minimum)
     * @return Deck with only basic lands
     */
    public static Deck createMinimalDeck(String landName, int count) {
        CardDb cardDb = FModel.getMagicDb().getCommonCards();
        PaperCard land = cardDb.getCard(landName);
        if (land == null) {
            throw new IllegalStateException("Basic land not found: " + landName);
        }

        Deck deck = new Deck(landName + " x" + count);
        for (int i = 0; i < count; i++) {
            deck.getMain().add(land);
        }
        return deck;
    }
}
