package forge.deck.generate;

import forge.CardList;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 1000)
public class GenerateConstructedDeckTest {


    /**
     *
     */
    @Test(enabled = false, timeOut = 1000)
    public  void GenerateConstructedDeckTest1() {
        GenerateConstructedDeck g = new GenerateConstructedDeck();

        for (int i = 0; i < 50; i++) {
            CardList c = g.generateDeck();
            System.out.println(c.getType("Creature").size() + " - " + c.size());
        }
        System.exit(1);

    }//main
}
