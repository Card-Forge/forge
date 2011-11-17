package forge;

import org.testng.annotations.Test;

import forge.deck.generate.GenerateConstructedDeck;

/**
 * The Class GameActionTest.
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class GameActionTest {

    /**
     * <p>
     * main.
     * </p>
     * 
     * @throws Exception
     *             the exception
     */
    @Test(groups = { "UnitTest", "fast" }, timeOut = 5000, enabled = false)
    public void gameActionTest1() throws Exception {
        System.out.println("GameActionTest");
        final GameAction gameAction = new GameAction();
        final GenerateConstructedDeck gen = new GenerateConstructedDeck();

        for (int i = 0; i < 2000; i++) {
            final CardList list = gen.generateDeck();

            final Card[] card = gameAction.smoothComputerManaCurve(list.toArray());

            final CardList check = new CardList();
            for (int a = 0; a < 30; a++) {
                check.add(card[a]);
            }

            if (check.getType("Land").size() != 7) {
                System.out.println("error - " + check);
                break;
            }
        } // for
    }
}
