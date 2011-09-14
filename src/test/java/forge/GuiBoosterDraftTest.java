package forge;

import forge.deck.Deck;
import forge.game.GameType;
import forge.game.limited.BoosterDraft_1;
import forge.game.limited.CardPoolLimitation;
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
        Constant.Runtime.gameType = GameType.Draft;
        Constant.Runtime.HumanDeck[0] = new Deck(GameType.Sealed);

        DeckEditorDraft g = new DeckEditorDraft();
        g.showGui(new BoosterDraft_1(CardPoolLimitation.Full));
        Assert.assertNotNull(g);
        g.dispose();
    }

}
