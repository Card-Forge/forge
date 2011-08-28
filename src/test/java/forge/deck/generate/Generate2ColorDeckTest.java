package forge.deck.generate;

import forge.CardList;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, enabled = false)
public class Generate2ColorDeckTest {


    /**
     *
     */
    @Test(enabled = false)
    public  void Generate2ColorDeckTest1() {
        Generate2ColorDeck gen = new Generate2ColorDeck("white", "blue");
        CardList cardList = gen.get2ColorDeck(60, null);
        Assert.assertNotNull(cardList);
    }
}
