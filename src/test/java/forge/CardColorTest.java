package forge;

import forge.card.mana.ManaCost;
import org.testng.annotations.Test;

import java.util.EnumSet;

/**
 *
 */
@Test(groups = {"UnitTest"}, timeOut = 1000)
public class CardColorTest {
    /**
     *
     */
    @Test(groups = {"UnitTest", "fast"}, timeOut = 1000)
    public void CardColorTest1() {
        ManaCost mc = new ManaCost("R W U");
        EnumSet<Color> col = Color.convertManaCostToColor(mc);
        System.out.println(col.toString());
    }
}
