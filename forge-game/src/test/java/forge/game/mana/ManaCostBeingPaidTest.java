package forge.game.mana;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import static forge.card.MagicColor.COLORLESS;
import static forge.card.MagicColor.GREEN;
import static forge.card.MagicColor.RED;
import static forge.card.MagicColor.WHITE;

import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;

public class ManaCostBeingPaidTest {

	@Test
	public void testPayManaViaConvoke() {
		runConvokeTest("1 W W", new byte[] { WHITE, COLORLESS, WHITE }, new String[] { "{1}{W}{W}", "{1}{W}", "{W}" });
		runConvokeTest("1 W W", new byte[] { COLORLESS, WHITE, WHITE }, new String[] { "{1}{W}{W}", "{W}{W}", "{W}" });
		runConvokeTest("1 W W", new byte[] { GREEN, WHITE, WHITE }, new String[] { "{1}{W}{W}", "{W}{W}", "{W}" });
		runConvokeTest("1 W G", new byte[] { GREEN, RED, WHITE }, new String[] { "{1}{W}{G}", "{1}{W}", "{W}" });
	}

	private void runConvokeTest(String initialCost, byte[] colorsToPay, String[] expectedRemainder) {

		ManaCostBeingPaid costBeingPaid = createManaCostBeingPaid(initialCost);

		for (int i = 0; i < colorsToPay.length; i++) {
			AssertJUnit.assertEquals(expectedRemainder[i], costBeingPaid.toString());
			costBeingPaid.payManaViaConvoke(colorsToPay[i]);
		}

		AssertJUnit.assertEquals("0", costBeingPaid.toString());
	}

	private ManaCostBeingPaid createManaCostBeingPaid(String costString) {
		ManaCostParser parsedCostString = new ManaCostParser(costString);
		ManaCost manaCost = new ManaCost(parsedCostString);

		return new ManaCostBeingPaid(manaCost);
	}
}
