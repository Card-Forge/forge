package forge.deck.generate;

import forge.Singletons;
import forge.card.CardDb;
import forge.deck.generation.DeckGenerator2Color;
import forge.item.PaperCard;
import forge.util.ItemPool;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, enabled = false)
public class Generate2ColorDeckTest {

    /**
     * Generate2 color deck test1.
     */
    @Test(enabled = false)
    public void generate2ColorDeckTest1() {
        CardDb cardDb = Singletons.getMagicDb().getCommonCards();
        final DeckGenerator2Color gen = new DeckGenerator2Color(cardDb, "white", "blue");
        final ItemPool<PaperCard> cardList = gen.getDeck(60, false);
        Assert.assertNotNull(cardList);
    }
}
