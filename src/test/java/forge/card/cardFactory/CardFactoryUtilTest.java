package forge.card.cardFactory;

import org.testng.annotations.Test;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;

import forge.AllZone;
import forge.Card;
import forge.CardFilter;
import forge.CardList;
import forge.Counters;
import forge.card.cardfactory.CardFactoryUtil;

/**
 * <p>
 * Mana_PartTest class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
@Test(groups = { "UnitTest" }, timeOut = 1000, enabled = false)
public class CardFactoryUtilTest {

    /**
     * Card factory test1.
     */
    @Test(timeOut = 1000, enabled = false)
    public void CardFactoryTest1() {

        Generator<Card> in = YieldUtils.toGenerator(AllZone.getCardFactory());

        in = CardFilter.getColor(in, "black");

        CardList list = new CardList(in);
        list = list.getType("Creature");

        System.out.println("Most prominent creature type: " + CardFactoryUtil.getMostProminentCreatureType(list));

        final String manacost = "3 GW W W R B S";
        final String multipliedTwice = CardFactoryUtil.multiplyManaCost(manacost, 2);
        final String multipliedThrice = CardFactoryUtil.multiplyManaCost(manacost, 3);

        System.out.println(manacost + " times 2 = " + multipliedTwice);
        System.out.println(manacost + " times 3 = " + multipliedThrice);

        if (CardFactoryUtil.isNegativeCounter(Counters.M1M1)) {
            System.out.println("M1M1 is a bad counter!");
        } else {
            System.out.println("M1M1 is a good counter!");
        }
    }
}
