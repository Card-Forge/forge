package forge.deck.generate;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.CardList;

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
        final Generate5ColorDeck gen = new Generate5ColorDeck();
        final CardList cardList = gen.get5ColorDeck(60, null);
        Assert.assertNotNull(cardList);
    }
}
