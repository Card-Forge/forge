package forge;

import forge.deck.Deck;
import forge.game.GameType;
import forge.game.limited.BoosterDraft;
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
        Constant.Runtime.setGameType(GameType.Draft);
        Constant.Runtime.HUMAN_DECK[0] = new Deck(GameType.Sealed);

        DeckEditorDraft g = new DeckEditorDraft();
        g.showGui(new BoosterDraft(CardPoolLimitation.Full));
        Assert.assertNotNull(g);
        g.dispose();
    }

}
