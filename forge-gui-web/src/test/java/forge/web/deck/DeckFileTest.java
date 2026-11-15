package forge.web.deck;

import forge.deck.DeckSection;
import forge.game.GameType;
import forge.item.PaperCard;
import forge.web.WebTestSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

public class DeckFileTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    // Fails if a card unknown to Forge stops a deck file from being read, is dropped without being counted, is imported as a stand-in, or is dropped by the importer without a problem being reported
    @Test
    public void unknownCardsAreCountedAndMarked() {
        final String file = "[metadata]\nName=Mini\n[Main]\n4 Lightning Bolt|M10\n3 No Such Card Anywhere|M10\n";
        final DeckWorkspace.FileDeck saved = DeckWorkspace.readFile(file);
        Assert.assertEquals(saved.deck().getMain().countAll(), 4);
        Assert.assertEquals(saved.leftOut(), 3);

        final DeckImport.Read read = DeckImport.read(file, LegalityScope.of(GameType.Constructed, null));
        Assert.assertEquals(read.deck().getMain().countAll(), 4);
        Assert.assertEquals(read.notImported(), 1);
        Assert.assertEquals(read.lines().get(4).kind().name(), "problem");
        Assert.assertTrue(read.problems().stream().anyMatch(p -> p.line() == 4));
    }

    // Fails if a pasted list is treated as a deck file. It would then be saved without the importer reading it
    @Test
    public void plainListIsNotADeckFile() {
        Assert.assertNull(DeckWorkspace.readFile("4 Lightning Bolt\n20 Mountain\n"));
    }

    // Fails if the importer loses a card, its printing or its foil when it is given a deck file
    @Test
    public void importerReadsADeckFile() {
        final DeckImport.Read read = DeckImport.read("[metadata]\nName=Mini\n[Main]\n4 Lightning Bolt|M10\n3 Shock|M10|1\n"
                + "2 Forest|M10|[246]\n1 Counterspell+|7ED\n[Sideboard]\n2 Duress|M10|[96]\n", LegalityScope.of(GameType.Constructed, null));
        Assert.assertEquals(read.name(), "Mini");
        Assert.assertEquals(read.deck().getMain().countAll(), 10);
        Assert.assertEquals(read.deck().get(DeckSection.Sideboard).countAll(), 2);
        Assert.assertEquals(read.notImported(), 0);
        Assert.assertEquals(read.lines().stream().map(l -> l.kind().name()).toList().subList(0, 9),
                List.of("ignored", "heading", "heading", "read", "read", "read", "read", "heading", "read"));
        final PaperCard forest = read.deck().getMain().toFlatList().stream().filter(c -> c.getName().equals("Forest")).findFirst().orElseThrow();
        Assert.assertEquals(forest.getCollectorNumber(), "246");
        Assert.assertTrue(read.deck().getMain().toFlatList().stream().anyMatch(c -> c.getName().equals("Counterspell") && c.isFoil()));
    }
}
