package forge.deck.generate;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.card.CardDb;
import forge.deck.DeckFormat;
import forge.deck.generation.DeckGenerator2Color;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.ItemPool;

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
        CardDb cardDb = FModel.getMagicDb().getCommonCards();
        final DeckGenerator2Color gen = new DeckGenerator2Color(cardDb, DeckFormat.Constructed, "white", "blue");
        final ItemPool<PaperCard> cardList = gen.getDeck(60, false);
        Assert.assertNotNull(cardList);
    }
}
