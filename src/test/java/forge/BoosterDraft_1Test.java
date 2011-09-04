package forge;

import net.slightlymagic.maxmtg.Predicate;

import org.testng.annotations.Test;

import forge.card.CardRules;
import forge.card.CardPoolView;

import forge.game.limited.BoosterDraft_1;


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
        BoosterDraft_1 draft = new BoosterDraft_1();
        while (draft.hasNextChoice()) {
            CardPoolView list = draft.nextChoice();
            System.out.println(list.countAll());
            draft.setChoice(Predicate.getTrue(CardRules.class).first(list, CardPoolView.fnToCard, CardPoolView.fnToReference));
        }
    }
}
