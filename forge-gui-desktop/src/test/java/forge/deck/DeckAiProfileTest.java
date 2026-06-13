package forge.deck;

import forge.deck.io.DeckSerializer;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static org.testng.Assert.assertEquals;

public class DeckAiProfileTest {
    @Test
    public void aiProfileIsReadFromAiHintsMetadata() throws IOException {
        final File deckFile = File.createTempFile("forge-ai-profile", ".dck");
        try {
            final Deck deck = new Deck("Profile Test");
            deck.setAiHint("DeckArchetype", "Combo");
            deck.setAiHint("AiProfile", "Berserker");

            DeckSerializer.writeDeck(deck, deckFile);

            final Deck reloaded = DeckSerializer.fromFile(deckFile);
            assertEquals(reloaded.getAiHint("AiProfile"), "Berserker");
            assertEquals(reloaded.getAiHint("DeckArchetype"), "Combo");
        } finally {
            deckFile.delete();
        }
    }
}
