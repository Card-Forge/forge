package forge;

import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.testng.annotations.Test;

import forge.game.limited.ReadDraftRankings;

/**
 * Tests for DeckWants.
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
        Map<String, List<String>> rankings = rdr.getDraftRankings();
        Assert.assertNotNull(rankings);
        Assert.assertEquals("Garruk Primal Hunter", rankings.get("M13").get(0));
        Assert.assertEquals("Clone", rankings.get("M13").get(37));
        Assert.assertEquals("Tamiyo the Moon Sage", rankings.get("AVR").get(0));
    }
}
