package forge.game.mana;

import static forge.card.MagicColor.COLORLESS;
import static forge.card.MagicColor.GREEN;
import static forge.card.MagicColor.RED;
import static forge.card.MagicColor.WHITE;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;

public class ManaCostBeingPaidTest {

    @Test
    public void testPayManaViaConvoke() {
        runConvokeTest("1 W W", new byte[]{WHITE, COLORLESS, WHITE}, new String[]{"{1}{W}{W}", "{1}{W}", "{W}"});
        runConvokeTest("1 W W", new byte[]{COLORLESS, WHITE, WHITE}, new String[]{"{1}{W}{W}", "{W}{W}", "{W}"});
        runConvokeTest("1 W W", new byte[]{GREEN, WHITE, WHITE}, new String[]{"{1}{W}{W}", "{W}{W}", "{W}"});
        runConvokeTest("1 W G", new byte[]{GREEN, RED, WHITE}, new String[]{"{1}{W}{G}", "{1}{W}", "{W}"});
    }

    private void runConvokeTest(String initialCost, byte[] colorsToPay, String[] expectedRemainder) {
        ManaCostBeingPaid costBeingPaid = createManaCostBeingPaid(initialCost);
        for (int i = 0; i < colorsToPay.length; i++) {
            assertEquals(expectedRemainder[i], costBeingPaid.toString());
            costBeingPaid.payManaViaConvoke(colorsToPay[i]);
        }
        assertEquals("0", costBeingPaid.toString());
    }

    private ManaCostBeingPaid createManaCostBeingPaid(String cost) {
        ManaCostParser parser = new ManaCostParser(cost);
        return new ManaCostBeingPaid(new ManaCost(parser));
    }
}
