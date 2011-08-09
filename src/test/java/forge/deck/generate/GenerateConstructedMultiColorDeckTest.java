package forge.deck.generate;

import forge.CardList;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 1000, enabled = false)
public class GenerateConstructedMultiColorDeckTest {


    /**
     *
     */
    @Test(enabled = false, timeOut = 1000)
    public void GenerateConstructedMultiColorDeckTest1() {
        GenerateConstructedMultiColorDeck g = new GenerateConstructedMultiColorDeck();

        for (int i = 0; i < 10; i++) {
            System.out.println("***GENERATING DECK***");
            CardList c = g.generate3ColorDeck();
            System.out.println(c.getType("Creature").size() + " - " + c.size());
            for (int j = 0; j < c.size(); j++) {
                System.out.println(c.get(j).getName());
            }
            System.out.println("***DECK GENERATED***");

        }
        System.exit(1);
    }//main
}

