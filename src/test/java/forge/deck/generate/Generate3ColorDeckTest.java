package forge.deck.generate;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.CardList;

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
        final Generate3ColorDeck gen = new Generate3ColorDeck("white", "blue", "black");
        final CardList cardList = gen.get3ColorDeck(60, null);
        Assert.assertNotNull(cardList);
    }
}
