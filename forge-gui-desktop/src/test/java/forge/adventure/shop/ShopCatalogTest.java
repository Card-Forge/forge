package forge.adventure.shop;

import com.badlogic.gdx.Input;
import forge.adventure.util.KeyBinding;
import forge.adventure.util.SaveFileData;
import forge.itemmanager.ItemColumn;
import forge.itemmanager.ItemColumnConfig;
import forge.itemmanager.SColumnUtil;
import forge.util.Localizer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Test(groups = { "UnitTest", "fast" })
public class ShopCatalogTest {
    @BeforeClass
    public void initializeLocalization() {
        Path languages = Paths.get(System.getProperty("user.dir"), "forge-gui", "res", "languages");
        if (!Files.isDirectory(languages)) {
            languages = Paths.get(System.getProperty("user.dir"), "..", "forge-gui", "res", "languages");
        }
        Localizer.getInstance().initialize("en-US", languages.normalize().toString());
    }

    @Test
    public void standardCatalogColumnsHaveCompleteSortAndDisplayFunctions() {
        for (ItemColumnConfig column : SColumnUtil.getCatalogDefaultColumns(false).values()) {
            Assert.assertNotNull(new ItemColumn(column));
        }
    }

    @Test
    public void catalogHotkeyUsesC() {
        Assert.assertTrue(KeyBinding.Catalog.isPressed(Input.Keys.C));
        Assert.assertFalse(KeyBinding.Catalog.isPressed(Input.Keys.B));
    }

    @Test
    public void offerIdentityIncludesEverySourceComponent() {
        ShopOfferId original = new ShopOfferId("poi", 7, 11L, 3);

        Assert.assertEquals(original, new ShopOfferId("poi", 7, 11L, 3));
        Assert.assertNotEquals(original, new ShopOfferId("other", 7, 11L, 3));
        Assert.assertNotEquals(original, new ShopOfferId("poi", 8, 11L, 3));
        Assert.assertNotEquals(original, new ShopOfferId("poi", 7, 12L, 3));
        Assert.assertNotEquals(original, new ShopOfferId("poi", 7, 11L, 4));
    }

    @Test
    public void revisitingSameInventoryDoesNotDuplicateOffer() {
        ShopCatalog catalog = new ShopCatalog();
        ShopOfferId id = new ShopOfferId("poi", 7, 11L, 3);

        ShopCatalogOffer first = catalog.observe(id, "Card|SET|[1]", null,
                "Shop", "Town", false);
        ShopCatalogOffer second = catalog.observe(id, "Other|SET|[2]", null,
                "Other Shop", "Other Town", false);

        Assert.assertSame(second, first);
        Assert.assertEquals(catalog.size(), 1);
    }

    @Test
    public void identicalCardsFromDifferentSlotsRemainDistinctOffers() {
        ShopCatalog catalog = new ShopCatalog();
        catalog.observe(new ShopOfferId("poi", 7, 11L, 3), "Card|SET|[1]", null,
                "Shop", "Town", false);
        catalog.observe(new ShopOfferId("poi", 7, 11L, 4), "Card|SET|[1]", null,
                "Shop", "Town", false);

        Assert.assertEquals(catalog.getAvailableOffers().size(), 2);
    }

    @Test
    public void finiteOffersAreConsumedButUnlimitedOffersRemainAvailable() {
        ShopCatalog catalog = new ShopCatalog();
        ShopOfferId finiteId = new ShopOfferId("poi", 7, 11L, 3);
        ShopOfferId unlimitedId = new ShopOfferId("poi", 8, 12L, 4);
        catalog.observe(finiteId, "Card|SET|[1]", null, "Shop", "Town", false);
        catalog.observe(unlimitedId, "Land|SET|[2]", null, "Land Shop", "Town", true);

        Assert.assertTrue(catalog.consume(finiteId));
        Assert.assertTrue(catalog.consume(unlimitedId));
        Assert.assertFalse(catalog.get(finiteId).isAvailable());
        Assert.assertTrue(catalog.get(unlimitedId).isAvailable());
        Assert.assertEquals(catalog.getAvailableOffers().size(), 1);
    }

    @Test
    public void saveRetainsSourceAvailabilityAndFunctionalVariant() {
        ShopCatalog catalog = new ShopCatalog();
        ShopOfferId id = new ShopOfferId("poi/interior", 7, 11L, 3);
        catalog.observe(id, "Card|SET|[1]", "variant", null, "Shop", "Town", false);
        catalog.consume(id);

        SaveFileData saved = catalog.save();
        SaveFileData offer = saved.readSubData("offer_0");

        Assert.assertEquals(saved.readInt("count"), 1);
        Assert.assertEquals(offer.readString("pointOfInterestChangesKey"), "poi/interior");
        Assert.assertEquals(offer.readInt("shopObjectId"), 7);
        Assert.assertEquals(offer.readLong("shopSeed"), 11L);
        Assert.assertEquals(offer.readInt("rewardIndex"), 3);
        Assert.assertEquals(offer.readString("card"), "Card|SET|[1]");
        Assert.assertEquals(offer.readString("functionalVariant"), "variant");
        Assert.assertFalse(offer.readBool("available"));
    }
}
