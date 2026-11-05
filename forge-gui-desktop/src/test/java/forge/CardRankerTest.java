package forge;

import forge.card.CardRarity;
import forge.card.CardRules;
import forge.gamemodes.limited.CardRanker;
import forge.gamemodes.limited.DraftRankCache;
import forge.gui.GuiBase;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgeConstants;
import forge.util.FileUtil;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

@Test(timeOut = 1000, enabled = true)
public class CardRankerTest {

    @BeforeTest
    void setupTest() {
        GuiBase.setInterface(new GuiDesktop());
    }

    /**
     * The first draft ranking lookup lazily reads and parses the entire rankings
     * folder (~186 files, ~45k lines). Left to happen inside testRank, that one-time
     * load dominates the method's 1 second timeout - the test measures disk I/O
     * rather than ranking, and fails intermittently when the machine is busy running
     * the rest of the suite. Warming it here keeps the timeout meaningful.
     * Runs after @BeforeTest, so GuiBase is already set.
     */
    @BeforeClass
    void warmDraftRankings() {
        DraftRankCache.getRanking("Plains", "BFZ");
    }

    @Test(timeOut = 1000, enabled = true)
    void testRank() {
        List<PaperCard> list = new ArrayList<>();
        PaperCard c0 = readCard("makindi_patrol.txt");
        list.add(c0);
        PaperCard c1 = readCard("hero_of_goma_fada.txt");
        list.add(c1);
        PaperCard c2 = readCard("altars_reap.txt");
        list.add(c2);
        PaperCard c3 = readCard("plains.txt");
        list.add(c3);

        List<PaperCard> ranked = CardRanker.rankCardsInDeck(list);
        assertEquals("Hero of Goma Fada", ranked.get(0).getName());
        assertEquals("Makindi Patrol", ranked.get(1).getName());
        assertEquals("Altar's Reap", ranked.get(2).getName());
        assertEquals("Plains", ranked.get(3).getName());
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
        return new PaperCard(crr.getCard(), "BFZ", CardRarity.Common);
    }
}
