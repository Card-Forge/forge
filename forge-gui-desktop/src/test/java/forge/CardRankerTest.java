package forge;

import forge.card.CardRarity;
import forge.card.CardRules;
import forge.item.PaperCard;
import forge.limited.CardRanker;
import forge.properties.ForgeConstants;
import forge.util.FileUtil;
import org.junit.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Test(timeOut = 1000, enabled = true)
public class CardRankerTest {

    private CardRanker ranker = new CardRanker();
    @BeforeTest
    void setupTest() {
        GuiBase.setInterface(new GuiDesktop());
    }

    @Test(timeOut = 1000, enabled = true)
    void testRank() {
        PaperCard cp = readCard("hero_of_goma_fada.txt");
        Assert.assertEquals("Hero of Goma Fada", cp.getName());

        List<PaperCard> list = new ArrayList<PaperCard>();
        PaperCard c0 = readCard("makindi_patrol.txt");
        list.add(c0);
        PaperCard c1 = readCard("hero_of_goma_fada.txt");
        list.add(c1);
        PaperCard c2 = readCard("altars_reap.txt");
        list.add(c2);
        PaperCard c3 = readCard("plains.txt");
        list.add(c3);

        List<PaperCard> ranked = ranker.rankCardsInDeck(list);
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
        // Don't care what the actual rarity is here.
        return new PaperCard(crr.getCard(), "BFZ", CardRarity.Common, 0);
    }
}
