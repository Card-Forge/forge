package forge.gui.download;

import forge.GuiDesktop;
import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.gamesimulationtests.util.CardDatabaseHelper;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.item.PaperToken;
import forge.localinstance.properties.ForgeConstants;
import forge.util.Localizer;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The token images that a selection of cards needs when its edition takes tokens from another one through
 * TokenFallbackCode: Magic 2014 Promos has none of its own and uses those of Magic 2014, and Born of the Gods adds
 * a few to those of Theros. Run on the real editions, because what the engine resolves for such an edition
 * (CardEdition.getTokenSet and getOtherSet) is what the download has to follow.
 */
@Test(groups = {"UnitTest"})
public class GuiDownloadFilteredCardImagesTest {
    private IGuiBase previousInterface;
    private StaticData data;

    @BeforeClass
    public void setUp() {
        previousInterface = GuiBase.getInterface();
        GuiBase.setInterface(new GuiDesktop()); // ForgeConstants needs it
        Localizer.getInstance().initialize("en-US", ForgeConstants.LANG_DIR);
        data = CardDatabaseHelper.createStaticData("GuiDownloadFilteredCardImagesTest", true);
    }

    @AfterClass
    public void tearDown() {
        GuiBase.setInterface(previousInterface);
    }

    private static boolean hasOwnEntries(final CardEdition edition) {
        return !edition.getTokens().isEmpty() || !edition.getOther().isEmpty();
    }

    /** The edition and the editions its TokenFallbackCode leads to, nearest first. */
    private List<CardEdition> chain(final CardEdition edition) {
        final List<CardEdition> chain = new ArrayList<>();
        CardEdition next = edition;
        while (next != null && !chain.contains(next)) {
            chain.add(next);
            next = next.getTokenFallbackCode() == null ? null : data.getCardEdition(next.getTokenFallbackCode());
        }
        return chain;
    }

    /** Adds what names the image of each numbered entry: an entry listed twice for two artists is one image. */
    private static void addImages(final Set<String> images, final String set, final Collection<CardEdition.EditionEntry> entries) {
        for (final CardEdition.EditionEntry entry : entries) {
            if (!StringUtils.isBlank(entry.collectorNumber())) {
                images.add(set + "|" + entry.name() + "|" + entry.collectorNumber());
            }
        }
    }

    private static int ownImages(final CardEdition edition) {
        final Set<String> images = new HashSet<>();
        addImages(images, edition.getCode(), edition.getTokens().values());
        addImages(images, edition.getCode(), edition.getOther().values());
        return images.size();
    }

    /**
     * How many images the cards of an edition need, as the engine finds them: the numbered tokens and extras it has
     * itself, and those of its fallbacks that it has no entry of that name for, with the back face of each
     * double-faced token.
     */
    private int resolvedImages(final CardEdition edition) {
        final Set<String> tokenNames = new HashSet<>();
        final Set<String> otherNames = new HashSet<>();
        for (final CardEdition source : chain(edition)) {
            tokenNames.addAll(source.getTokens().keySet());
            otherNames.addAll(source.getOther().keySet());
        }
        final Set<String> images = new HashSet<>();
        for (final String name : tokenNames) {
            final CardEdition holder = data.getCardEdition(edition.getTokenSet(name));
            final CardRules rules = data.getAllTokens().getRules().get(name);
            for (final CardEdition.EditionEntry entry : holder.getTokens().get(name)) {
                if (StringUtils.isBlank(entry.collectorNumber())) {
                    continue;
                }
                final String image = holder.getCode() + "|" + name + "|" + entry.collectorNumber();
                images.add(image);
                if (rules != null && new PaperToken(rules, holder, name, entry.collectorNumber(), entry.artistName()).hasBackFace()) {
                    images.add(image + "|back");
                }
            }
        }
        for (final String name : otherNames) {
            final CardEdition holder = data.getCardEdition(edition.getOtherSet(name));
            addImages(images, holder.getCode(), holder.getOther().get(name));
        }
        return images.size();
    }

    private static int total(final CardEdition... editions) {
        return GuiDownloadFilteredCardImages.countTokenImages(Arrays.asList(editions))[0];
    }

    /** Editions that have no tokens or extras of their own and a TokenFallbackCode: Magic 2014 Promos and the like. */
    private List<CardEdition> editionsOnlyUsingTheirFallback() {
        final List<CardEdition> found = new ArrayList<>();
        for (final CardEdition edition : data.getEditions()) {
            if (edition.getTokenFallbackCode() != null && !hasOwnEntries(edition)
                    && edition.getType() != CardEdition.Type.CUSTOM_SET
                    && data.getCardEdition(edition.getTokenFallbackCode()) != null) {
                found.add(edition);
            }
        }
        Assert.assertFalse(found.isEmpty(), "no edition left that only uses the tokens of another one: the test checks nothing");
        return found;
    }

    @Test
    public void editionWithoutTokensOfItsOwn_hasTheTokensOfItsFallback() {
        for (final CardEdition edition : editionsOnlyUsingTheirFallback()) {
            final CardEdition fallback = data.getCardEdition(edition.getTokenFallbackCode());

            Assert.assertTrue(GuiDownloadFilteredCardImages.hasTokenImages(edition), edition.getCode() + " has no token images");
            Assert.assertTrue(total(fallback) > 0, edition.getCode() + " falls back on " + fallback.getCode() + ", which has no tokens");
            Assert.assertEquals(total(edition), total(fallback), edition.getCode() + " is not given the tokens of " + fallback.getCode());
        }
    }

    @Test
    public void editionAndItsFallbackSelectedTogether_countEachImageOnce() {
        for (final CardEdition edition : editionsOnlyUsingTheirFallback()) {
            final CardEdition fallback = data.getCardEdition(edition.getTokenFallbackCode());

            Assert.assertEquals(total(edition, fallback), total(fallback), edition.getCode() + " and " + fallback.getCode() + " are counted twice");
            Assert.assertEquals(total(edition, edition), total(edition));
        }
    }

    @Test
    public void everyEdition_hasExactlyTheTokenImagesTheEngineResolves() {
        boolean addsToItsOwn = false;
        for (final CardEdition edition : data.getEditions()) {
            if (edition.getType() == CardEdition.Type.CUSTOM_SET) {
                continue;
            }
            final int resolved = resolvedImages(edition);
            // fewer is a token of a fallback that is missing, more is one that the edition has a token of its own for
            Assert.assertEquals(total(edition), resolved, edition.getCode() + " does not have the images that the engine resolves for it");
            addsToItsOwn |= edition.getTokenFallbackCode() != null && resolved > ownImages(edition);
        }
        Assert.assertTrue(addsToItsOwn, "no edition takes tokens from its fallback besides its own: the test checks nothing");
    }

    @Test
    public void editionWithoutAFallback_hasTokenImagesWhenItHasTokens() {
        for (final CardEdition edition : data.getEditions()) {
            if (edition.getTokenFallbackCode() == null) {
                Assert.assertEquals(GuiDownloadFilteredCardImages.hasTokenImages(edition),
                        edition.getType() != CardEdition.Type.CUSTOM_SET && hasOwnEntries(edition), edition.getCode());
            }
        }
    }

    @Test
    public void editionThatIsNoneOfThat_hasNoTokenImages() {
        Assert.assertFalse(GuiDownloadFilteredCardImages.hasTokenImages(null));
        Assert.assertEquals(GuiDownloadFilteredCardImages.countTokenImages(new ArrayList<>())[0], 0);
    }
}
