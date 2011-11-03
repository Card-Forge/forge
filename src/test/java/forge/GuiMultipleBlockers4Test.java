package forge;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA. User: dhudson
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class GuiMultipleBlockers4Test {

    /**
     * Gui multiple blockers4 test1.
     */
    @Test(timeOut = 1000, enabled = false)
    public void GuiMultipleBlockers4Test1() {
        final CardList list = new CardList();
        list.add(AllZone.getCardFactory().getCard("Elvish Piper", null));
        list.add(AllZone.getCardFactory().getCard("Lantern Kami", null));
        list.add(AllZone.getCardFactory().getCard("Frostling", null));
        list.add(AllZone.getCardFactory().getCard("Frostling", null));

        for (int i = 0; i < 2; i++) {
            new GuiMultipleBlockers(null, list, i + 1, null);
        }
    }
}
