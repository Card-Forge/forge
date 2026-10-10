package forge.card;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.mockito.Mockito;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.ImageKeys;
import forge.gamesimulationtests.util.CardDatabaseHelper;
import forge.item.PaperCard;
import forge.model.FModel;

public class CardDbPreferredLanguageCardMockTestCase extends CardMockTestCase {

    private static final String NO_IMAGES_DB = "CardDbPreferredLanguage-NoImages";
    private static final String CACHED_FDN_DB = "CardDbPreferredLanguage-CachedFDN";

    private static final String SHIVAN_DRAGON = "Shivan Dragon";
    private static final String IN_SCOPE_ORIGINAL = "LEA";
    private static final String IN_SCOPE_LATEST = "FDN";
    private static final String OUT_OF_SCOPE = "GN3";
    private static final String PROMO_NEWER_THAN_OUT_OF_SCOPE = "P30T";
    private static final String COLLECTOR_OLDER_THAN_OUT_OF_SCOPE = "DRB";
    private static final CardDb.CardArtPreference LATEST_SCOPED = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;
    private static final CardDb.CardArtPreference ORIGINAL_SCOPED = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;
    private static final CardDb.CardArtPreference LATEST_ALL = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;

    private CardDb cardDb;

    @Override
    protected void initCardImageMocks() {
        imageKeysMock = Mockito.mockStatic(ImageKeys.class);
        imageKeysMock.when(() -> ImageKeys.hasImage(Mockito.any(PaperCard.class), Mockito.anyBoolean()))
                .thenReturn(false);
    }

    @Override
    protected void initializeStaticData() {
        useStaticData(CardDatabaseHelper.createStaticData(NO_IMAGES_DB, false));
    }

    @BeforeMethod
    public void setUpCardDb() {
        cardDb = FModel.getMagicDb().getCommonCards();
    }

    @AfterMethod(alwaysRun = true)
    public void clearPreferredLanguage() {
        if (cardDb != null)
            cardDb.setPreferredLanguageAvailability(null);
    }

    private void useCacheWithOnlyFdnImage() {
        imageKeysMock.when(() -> ImageKeys.hasImage(Mockito.any(PaperCard.class), Mockito.anyBoolean()))
                .thenAnswer(invocation -> IN_SCOPE_LATEST.equals(((PaperCard) invocation.getArgument(0)).getEdition()));
        useStaticData(CardDatabaseHelper.createStaticData(CACHED_FDN_DB, false));
        cardDb = FModel.getMagicDb().getCommonCards();
    }

    private CardEdition edition(String code) {
        CardEdition edition = FModel.getMagicDb().getEditions().get(code);
        assertNotNull(edition, "fixture edition not found: " + code);
        return edition;
    }

    private PaperCard printing(String code) {
        PaperCard printing = cardDb.getCard(SHIVAN_DRAGON, code);
        assertNotNull(printing, "fixture printing not found: " + SHIVAN_DRAGON + " in " + code);
        return printing;
    }

    private void assertInScope(String code) {
        assertTrue(LATEST_SCOPED.accept(edition(code)), "fixture edition no longer in scope: " + code);
    }

    private void assertWideningFixture(String code) {
        assertFalse(LATEST_SCOPED.accept(edition(code)), "fixture no longer exercises widening: " + code);
        assertFalse(CardDb.WIDENING_EXCLUDED_TYPES.contains(edition(code).getType()), "widening skips this edition type: " + code);
        assertNotEquals(printing(code).getRarity(), CardRarity.Special, "widening skips Special rarity: " + code);
    }

    private void assertExcludedFromWidening(String code) {
        assertFalse(LATEST_SCOPED.accept(edition(code)), "fixture no longer out of scope: " + code);
        assertTrue(CardDb.WIDENING_EXCLUDED_TYPES.contains(edition(code).getType()), "fixture edition type no longer excluded from widening: " + code);
        printing(code);
    }

    private void assertOlder(String older, String newer) {
        assertTrue(edition(older).getDate().before(edition(newer).getDate()), "fixture order changed: " + older + " must precede " + newer);
    }

    private void languageOnlyIn(String... codes) {
        Set<String> scryfallCodes = new HashSet<>();
        for (String code : codes)
            scryfallCodes.add(edition(code).getScryfallCode().toLowerCase());
        cardDb.setPreferredLanguageAvailability((scryfallCode, collectorNumber) -> scryfallCodes.contains(scryfallCode.toLowerCase()));
    }

    @Test
    public void testLanguagePrintInsideAcceptedEditionsWins() {
        assertInScope(IN_SCOPE_ORIGINAL);
        languageOnlyIn(IN_SCOPE_ORIGINAL);

        PaperCard card = cardDb.getCardFromEditions(SHIVAN_DRAGON, LATEST_SCOPED);

        assertNotNull(card);
        assertEquals(card.getEdition(), IN_SCOPE_ORIGINAL);
    }

    @Test
    public void testLanguagePrintOnlyOutsideAcceptedEditionsWidensSearch() {
        assertWideningFixture(OUT_OF_SCOPE);
        languageOnlyIn(OUT_OF_SCOPE);

        PaperCard card = cardDb.getCardFromEditions(SHIVAN_DRAGON, LATEST_SCOPED);

        assertNotNull(card);
        assertEquals(card.getEdition(), OUT_OF_SCOPE);
    }

