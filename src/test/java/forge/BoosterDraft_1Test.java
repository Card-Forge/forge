package forge;

import org.testng.annotations.Test;


import forge.game.limited.BoosterDraft;
import forge.game.limited.CardPoolLimitation;
import forge.item.CardPrinted;
import forge.item.ItemPoolView;


/**
 * Unit test for simple App.
 */
@Test(groups = {"UnitTest"}, timeOut = 1000)
public class BoosterDraft_1Test {

    /**
     *
     */
    @Test(groups = {"UnitTest", "fast"}, timeOut = 1000)
    public void BoosterDraft_1Test1() throws Exception {
        BoosterDraft draft = new BoosterDraft(CardPoolLimitation.Full);
        while (draft.hasNextChoice()) {
            ItemPoolView<CardPrinted> list = draft.nextChoice();
            System.out.println(list.countAll());
            draft.setChoice(list.toFlatList().get(0));
        }
    }
}
