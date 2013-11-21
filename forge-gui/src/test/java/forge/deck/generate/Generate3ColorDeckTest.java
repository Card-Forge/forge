package forge.deck.generate;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.Singletons;
import forge.card.CardDb;
import forge.deck.generation.DeckGenerator3Color;
import forge.item.PaperCard;
import forge.util.ItemPoolView;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class Generate3ColorDeckTest {

    /**
     * Generate3 color deck test1.
     */
    @Test(timeOut = 1000, enabled = false)
    public void generate3ColorDeckTest1() {
        CardDb cardDb = Singletons.getMagicDb().getCommonCards();
        final DeckGenerator3Color gen = new DeckGenerator3Color(cardDb, "white", "blue", "black");
        final ItemPoolView<PaperCard> cardList = gen.getDeck(60, false);
        Assert.assertNotNull(cardList);
    }
}
