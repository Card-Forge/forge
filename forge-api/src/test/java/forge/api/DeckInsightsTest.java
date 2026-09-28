package forge.api;

import forge.card.CardRarity;
import forge.card.CardRules;
import forge.item.PaperCard;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.*;

public class DeckInsightsTest {
    private static PaperCard card(String name, String mana, String type, String oracle) {
        return new PaperCard(new CardRules.Reader().readCard(List.of("Name:" + name, "ManaCost:" + mana,
                "Types:" + type, "Oracle:" + oracle)), "TST", CardRarity.Common);
    }
    private static DeckEditor.Entry entry(PaperCard card, String section, int quantity) {
        return new DeckEditor.Entry(section, CardCatalog.describe(card), quantity);
    }
    private static DeckEditor.Snapshot deck(DeckEditor.Entry... entries) {
        return new DeckEditor.Snapshot("Test", 7, List.of(entries), false, false);
    }

    @Test
    public void identityUsesCommandersTogetherAndIncludesActivatedManaSymbols() {
        var green = card("Green leader", "G", "Legendary Creature Elf", "Partner");
        var blue = card("Blue leader", "U", "Legendary Creature Human", "Partner");
        var black = card("Black spell", "B", "Sorcery", "Draw a card.");
        var artifact = card("Blue rock", "2", "Artifact", "{T}: Add {U}.");
        var colorless = card("Colorless leader", "4", "Legendary Creature Golem", "Vigilance");
        var catalog = new CardCatalog(List.of(green, blue, black, artifact, colorless));
        assertEquals(DeckInsights.identity(deck(entry(green, "Commander", 1), entry(blue, "Commander", 1), entry(black, "Main", 1)), "Commander"), Integer.valueOf(18));
        assertNull(DeckInsights.identity(deck(entry(green, "Main", 1)), "Commander"));
        assertEquals(DeckInsights.identity(deck(entry(colorless, "Commander", 1)), "Commander"), Integer.valueOf(0));
        assertTrue(catalog.browse(new CardCatalog.Query("Blue rock", null, null, 0, 24), "", "name", true, 16, "").cards().isEmpty());
        assertEquals(catalog.browse(new CardCatalog.Query("Blue rock", null, null, 0, 24), "", "name", true, 18, "").total(), 1);
        assertThrows(IllegalArgumentException.class, () -> catalog.browse(new CardCatalog.Query("", null, null, 0, 24), "", "name", true, -1, ""));
        assertThrows(IllegalArgumentException.class, () -> catalog.browse(new CardCatalog.Query("", null, null, 0, 24), "", "name", true, null, "bad"));
    }

    @Test
    public void roleReviewCountsCopiesInMainOnlyAndSuggestionsExcludeEveryOwnedPrinting() {
        var leader = card("Leader", "G", "Legendary Creature Elf", "Vigilance");
        var harmonize = card("Harmonize", "2 G G", "Sorcery", "Draw three cards.");
        var otherPrinting = new PaperCard(harmonize.getRules(), "OTHER", CardRarity.Rare);
        var rock = card("Mind Stone", "2", "Artifact", "{T}: Add {C}. {1}, {T}, Sacrifice Mind Stone: Draw a card.");
        var black = card("Sign in Blood", "B B", "Sorcery", "Target player draws two cards and loses 2 life.");
        var removal = card("Beast Within", "2 G", "Instant", "Destroy target permanent. Its controller creates a 3/3 green Beast creature token.");
        var catalog = new CardCatalog(List.of(leader, harmonize, otherPrinting, rock, black, removal));
        var snapshot = deck(entry(leader, "Commander", 1), entry(rock, "Main", 2), entry(otherPrinting, "Sideboard", 1));
        var report = DeckInsights.analyze(catalog, snapshot, "Commander", "deck-id");
        assertEquals(report.deckId(), "deck-id"); assertEquals(report.revision(), 7L);
        assertEquals(report.counts().get("Ramp"), Integer.valueOf(2));
        assertEquals(report.counts().get("Draw"), Integer.valueOf(2));
        assertEquals(report.counts().get("Interaction"), Integer.valueOf(0));
        assertEquals(report.suggestions().size(), 1);
        assertEquals(report.suggestions().get(0).card().name(), "Beast Within");
        assertTrue(report.suggestions().get(0).reason().contains("0 interaction"));
        assertTrue(DeckInsights.analyze(catalog, deck(), "Commander", "empty").suggestions().isEmpty());
        assertTrue(DeckInsights.analyze(catalog, snapshot, "Limited", "limited").suggestions().isEmpty());
        assertEquals(catalog.browse(new CardCatalog.Query("", null, null, 0, 24), "", "name", true, 16, "Interaction").cards().get(0).name(), "Beast Within");
    }
}
