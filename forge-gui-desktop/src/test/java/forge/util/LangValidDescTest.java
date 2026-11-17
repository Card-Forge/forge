package forge.util;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

/** #5634: script "Valid" strings read as words wherever a stack description names what an effect affects. */
public class LangValidDescTest {
    private Lang lang;

    @BeforeClass
    public void setUp() {
        Lang.createInstance("en-US");
        lang = Lang.getInstance();
    }

    @Test
    public void plainTypesKeepTheirOldForm() {
        assertEquals("creature", lang.formatValidDesc("Creature"));
        assertEquals("card", lang.formatValidDesc("Card"));
        assertEquals("Goblin", lang.formatValidDesc("Goblin"));
    }

    @Test
    public void restrictionsBecomeWords() {
        assertEquals("colored permanent", lang.formatValidDesc("Permanent.nonColorless"));
        assertEquals("colored permanents", lang.formatValidDesc("Permanent.nonColorless", true, false));
        assertEquals("basic land", lang.formatValidDesc("Land.Basic"));
        assertEquals("nonland permanent", lang.formatValidDesc("Permanent.nonLand"));
        assertEquals("other creature you control", lang.formatValidDesc("Creature.Other+YouCtrl"));
        assertEquals("creature with mana value 3 or less", lang.formatValidDesc("Creature.cmcLE3"));
    }

    @Test
    public void cardTypesAreDescribedAsCards() {
        assertEquals("land card you own", lang.formatValidDesc("Land.YouOwn", false, true));
        assertEquals("land card you own", lang.formatValidDesc("Card.Land+YouOwn", false, true));
    }

    @Test
    public void compoundAndCommaSeparatedDescriptionsKeepTheirWording() {
        assertEquals("creature or planeswalker",
                lang.describeValidList("Creature,Planeswalker"));
        assertEquals("white and blue card",
                lang.formatValidDesc("EACH Card.White & Card.Blue"));
    }

    @Test
    public void anythingElseIsLeftAlone() {
        assertEquals("other creature", lang.formatValidDesc("Creature.!Self"));
        assertEquals("Creature.attackedThisTurn,Land", lang.formatValidDesc("Creature.attackedThisTurn,Land"));
    }
}
