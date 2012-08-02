package forge.item;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.testng.annotations.Test;

import forge.card.CardRarity;
import forge.card.CardRulesReader;
import forge.card.DeckWants;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.FileUtil;

/**
 * Tests for DeckWants.
 * 
 */
@Test(timeOut = 1000, enabled = true)
public class DeckWantsTest {

    /**
     * Card test.
     */
    @Test(timeOut = 1000, enabled = true)
    void test() {
        CardPrinted cp = readCard("griffin_rider.txt");
        Assert.assertEquals("Griffin Rider", cp.getName());
        DeckWants hints = cp.getCard().getDeckWants();
        Assert.assertNotNull(hints);
        Assert.assertEquals(DeckWants.Type.TYPE, hints.getType());

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
        DeckWants hints = cp.getCard().getDeckWants();
        Assert.assertNotNull(hints);
        Assert.assertEquals(DeckWants.Type.NAME, hints.getType());

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
     *_/
    @Test(timeOut = 1000, enabled = true)
    void testKeywords() {
        CardPrinted cp = readCard("mwonvuli_beast_tracker.txt");
        DeckWants hints = cp.getCard().getDeckWants();
        Assert.assertNotNull(hints);
        Assert.assertEquals(DeckWants.Type.KEYWORDANY, hints.getType());

        List<CardPrinted> list = new ArrayList<CardPrinted>();
        CardPrinted c0 = readCard("acidic_slime.txt");
        list.add(c0);
        CardPrinted c1 = readCard("ajanis_sunstriker.txt");
        list.add(c1);

        Assert.assertEquals(1, hints.filter(list).size());
    }
    */


    /**
     * Filter for color.
     */
    @Test(timeOut = 1000, enabled = true)
    void testColor() {
        CardPrinted cp = readCard("wurms_tooth.txt");
        DeckWants hints = cp.getCard().getDeckWants();
        Assert.assertNotNull(hints);
        Assert.assertEquals(DeckWants.Type.COLOR, hints.getType());

        List<CardPrinted> list = new ArrayList<CardPrinted>();
        CardPrinted c0 = readCard("llanowar_elves.txt");
        list.add(c0);
        CardPrinted c1 = readCard("unsummon.txt");
        list.add(c1);

        Assert.assertEquals(1, hints.filter(list).size());
    }

    /**
     * Failing filter for cards.
     *_/
    @Test(timeOut = 1000, enabled = true)
    void testFailCards() {
        List<String> cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/t", "throne_of_empires.txt"));
        Card c = CardReader.readCard(cardLines);
        Assert.assertEquals("Throne of Empires", c.getName());
        Assert.assertNotNull(c.getDeckWants());
        Assert.assertEquals(DeckWants.Type.LISTALL, c.getDeckWants().getType());

        cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/a", "assault_griffin.txt"));
        Card assaultGriffin = CardReader.readCard(cardLines);
        CardList cl = new CardList();
        cl.add(assaultGriffin);
        cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/s", "scepter_of_empires.txt"));
        Card sc = CardReader.readCard(cardLines);
        cl.add(sc);

        Assert.assertEquals(0, c.getDeckWants().filter(cl).size());
        Assert.assertEquals(2, c.getDeckWants().getMinCardsNeeded());
    }

    /**
     * Card test for junk deck wants.
     *_/
    @Test(timeOut = 1000, enabled = true)
    void testJunk() {
        List<String> cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/g", "griffin_rider.txt"));
        Card c = CardReader.readCard(cardLines);
        c.setSVar("DeckWants", "Junk$Junk");
        Assert.assertNotNull(c.getDeckWants());
        Assert.assertEquals(DeckWants.Type.NONE, c.getDeckWants().getType());
    }

    /**
     * 
     * Test for no wants.
     *_/
    @Test(timeOut = 1000, enabled = false)
    void testNoFilter() {

        CardRules c = readCard("assault_griffin.txt");
        Assert.assertEquals("Assault Griffin", c.getName());
        Assert.assertNotNull(c.getDeckWants());
        Assert.assertEquals(DeckWants.Type.NONE, c.getDeckWants().getType());


        List<String> cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/a", "assault_griffin.txt"));

        CardRulesReader crr = new CardRulesReader();
        for(String line: cardLines)
            crr.parseLine(line);        
        Card assaultGriffin = CardReader.readCard(cardLines);
        CardList cl = new CardList();
        cl.add(assaultGriffin);
        Assert.assertEquals(1, c.getDeckWants().filter(cl).size());

    }
    */
    
    protected CardPrinted readCard(String filename) {
        String firstLetter = filename.substring(0,1);
        File dir = new File(ForgeProps.getFile(NewConstants.CARDSFOLDER), firstLetter);
        File txtFile = new File(dir, filename);

        CardRulesReader crr = new CardRulesReader();
        for(String line: FileUtil.readFile(txtFile))
            crr.parseLine(line);
        // Don't care what the actual set or rarity is here.
        return CardPrinted.build(crr.getCard(), "M11", CardRarity.Common, 0);
    }
    
}
