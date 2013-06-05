package forge;

import org.testng.annotations.Test;

import forge.game.limited.BoosterDraft;
import forge.game.limited.LimitedPoolType;
import forge.item.PaperCard;
import forge.item.ItemPoolView;

/**
 * Unit test for simple App.
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class BoosterDraft1Test {

    /**
     * Booster draft_1 test1.
     * 
     * @throws Exception
     *             the exception
     */
    @Test(groups = { "UnitTest", "fast" }, timeOut = 1000, enabled = false)
    public void boosterDraft1Test1() throws Exception {
        final BoosterDraft draft = new BoosterDraft(LimitedPoolType.Full);
        while (draft.hasNextChoice()) {
            final ItemPoolView<PaperCard> list = draft.nextChoice();
            System.out.println(list.countAll());
            draft.setChoice(list.toFlatList().get(0));
        }
    }
}
