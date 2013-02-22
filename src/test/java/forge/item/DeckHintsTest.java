package forge.item;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.testng.annotations.Test;

import forge.card.CardRarity;
import forge.card.CardRulesReader;
import forge.card.DeckHints;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.FileUtil;

/**
 * Tests for DeckHints.
 * 
 */
@Test(timeOut = 1000, enabled = true)
public class DeckHintsTest {

    /**
     * Card test.
     */
    @Test(timeOut = 1000, enabled = true)
    void test() {
        CardPrinted cp = readCard("griffin_rider.txt");
        Assert.assertEquals("Griffin Rider", cp.getName());
        DeckHints hints = cp.getRules().getAiHints().getDeckHints();
        Assert.assertNotNull(hints);
        Assert.assertEquals(DeckHints.Type.TYPE, hints.getType());

        List<CardPrinted> list = new ArrayList<CardPrinted>();
        CardPrinted c0 = readCard("assault_griffin.txt");
        list.add(c0);
        CardPrinted c1 = readCard("auramancer.txt");
        list.add(c1);

        Assert.assertEquals(1, hints.filter(list).size());
        Assert.assertEquals("Assault Griffin", hints.filter(list).get(0).getName());
    }

    /**
     * Filter for cards.
     */
    @Test(timeOut = 1000, enabled = true)
    void testCards() {
        CardPrinted cp = readCard("throne_of_empires.txt");
        Assert.assertEquals("Throne of Empires", cp.getName());
        DeckHints hints = cp.getRules().getAiHints().getDeckHints();
        Assert.assertNotNull(hints);
        Assert.assertEquals(DeckHints.Type.NAME, hints.getType());

        List<CardPrinted> list = new ArrayList<CardPrinted>();
        CardPrinted c0 = readCard("assault_griffin.txt");
        list.add(c0);
        CardPrinted c1 = readCard("scepter_of_empires.txt");
        list.add(c1);
        CardPrinted c2 = readCard("crown_of_empires.txt");
        list.add(c2);

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
        Assert.assertEquals(DeckHints.Type.KEYWORD, hints.getType());

        List<CardPrinted> list = new ArrayList<CardPrinted>();
        CardPrinted c0 = readCard("acidic_slime.txt");
        list.add(c0);
        CardPrinted c1 = readCard("ajanis_sunstriker.txt");
        list.add(c1);

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
        Assert.assertEquals(DeckHints.Type.COLOR, hints.getType());

        List<CardPrinted> list = new ArrayList<CardPrinted>();
        CardPrinted c0 = readCard("llanowar_elves.txt");
        list.add(c0);
        CardPrinted c1 = readCard("unsummon.txt");
        list.add(c1);

        Assert.assertEquals(1, hints.filter(list).size());
    }

    /**
     * 
     * Test for no wants.
     */
    @Test(timeOut = 1000, enabled = false)
    void testNoFilter() {
        CardPrinted cp = readCard("assault_griffin.txt");
        DeckHints hints = cp.getRules().getAiHints().getDeckHints();
        Assert.assertEquals("Assault Griffin", cp.getName());
        Assert.assertNotNull(hints);
        Assert.assertEquals(DeckHints.Type.NONE, hints.getType());

        List<CardPrinted> list = new ArrayList<CardPrinted>();
        CardPrinted c0 = readCard("assault_griffin.txt");
        list.add(c0);

        Assert.assertEquals(1, hints.filter(list).size());
    }

    /**
     * Create a CardPrinted from the given filename.
     * 
     * @param filename
     *            the filename
     * @return the CardPrinted
     */
    protected CardPrinted readCard(String filename) {
        String firstLetter = filename.substring(0, 1);
        File dir = new File(ForgeProps.getFile(NewConstants.CARDSFOLDER), firstLetter);
        File txtFile = new File(dir, filename);

        CardRulesReader crr = new CardRulesReader();
        for (String line : FileUtil.readFile(txtFile)) {
            crr.parseLine(line);
        }
        // Don't care what the actual set or rarity is here.
        return CardPrinted.build(crr.getCard(), "M11", CardRarity.Common, 0);
    }

}
