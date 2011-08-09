package forge;

import forge.deck.Deck;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"})
public class GuiWinLoseTest {
    /**
     *
     *
     */
    @Test()
    public void GuiWinLoseTest1() {
        Gui_WinLose dialog = new Gui_WinLose();
        dialog.setVisible(true);
        Assert.assertNotNull(dialog);
        dialog.dispose();
    }
}
