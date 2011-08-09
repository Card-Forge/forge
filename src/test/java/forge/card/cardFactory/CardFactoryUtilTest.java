package forge.card.cardFactory;

import static forge.card.cardFactory.CardFactoryUtil.getMostProminentCreatureType;
import static forge.card.cardFactory.CardFactoryUtil.isNegativeCounter;
import static forge.card.cardFactory.CardFactoryUtil.multiplyManaCost;

import org.testng.annotations.Test;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;

import forge.AllZone;
import forge.Card;
import forge.CardFilter;
import forge.CardList;
import forge.Counters;
import forge.properties.NewConstants;

/**
 * <p>Mana_PartTest class.</p>
 *
 * @author Forge
 * @version $Id$
 */
@Test(groups = {"UnitTest"}, timeOut = 1000, enabled = false)
public class CardFactoryUtilTest implements NewConstants {

    /**
     *
     */
    @Test(timeOut = 1000, enabled = false)
    public void CardFactoryTest1() {

        Generator<Card> in = YieldUtils.toGenerator(AllZone.getCardFactory());

        in = CardFilter.getColor(in, "black");
        
        CardList list = new CardList(in);
        list = list.getType("Creature");

        System.out.println("Most prominent creature type: " + getMostProminentCreatureType(list));


        String manacost = "3 GW W W R B S";
        String multipliedTwice = multiplyManaCost(manacost, 2);
        String multipliedThrice = multiplyManaCost(manacost, 3);

        System.out.println(manacost + " times 2 = " + multipliedTwice);
        System.out.println(manacost + " times 3 = " + multipliedThrice);

        if (isNegativeCounter(Counters.M1M1)) {
            System.out.println("M1M1 is a bad counter!");
        } else
            System.out.println("M1M1 is a good counter!");
    }
}
