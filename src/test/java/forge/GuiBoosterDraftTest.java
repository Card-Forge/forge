package forge;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.deck.Deck;
import forge.game.GameType;
import forge.game.limited.BoosterDraft;
import forge.game.limited.CardPoolLimitation;
import forge.gui.deckeditor.DeckEditorDraft;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, enabled = false)
public class GuiBoosterDraftTest {

    /**
     * Gui booster draft test1.
     */
    @Test(groups = { "UnitTest", "fast" }, enabled = false)
    public void guiBoosterDraftTest1() {
        Constant.Runtime.setGameType(GameType.Draft);
        Constant.Runtime.HUMAN_DECK[0] = new Deck(GameType.Sealed);

        final DeckEditorDraft g = new DeckEditorDraft();
        g.showGui(new BoosterDraft(CardPoolLimitation.Full));
        Assert.assertNotNull(g);
        g.dispose();
    }

}
