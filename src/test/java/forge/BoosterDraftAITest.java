package forge;

import java.util.List;

import forge.deck.Deck;
import forge.game.limited.BoosterDraftAI;
import forge.item.CardPrinted;
import forge.item.ItemPool;

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
            Deck thisDeck = deck[outer];
            System.out.print(thisDeck.countMain() + " - ");

            List<CardPrinted> cards = thisDeck.getMain().toFlatList();
            for (int i = 0; i < 16; i++)
                System.out.print(cards.get(i) + ", ");

            System.out.println("");

            for (int i = 16; i < 22; i++)
                System.out.print(cards.get(i) + ", ");

            System.out.println("\n");
        }//for outer
    }//runTestPrint()

    @Test(timeOut = 5000)
    public void runTest(BoosterDraftAI ai) {
        ReadDraftBoosterPack booster = new ReadDraftBoosterPack();
        for (int outer = 0; outer < 1; outer++) {
            ItemPool<CardPrinted> allBooster = new ItemPool<CardPrinted>();
            for (int i = 0; i < 21; i++)
                allBooster.addAll(booster.getBoosterPack());
            
            CardList forgeCardlist = allBooster.toForgeCardList();
            int stop = forgeCardlist.size();
            for (int i = 0; i < stop; i++) {
                forgeCardlist.remove(ai.choose(forgeCardlist, i));
            }
            //ai.checkDeckList(ai.deck);
        }
    }//runTest()
}
