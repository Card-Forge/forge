package forge.card.cardFactory;

import forge.Card;
import forge.CardList;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.swing.OldGuiNewGame;
import net.slightlymagic.braids.util.ClumsyRunnable;
import net.slightlymagic.braids.util.testng.BraidsAssertFunctions;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Set;
import java.util.TreeSet;

//import net.slightlymagic.braids.testng.BraidsAssertFunctions;

/**
 * <p>Mana_PartTest class.</p>
 *
 * @author Forge
 * @version $Id$
 */
@Test(groups = {"UnitTest"}, timeOut = 5000)
public class CardFactoryTest implements NewConstants {

    private static CardFactoryInterface factory;
    static {
        OldGuiNewGame.loadDynamicGamedata();
        factory = new LazyCardFactory(ForgeProps.getFile(CARDSFOLDER));
    }


    /**
     * Just a quick test to see if Arc-Slogger is in the database, and if it
     * has the correct owner.
     */
    @Test(enabled = true, timeOut = 5000)
    public final void test_getCard_1() {
        final Card card = factory.getCard("Arc-Slogger", null);
        Assert.assertNotNull(card, "card is not null");
        Assert.assertNull(card.getOwner(), "card has correct owner");
    }

    /**
     * Make sure the method throws an exception when it's supposed to.
     *
     * This doesn't work with LazyCardFactory, so it is too slow to enable by default.
     */
    @Test(enabled = false, timeOut = 5000)
    public final void test_getRandomCombinationWithoutRepetition_tooLarge() {
        BraidsAssertFunctions.assertThrowsException(IllegalArgumentException.class,
                new ClumsyRunnable() {
            public void run() throws Exception {
                factory.getRandomCombinationWithoutRepetition(factory.size());
            }
        });

        BraidsAssertFunctions.assertThrowsException(IllegalArgumentException.class,
                new ClumsyRunnable() {
            public void run() throws Exception {
                int largeDivisorForRandomCombo = 4;
                factory.getRandomCombinationWithoutRepetition(factory.size() / largeDivisorForRandomCombo);
            }
        });
    }

    /**
     * Make sure the method works.
     *
     * This doesn't work with LazyCardFactory, so it is too slow to enable by default.
     */
    @Test(enabled = false, timeOut = 5000)
    public final void test_getRandomCombinationWithoutRepetition_oneTenth() {
        int divisor = 10;
        final CardList actual = factory.getRandomCombinationWithoutRepetition(factory.size() / divisor);

        final Set<String> cardNames = new TreeSet<String>();

        for (Card card : actual) {
            Assert.assertNotNull(card);
            cardNames.add(card.getName());
        }

        // Make sure we got a unique set of card names and that all are
        // accounted for.
        Assert.assertEquals(actual.size(), cardNames.size());
    }
}
