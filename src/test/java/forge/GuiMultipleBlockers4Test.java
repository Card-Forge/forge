package forge;

import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: dhudson
 */
@Test(groups = {"UnitTest"}, timeOut = 1000, enabled = false)
public class GuiMultipleBlockers4Test {

    /**
     *
     *
     */
    @Test(timeOut = 1000, enabled = false)
    public void GuiMultipleBlockers4Test1() {
        CardList list = new CardList();
        list.add(AllZone.getCardFactory().getCard("Elvish Piper", null));
        list.add(AllZone.getCardFactory().getCard("Lantern Kami", null));
        list.add(AllZone.getCardFactory().getCard("Frostling", null));
        list.add(AllZone.getCardFactory().getCard("Frostling", null));

        for (int i = 0; i < 2; i++) {
            new GuiMultipleBlockers4(null, list, i + 1, null);
        }
    }
}
