package forge.web;

import com.google.gson.JsonObject;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.model.FModel;
import org.testng.Assert;

final class TestDecks {
    private TestDecks() {}

    static Deck of(final String name, final Object... cardNameCountPairs) {
        final Deck deck = new Deck(name);
        for (int i = 0; i < cardNameCountPairs.length; i += 2) {
            final PaperCard card = FModel.getMagicDb().getCommonCards().getCard((String) cardNameCountPairs[i]);
            if (card == null) {
                throw new IllegalStateException("No card named " + cardNameCountPairs[i]);
            }
            deck.getMain().add(card, (Integer) cardNameCountPairs[i + 1]);
        }
        return deck;
    }

    /** The key of the first deck in a deck list that is legal and built, not generated when the game starts. */
    static String firstLegal(final JsonObject decks) {
        Assert.assertNotNull(decks, "no deck list arrived");
        for (final var d : decks.getAsJsonArray("decks")) {
            final JsonObject deck = d.getAsJsonObject();
            if (!deck.has("problem") && !(deck.has("generated") && deck.get("generated").getAsBoolean())) {
                return deck.get("key").getAsString();
            }
        }
        throw new AssertionError("no legal deck to choose");
    }
}
