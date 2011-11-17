package forge;

import java.util.EnumSet;

import org.testng.annotations.Test;

import forge.card.mana.ManaCost;

/**
 * The Class CardColorTest.
 */
@Test(groups = { "UnitTest" }, timeOut = 1000)
public class CardColorTest {

    /**
     * Card color test1.
     */
    @Test(groups = { "UnitTest", "fast" }, timeOut = 1000)
    public void cardColorTest1() {
        final ManaCost mc = new ManaCost("R W U");
        final EnumSet<Color> col = Color.convertManaCostToColor(mc);
        System.out.println(col.toString());
    }
}