    @Test
    public void testLanguageAbsentEverywhereFallsBackToDefaultBehaviour() {
        cardDb.setPreferredLanguageAvailability((scryfallCode, collectorNumber) -> false);
        PaperCard withPredicate = cardDb.getCardFromEditions(SHIVAN_DRAGON, LATEST_SCOPED);

        cardDb.setPreferredLanguageAvailability(null);
        PaperCard withoutPredicate = cardDb.getCardFromEditions(SHIVAN_DRAGON, LATEST_SCOPED);

        assertNotNull(withPredicate);
        assertEquals(withPredicate.getEdition(), withoutPredicate.getEdition());
    }

    @Test
    public void testWideningSkipsNewerPromoAndTakesRegularProduct() {
        assertExcludedFromWidening(PROMO_NEWER_THAN_OUT_OF_SCOPE);
        assertWideningFixture(OUT_OF_SCOPE);
        assertOlder(OUT_OF_SCOPE, PROMO_NEWER_THAN_OUT_OF_SCOPE);
        languageOnlyIn(PROMO_NEWER_THAN_OUT_OF_SCOPE, OUT_OF_SCOPE);

        PaperCard card = cardDb.getCardFromEditions(SHIVAN_DRAGON, LATEST_SCOPED);

        assertNotNull(card);
        assertEquals(card.getEdition(), OUT_OF_SCOPE);
    }

    @Test
    public void testWideningSkipsOlderCollectorEditionAndTakesRegularProduct() {
        assertExcludedFromWidening(COLLECTOR_OLDER_THAN_OUT_OF_SCOPE);
        assertWideningFixture(OUT_OF_SCOPE);
        assertOlder(COLLECTOR_OLDER_THAN_OUT_OF_SCOPE, OUT_OF_SCOPE);
        languageOnlyIn(COLLECTOR_OLDER_THAN_OUT_OF_SCOPE, OUT_OF_SCOPE);

        PaperCard card = cardDb.getCardFromEditions(SHIVAN_DRAGON, ORIGINAL_SCOPED);

        assertNotNull(card);
        assertEquals(card.getEdition(), OUT_OF_SCOPE);
    }

    @Test
    public void testAcceptedLanguagePrintWinsOverCachedPrintingInAnotherEdition() {
        useCacheWithOnlyFdnImage();
        assertInScope(IN_SCOPE_ORIGINAL);
        assertInScope(IN_SCOPE_LATEST);
        languageOnlyIn(IN_SCOPE_ORIGINAL);

        PaperCard card = cardDb.getCardFromEditions(SHIVAN_DRAGON, LATEST_SCOPED);

        assertNotNull(card);
        assertEquals(card.getEdition(), IN_SCOPE_ORIGINAL);
    }

    @Test
    public void testCachedLanguagePrintWinsOverEarlierUncachedOne() {
        useCacheWithOnlyFdnImage();
        assertInScope(IN_SCOPE_ORIGINAL);
        assertInScope(IN_SCOPE_LATEST);
        languageOnlyIn(IN_SCOPE_ORIGINAL, IN_SCOPE_LATEST);

        PaperCard card = cardDb.getCardFromEditions(SHIVAN_DRAGON, ORIGINAL_SCOPED);

        assertNotNull(card);
        assertEquals(card.getEdition(), IN_SCOPE_LATEST);
    }

    @Test
    public void testWideningSkippedWhenAnAcceptedPrintingIsCached() {
        useCacheWithOnlyFdnImage();
        assertInScope(IN_SCOPE_LATEST);
        assertWideningFixture(OUT_OF_SCOPE);
        languageOnlyIn(OUT_OF_SCOPE);

        PaperCard card = cardDb.getCardFromEditions(SHIVAN_DRAGON, LATEST_SCOPED);

        assertNotNull(card);
        assertEquals(card.getEdition(), IN_SCOPE_LATEST);
    }

    @Test
    public void testWithoutScopeFilterLanguagePrintIsFoundWithoutWidening() {
        assertTrue(LATEST_ALL.accept(edition(OUT_OF_SCOPE)), "all-editions preference no longer accepts: " + OUT_OF_SCOPE);
        assertFalse(LATEST_SCOPED.accept(edition(OUT_OF_SCOPE)), "fixture no longer out of the Core/Expansion/Reprint scope: " + OUT_OF_SCOPE);
        languageOnlyIn(OUT_OF_SCOPE);

        PaperCard card = cardDb.getCardFromEditions(SHIVAN_DRAGON, LATEST_ALL);

        assertNotNull(card);
        assertEquals(card.getEdition(), OUT_OF_SCOPE);
    }

    @Test
    public void testWithoutScopeFilterPromoLanguagePrintIsNotExcluded() {
        assertTrue(LATEST_ALL.accept(edition(PROMO_NEWER_THAN_OUT_OF_SCOPE)), "all-editions preference no longer accepts: " + PROMO_NEWER_THAN_OUT_OF_SCOPE);
        assertExcludedFromWidening(PROMO_NEWER_THAN_OUT_OF_SCOPE);
        languageOnlyIn(PROMO_NEWER_THAN_OUT_OF_SCOPE);

        PaperCard card = cardDb.getCardFromEditions(SHIVAN_DRAGON, LATEST_ALL);

        assertNotNull(card);
        assertEquals(card.getEdition(), PROMO_NEWER_THAN_OUT_OF_SCOPE);
    }
}
