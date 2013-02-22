package forge;

import java.io.File;
import java.util.List;

import junit.framework.Assert;

import org.testng.annotations.Test;

import forge.card.CardRules;
import forge.card.CardRulesReader;
import forge.game.limited.ReadDraftRankings;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.FileUtil;

/**
 * Tests for ReadDraftRankings.
 * 
 */
@Test(enabled = true)
public class ReadDraftRankingsTest {

    /**
     * Card test.
     */
    @Test(enabled = true)
    void test() {
        ReadDraftRankings rdr = new ReadDraftRankings();
        Assert.assertNotNull(rdr);
        
        CardRulesReader cr = new CardRulesReader();

        List<String> cardLines = FileUtil.readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/g", "garruk_primal_hunter.txt"));
        CardRules c = cr.readCard(cardLines);
        Assert.assertEquals(1.0 / 234.0, rdr.getRanking(c.getName(), "M13").doubleValue());

        cardLines = FileUtil.readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/c", "clone.txt"));
        c = cr.readCard(cardLines);
        Assert.assertEquals(38.0 / 234.0, rdr.getRanking(c.getName(), "M13").doubleValue());

        cardLines = FileUtil.readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/t", "tamiyo_the_moon_sage.txt"));
        c = cr.readCard(cardLines);
        Assert.assertEquals(1.0 / 234.0, rdr.getRanking(c.getName(), "AVR").doubleValue());

        // Mikaeus, the Lunarch has a comma in its name in the rankings file
        cardLines = FileUtil.readFile(new File(ForgeProps.getFile(NewConstants.CARDSFOLDER) + "/m", "mikaeus_the_lunarch.txt"));
        c = cr.readCard(cardLines);
        Assert.assertEquals(4.0 / 255.0, rdr.getRanking(c.getName(), "ISD").doubleValue());

    }
}
