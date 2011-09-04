package forge;

import forge.deck.Deck;
import forge.game.limited.BoosterDraft_1;
import forge.gui.deckeditor.DeckEditorDraft;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"})
public class GuiBoosterDraftTest {

    /**
     *
     *
     */
    @Test(groups = {"UnitTest", "fast"})
    public void GuiBoosterDraftTest1() {
        Constant.Runtime.GameType[0] = Constant.GameType.Draft;
        Constant.Runtime.HumanDeck[0] = new Deck(Constant.GameType.Sealed);

        DeckEditorDraft g = new DeckEditorDraft();
        g.showGui(new BoosterDraft_1());
        Assert.assertNotNull(g);
        g.dispose();
    }

}
