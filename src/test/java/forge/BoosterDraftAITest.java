package forge;

import forge.deck.Deck;
import org.testng.annotations.Test;

@Test(groups = {"UnitTest"}, timeOut = 5000, enabled = false)
public class BoosterDraftAITest {

    /**
     * <p>runTestPrint.</p>
     */
    @Test(timeOut = 5000)
    public void runTestPrint() {
        BoosterDraftAI ai = new BoosterDraftAI();
        runTest(ai);

        Deck[] deck = ai.getDecks();

        for (int outer = 0; outer < 7; outer++) {
            System.out.print(deck[outer].countMain() + " - ");

            for (int i = 0; i < 16; i++)
                System.out.print(deck[outer].getMain(i) + ", ");

            System.out.println("");

            for (int i = 16; i < 22; i++)
                System.out.print(deck[outer].getMain(i) + ", ");

            System.out.println("\n");
        }//for outer
    }//runTestPrint()

    @Test(timeOut = 5000)
    public void runTest(BoosterDraftAI ai) {
        ReadDraftBoosterPack booster = new ReadDraftBoosterPack();
        for (int outer = 0; outer < 1; outer++) {
            CardList allBooster = new CardList();
            for (int i = 0; i < 21; i++)
                allBooster.addAll(booster.getBoosterPack());

            int stop = allBooster.size();
            for (int i = 0; i < stop; i++) {
                ai.choose(allBooster, i);
            }
            //ai.checkDeckList(ai.deck);
        }
    }//runTest()
}
