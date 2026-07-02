package forge.download;

import forge.gui.download.ScryfallBulkData;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link ScryfallBulkData}.
 *
 * Card UUIDs are stored in {@code res/cdn_uuid/{setCode}/{collectorNumber}.json} files
 * (part of the assets zip) and loaded at runtime by {@link forge.gui.download.CdnUuidCache}.
 */
@Test(groups = {"UnitTest"})
public class ScryfallBulkDataTest {

    @Test
    public void testCdnUrlFormula() {
        String uuid = "4e7a547f-d1b0-4f4e-9a99-3c44fc89c048";
        Assert.assertEquals(
                ScryfallBulkData.cdnUrl(uuid, "front", "normal"),
                "https://cards.scryfall.io/normal/front/4/e/" + uuid + ".jpg");
        Assert.assertEquals(
                ScryfallBulkData.cdnUrl(uuid, "back", "art_crop"),
                "https://cards.scryfall.io/art_crop/back/4/e/" + uuid + ".jpg");
    }
}
