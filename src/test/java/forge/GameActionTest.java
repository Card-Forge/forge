package forge;

import forge.deck.generate.GenerateConstructedDeck;
import org.testng.annotations.Test;

/**
 *
 */
@Test(groups = {"UnitTest"}, timeOut = 1000)
public class GameActionTest {

    /**
     * <p>main.</p>
     */
    @Test(timeOut = 1000)
    public void GameActionTest1() throws Exception {
        System.out.println("GameActionTest");
        GameAction gameAction = new GameAction();
        GenerateConstructedDeck gen = new GenerateConstructedDeck();

        for (int i = 0; i < 2000; i++) {
            CardList list = gen.generateDeck();

            Card[] card = gameAction.smoothComputerManaCurve(list.toArray());

            CardList check = new CardList();
            for (int a = 0; a < 30; a++)
                check.add(card[a]);

            if (check.getType("Land").size() != 7) {
                System.out.println("error - " + check);
                break;
            }
        }//for
    }
}
