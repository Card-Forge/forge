package forge.web.deck;

import forge.deck.Deck;
import forge.game.GameType;
import forge.web.WebTestSupport;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class DeckCatalogTest {
    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    // Fails if a rebuilt list hands out the deck from before a save, so a reopened deck loses its edits
    @Test
    public void aRebuiltListHoldsTheLatestDeviceDeck() {
        final DeckCatalog catalog = new DeckCatalog();
        final Map<String, DeckCatalog.OnDevice> device = new HashMap<>();
        final Deck before = new Deck("Mine");
        device.put("id", new DeckCatalog.OnDevice(before, GameType.Constructed));
        catalog.refresh(GameType.Constructed, null, true, device, null);
        Assert.assertSame(catalog.deck(DeckCatalog.DEVICE + ":id"), before);

        final Deck saved = new Deck("Mine");
        device.put("id", new DeckCatalog.OnDevice(saved, GameType.Constructed));
        catalog.refresh(GameType.Constructed, null, true, device, null);
        Assert.assertSame(catalog.deck(DeckCatalog.DEVICE + ":id"), saved);
    }
}
