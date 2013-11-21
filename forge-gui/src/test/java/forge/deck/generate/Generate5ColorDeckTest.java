package forge.deck.generate;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.Singletons;
import forge.card.CardDb;
import forge.deck.generation.DeckGenerator5Color;
import forge.item.PaperCard;
import forge.util.ItemPoolView;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class Generate5ColorDeckTest {

    /**
     * Generate5 color deck test1.
     */
    @Test(timeOut = 1000, enabled = false)
    public void generate5ColorDeckTest1() {
        CardDb cardDb = Singletons.getMagicDb().getCommonCards();
        final DeckGenerator5Color gen = new DeckGenerator5Color(cardDb);
        final ItemPoolView<PaperCard> cardList = gen.getDeck(60, false);
        Assert.assertNotNull(cardList);
    }
}
