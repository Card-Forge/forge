package forge.deck.generate;

import forge.CardList;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 1000, enabled = false)
public class Generate5ColorDeckTest {


    /**
     *
     */
    @Test(timeOut = 1000, enabled = false)
    public  void Generate5ColorDeckTest1() {
        Generate5ColorDeck gen = new Generate5ColorDeck();
        CardList cardList = gen.get5ColorDeck(60);
        Assert.assertNotNull(cardList);
    }
}
