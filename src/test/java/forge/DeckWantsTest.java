package forge;

import java.io.File;
import java.util.List;

import org.testng.annotations.Test;

import junit.framework.Assert;

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
     *_/
    @Test(timeOut = 1000, enabled = true)
    void test() {
        List<String> cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/g", "griffin_rider.txt"));
        Card c = CardReader.readCard(cardLines);
        Assert.assertEquals("Griffin Rider", c.getName());
        Assert.assertNotNull(c.getDeckWants());
        Assert.assertEquals(DeckWants.Type.TYPE, c.getDeckWants().getType());

        cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/a", "assault_griffin.txt"));
        Card assaultGriffin = CardReader.readCard(cardLines);
        CardList cl = new CardList();
        cl.add(assaultGriffin);
        cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/a", "auramancer.txt"));
        Card auramancer = CardReader.readCard(cardLines);
        cl.add(auramancer);

        Assert.assertEquals(1, c.getDeckWants().filter(cl).size());
        Assert.assertEquals("Assault Griffin", c.getDeckWants().filter(cl).get(0).getName());
        Assert.assertEquals(1, c.getDeckWants().getMinCardsNeeded());
    }

    /**
     * Filter for cards.
     *_/
    @Test(timeOut = 1000, enabled = true)
    void testCards() {
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
        cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/c", "crown_of_empires.txt"));
        Card cr = CardReader.readCard(cardLines);
        cl.add(cr);

        Assert.assertEquals(2, c.getDeckWants().filter(cl).size());
        Assert.assertEquals(2, c.getDeckWants().getMinCardsNeeded());
    }

    /**
     * Filter for keywords.
     *_/
    @Test(timeOut = 1000, enabled = true)
    void testKeywords() {
        List<String> cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/m", "mwonvuli_beast_tracker.txt"));
        Card c = CardReader.readCard(cardLines);
        Assert.assertNotNull(c.getDeckWants());
        Assert.assertEquals(DeckWants.Type.KEYWORDANY, c.getDeckWants().getType());

        cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/a", "acidic_slime.txt"));
        Card card = CardReader.readCard(cardLines);
        CardList cl = new CardList();
        cl.add(card);
        cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/a", "ajanis_sunstriker.txt"));
        Card card2 = CardReader.readCard(cardLines);
        cl.add(card2);

        Assert.assertEquals(1, c.getDeckWants().filter(cl).size());
        Assert.assertEquals(1, c.getDeckWants().getMinCardsNeeded());
    }


    /**
     * Filter for color.
     *_/
    @Test(timeOut = 1000, enabled = true)
    void testColor() {
        List<String> cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/w", "wurms_tooth.txt"));
        Card c = CardReader.readCard(cardLines);
        Assert.assertNotNull(c.getDeckWants());
        Assert.assertEquals(DeckWants.Type.COLOR, c.getDeckWants().getType());

        cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/l", "llanowar_elves.txt"));
        Card card = CardReader.readCard(cardLines);
        CardList cl = new CardList();
        cl.add(card);
        cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/u", "unsummon.txt"));
        card = CardReader.readCard(cardLines);
        cl.add(card);

        cl.getOnly2Colors("green", "white");
        Assert.assertEquals(1, c.getDeckWants().filter(cl).size());
        Assert.assertEquals(1, c.getDeckWants().getMinCardsNeeded());
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
    @Test(timeOut = 1000, enabled = true)
    void testNoFilter() {
        List<String> cardLines = FileUtil
                .readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/a", "assault_griffin.txt"));
        Card c = CardReader.readCard(cardLines);
        Assert.assertEquals("Assault Griffin", c.getName());
        Assert.assertNotNull(c.getDeckWants());
        Assert.assertEquals(DeckWants.Type.NONE, c.getDeckWants().getType());

        Card assaultGriffin = CardReader.readCard(cardLines);
        CardList cl = new CardList();
        cl.add(assaultGriffin);
        Assert.assertEquals(1, c.getDeckWants().filter(cl).size());

    }
    */
}
