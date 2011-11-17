package forge.deck.generate;

import org.testng.annotations.Test;

import forge.CardList;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, timeOut = 1000)
public class GenerateConstructedDeckTest {

    /**
     * Generate constructed deck test1.
     */
    @Test(enabled = false, timeOut = 1000)
    public void generateConstructedDeckTest1() {
        final GenerateConstructedDeck g = new GenerateConstructedDeck();

        for (int i = 0; i < 50; i++) {
            final CardList c = g.generateDeck();
            System.out.println(c.getType("Creature").size() + " - " + c.size());
        }
        System.exit(1);

    } // main
}
