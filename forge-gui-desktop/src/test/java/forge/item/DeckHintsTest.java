package forge.item;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Iterables;
import junit.framework.Assert;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import forge.GuiBase;
import forge.GuiDesktop;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.DeckHints;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;

/**
 * Tests for DeckHints.
 * 
 */
@Test(timeOut = 1000, enabled = true)
public class DeckHintsTest {

	@BeforeTest
	void setupTest() {
		GuiBase.setInterface(new GuiDesktop());
	}
	
    /**
     * Card test.
     */
    @Test(timeOut = 1000, enabled = true)
    void test() {
        PaperCard cp = readCard("griffin_rider.txt");
        Assert.assertEquals("Griffin Rider", cp.getName());
        DeckHints hints = cp.getRules().getAiHints().getDeckHints();
        Assert.assertNotNull(hints);
        Assert.assertTrue(hints.isValid());

        List<PaperCard> list = new ArrayList<PaperCard>();
        list.add(readCard("assault_griffin.txt"));
        list.add(readCard("auramancer.txt"));

        List<PaperCard> filtered = hints.filter(list);
        Assert.assertEquals(1, filtered.size());
        Assert.assertEquals("Assault Griffin", filtered.get(0).getName());
    }

    /**
     * Filter for cards.
     */
    @Test(timeOut = 1000, enabled = true)
    void testCards() {
        PaperCard cp = readCard("throne_of_empires.txt");
        Assert.assertEquals("Throne of Empires", cp.getName());
        DeckHints hints = cp.getRules().getAiHints().getDeckHints();
        Assert.assertNotNull(hints);
        Assert.assertTrue(hints.isValid());

        List<PaperCard> list = new ArrayList<PaperCard>();
        list.add(readCard("assault_griffin.txt"));
        list.add(readCard("scepter_of_empires.txt"));
        list.add(readCard("crown_of_empires.txt"));

        Assert.assertEquals(2, hints.filter(list).size());
    }

    /**
     * Filter for keywords.
     */
    @Test(timeOut = 1000, enabled = true)
    void testKeywords() {
        IPaperCard cp = readCard("mwonvuli_beast_tracker.txt");
        DeckHints hints = cp.getRules().getAiHints().getDeckHints();
        Assert.assertNotNull(hints);
        Assert.assertTrue(hints.isValid());

        List<PaperCard> list = new ArrayList<PaperCard>();
        list.add(readCard("acidic_slime.txt"));
        list.add(readCard("ajanis_sunstriker.txt"));

        Assert.assertEquals(1, hints.filter(list).size());
    }

    /**
     * Filter for color.
     */
    @Test(timeOut = 1000, enabled = true)
    void testColor() {
        IPaperCard cp = readCard("wurms_tooth.txt");
        DeckHints hints = cp.getRules().getAiHints().getDeckNeeds();
        Assert.assertNotNull(hints);
        Assert.assertTrue(hints.isValid());

        List<PaperCard> list = new ArrayList<PaperCard>();
        list.add(readCard("llanowar_elves.txt"));
        list.add(readCard("unsummon.txt"));

        Assert.assertEquals(1, hints.filter(list).size());
    }

    /**
     * 
     * Test for no wants.
     */
    @Test(timeOut = 1000, enabled = true)
    void testNoFilter() {
        PaperCard cp = readCard("assault_griffin.txt");
        DeckHints hints = cp.getRules().getAiHints().getDeckHints();
        Assert.assertEquals("Assault Griffin", cp.getName());
        Assert.assertNull(hints);
    }

    /**
     * Test for multiple.
     */
    @Test(timeOut = 1000, enabled = true)
    void testMultiple() {
        PaperCard pc = readCard("ruination_guide.txt");
        DeckHints hints = pc.getRules().getAiHints().getDeckHints();
        Assert.assertNotNull(hints);
        Assert.assertTrue(hints.isValid());

        List<PaperCard> list = new ArrayList<PaperCard>();
        list.add(readCard("assault_griffin.txt"));
        list.add(readCard("breaker_of_armies.txt"));
        list.add(readCard("benthic_infiltrator.txt"));

        Map<DeckHints.Type, Iterable<PaperCard>> filterByType = hints.filterByType(list);
        Assert.assertEquals(1, Iterables.size(filterByType.get(DeckHints.Type.KEYWORD)));
        Assert.assertEquals(1, Iterables.size(filterByType.get(DeckHints.Type.COLOR)));
    }

    /**
     * Test for has ability.
     */
    @Test(timeOut = 1000, enabled = true)
    void testDeckHasAbility() {
        PaperCard pc = readCard("kozileks_channeler.txt");
        DeckHints has = pc.getRules().getAiHints().getDeckHas();
        Assert.assertNotNull(has);

        PaperCard pc2 = readCard("kozileks_pathfinder.txt");
        DeckHints hints = pc2.getRules().getAiHints().getDeckHints();

        List<PaperCard> list = new ArrayList<>();
        list.add(pc);
        list.add(readCard("assault_griffin.txt"));

        Assert.assertEquals(1, hints.filter(list).size());
    }

    /**
     * Create a CardPrinted from the given filename.
     * 
     * @param filename
     *            the filename
     * @return the CardPrinted
     */
    protected PaperCard readCard(String filename) {
        String firstLetter = filename.substring(0, 1);
        File dir = new File(ForgeConstants.CARD_DATA_DIR, firstLetter);
        File txtFile = new File(dir, filename);

        CardRules.Reader crr = new CardRules.Reader();
        for (String line : FileUtil.readFile(txtFile)) {
            crr.parseLine(line);
        }
        // Don't care what the actual set or rarity is here.
        return new PaperCard(crr.getCard(), "M11", CardRarity.Common, 0);
    }

}
