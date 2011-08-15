package forge.deck.generate;

import forge.CardList;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 1000, enabled = false)
public class Generate3ColorDeckTest {


    /**
     *
     */
    @Test(timeOut = 1000, enabled = false)
    public  void Generate3ColorDeckTest1() {
        Generate3ColorDeck gen = new Generate3ColorDeck("white", "blue", "black");
        CardList cardList = gen.get3ColorDeck(60);
        Assert.assertNotNull(cardList);
    }
}
