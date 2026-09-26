package forge.card;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.function.BiPredicate;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.item.PaperCard;

/**
 * Covers CardDb's language-preference selection in tryToGetCardFromEditions, added
 * alongside CardArtPreference. Reuses the Shivan Dragon fixtures from
 * {@link CardDbCardMockTestCase} rather than inventing a synthetic card, since the real
 * printings already give us one edition inside "Core/Expansion/Reprint Only" scope
 * (LEA) and one outside it (whatever the dynamically-computed latest printing is).
 *
 * preferredLanguageAvailability is a fake BiPredicate here, not the real CDN index -
 * these tests only check that CardDb reacts to it correctly, not that the index itself
 * is accurate.
 */
public class CardDbPreferredLanguageCardMockTestCase extends CardDbCardMockTestCase {

    /**
     * setPreferredLanguageAvailability triggers a reIndex() and StaticData/CardDb is
     * shared process-wide across test classes (see CardMockTestCase's class javadoc), so
     * a predicate left behind here would leak into unrelated tests that run later in the
     * same JVM.
     */
    @AfterMethod(alwaysRun = true)
    public void clearPreferredLanguage() {
        this.cardDb.setPreferredLanguageAvailability(null);
    }

    private String scryfallCodeOf(String editionCode) {
        CardEdition edition = StaticData.instance().getEditions().get(editionCode);
        assertNotNull(edition, "Test fixture edition not found: " + editionCode);
        return edition.getScryfallCode();
    }

    @Test
    public void testLanguagePrintInsideAcceptedEditionsWins() {
        // LEA (Alpha) is a CORE edition, so it's always in scope for
        // LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY. Without a language preference this
        // frame would return latestArtShivanDragonEditionNoPromo (FDN) instead - pretending
        // only LEA has the language forces CardDb to pick it over the newer printing,
        // proving the language step actually overrides the plain date-based choice
        // rather than just agreeing with it by coincidence.
        String leaScryfallCode = scryfallCodeOf(originalArtShivanDragonEdition);
        BiPredicate<String, String> onlyLea = (scryfallCode, collectorNumber) -> leaScryfallCode.equalsIgnoreCase(scryfallCode);
        this.cardDb.setPreferredLanguageAvailability(onlyLea);

        PaperCard card = this.cardDb.getCardFromEditions(cardNameShivanDragon,
                CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        assertNotNull(card);
        assertEquals(card.getName(), cardNameShivanDragon);
        assertEquals(card.getEdition(), originalArtShivanDragonEdition);
    }

    @Test
    public void testLanguagePrintOnlyOutsideAcceptedEditionsWidensSearch() {
        // latestArtShivanDragonEdition is the true latest printing regardless of set
        // type, which today is a promo/non-core edition excluded by
        // LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY (see the field's own comment: it has to
        // stay whatever the newest printing actually is, since that's the whole point of
        // this scenario). Pretending only that edition has the language means no
        // accepted-scope candidate matches, so this only passes if the widening step
        // actually reaches outside acceptedEditions - the fix for the Fellwar
        // Stone/Arcane Signet case.
        String latestScryfallCode = scryfallCodeOf(latestArtShivanDragonEdition);
        BiPredicate<String, String> onlyLatest = (scryfallCode, collectorNumber) -> latestScryfallCode.equalsIgnoreCase(scryfallCode);
        this.cardDb.setPreferredLanguageAvailability(onlyLatest);

        PaperCard card = this.cardDb.getCardFromEditions(cardNameShivanDragon,
                CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        assertNotNull(card);
        assertEquals(card.getName(), cardNameShivanDragon);
        assertEquals(card.getEdition(), latestArtShivanDragonEdition);
    }

    @Test
    public void testLanguageAbsentEverywhereFallsBackToDefaultBehaviour() {
        // No candidate ever matches, so this must behave exactly as if the language
        // preference were never set - confirms the feature is inert rather than merely
        // "usually harmless" when nothing in the card pool has the language.
        BiPredicate<String, String> neverMatches = (scryfallCode, collectorNumber) -> false;
        this.cardDb.setPreferredLanguageAvailability(neverMatches);

        PaperCard withPredicate = this.cardDb.getCardFromEditions(cardNameShivanDragon,
                CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        this.cardDb.setPreferredLanguageAvailability(null);
        PaperCard withoutPredicate = this.cardDb.getCardFromEditions(cardNameShivanDragon,
                CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        assertNotNull(withPredicate);
        assertEquals(withPredicate.getEdition(), withoutPredicate.getEdition());
        assertEquals(withPredicate.getEdition(), latestArtShivanDragonEditionNoPromo);
    }
}