package forge.card;

import com.google.common.base.Predicate;
import forge.StaticData;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.model.FModel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

import static org.testng.Assert.*;

public class CardDbTestCase extends ForgeCardMockTestCase {

    protected LegacyCardDb legacyCardDb;
    protected CardDb cardDb;

    // Shivan Dragon is great as it has multiple arts from re-prints
    protected final String cardNameShivanDragon = "Shivan Dragon";
    protected final String editionShivanDragon = "2ED";
    protected final String collNrShivanDragon = "175";

    // Test Foil case - first foil ever printed!
    protected final String cardNameFoilLightningDragon = "Lightning Dragon+";
    protected final String cardNameLightningDragon = "Lightning Dragon";
    protected final String editionLightningDragon = "PUSG";
    protected final String collNrLightningDragon = "202";

    // Get a card with multiple arts
    protected final String cardNameHymnToTourach = "Hymn to Tourach";  // good 'ol hymn w/ four different art
    protected final String[] collectorNumbersHymnToTourach = {"38a", "38b", "38c", "38d"};
    protected final String editionHymnToTourach = "FEM";

    //Get Card From Editions Test fixtures
    protected final String originalArtShivanDragonEdition = "LEA";
    protected final String latestArtShivanDragonEdition = "M20";

    protected final String originalArtLightningDragonEdition = "USG";
    protected final String originalArtLightningDragonEditionNoPromo = "USG";

    protected final String latestArtLightningDragonEdition = "VMA";
    protected final String latestArtLightningDragonEditionNoPromo = "USG";

    protected final String latestArtHymnToTourachEdition = "EMA";
    protected final String latestArtHymnToTourachEditionNoPromo = "EMA";
    protected final String originalArtHymnToTourachEdition = "FEM";
    protected final String originalArtHymnToTourachEditionNoPromo = "FEM";

    // Test Dates and Editions
    protected final String releasedBeforeFromTheVaultDate = "2008-10-01";

    protected final String latestArtShivanDragonEditionReleasedBeforeFromTheVault = "DRB";
    protected final String latestArtShivanDragonEditionReleasedBeforeFromTheVaultNoPromo = "10E";
    protected final String latestArtLightningDragonEditionReleasedBeforeFromTheVault = "MBP";
    protected final String latestArtLightningDragonEditionReleasedBeforeFromTheVaultNoPromo = "USG";

    protected final String releasedAfterTenthEditionDate = "2007-07-13";

    protected final String originalArtShivanDragonEditionReleasedAfterTenthEdition = "DRB";
    protected final String originalArtShivanDragonEditionReleasedAfterTenthEditionNoPromo = "M10";
    protected final String originalArtLightningDragonEditionReleasedAfterTenthEdition = "VMA";
    // NOTE: too strict, therefore MBP should still be returned.
    protected final String originalArtLightningDragonEditionReleasedAfterTenthEditionNoPromo = "VMA";

    protected final String releasedBeforeEternalMastersDate = "2015-01-01";
    protected final String latestArtHymnToTourachEditionReleasedBeforeEternalMasters = "VMA";
    protected final String latestArtHymnToTourachEditionReleasedBeforeEternalMastersNoPromo = "FEM";

    protected final String releasedAfterAnthologiesDate = "1998-11-01";
    protected final String originalArtHymnToTourachEditionReleasedAfterAnthologies = "PRM";
    // NOTE: This is the only edition (reprint) matching art preference after Anthologies
    protected final String originalArtHymnToTourachEditionReleasedAfterAnthologiesNoPromo = "EMA";

    // Get a card that has lots of editions so that we can test fetching for specific editions and print dates
    protected final String cardNameCounterspell = "Counterspell";
    protected final String[] editionsCounterspell = {"3ED", "4ED", "ICE", "5ED", "TMP", "S99", "MMQ",
                                                     "BTD", "BRB", "A25", "MH2", "SLD"};

    protected final String counterspellReleasedBeforeMagicOnlinePromosDate = "2018-03-15";
    protected final String[] counterspellLatestArtsReleasedBeforeMagicOnlinePromos = {"MPS_AKH", "EMA"};

    protected final String counterspellReleasedBeforeEternalMastersDate = "2016-06-10";
    protected final String[] counterspellLatestArtReleasedBeforeEternalMasters = {"TPR", "7ED"};
    protected final String[] counterspellOriginalArtReleasedAfterEternalMasters = {"MPS_AKH", "A25"};

    protected final String counterspellReleasedAfterBattleRoyaleDate = "1999-11-12";
    protected final String[] counterspellOriginalArtReleasedAfterBattleRoyale = {"G00", "7ED"};

    // test for date restrictions - boundary cases
    protected final String alphaEditionReleaseDate = "1993-08-05";


    @BeforeMethod
    public void setup(){
        StaticData data = FModel.getMagicDb();
        this.cardDb = data.getCommonCards();
        this.legacyCardDb = new LegacyCardDb(data.getCommonCards().getAllCards(), data.getEditions());
    }

    /*
     * TEST FOR GET ALL CARDS
     */

    @Test
    public void testGetAllCardsWithName(){
        List<PaperCard> allCounterSpellPrints = this.cardDb.getAllCards(this.cardNameCounterspell);
        assertNotNull(allCounterSpellPrints);
        for (PaperCard card : allCounterSpellPrints)
            assertEquals(card.getName(), this.cardNameCounterspell);
    }

    @Test
    public void testGetAllCardsThatWerePrintedInSets(){
        List<String> allowedSets = new ArrayList<>();
        allowedSets.add(this.latestArtShivanDragonEdition);
        Predicate<PaperCard> wasPrinted = (Predicate<PaperCard>) this.cardDb.wasPrintedInSets(allowedSets);
        List<PaperCard> allCardsInSet = this.cardDb.getAllCards(wasPrinted);
        assertNotNull(allCardsInSet);
    }

    @Test void testGetAllCardsOfaGivenNameAndPrintedInSets(){
        List<String> allowedSets = new ArrayList<>(Arrays.asList(this.editionsCounterspell));
        Predicate<PaperCard> printedInSets = (Predicate<PaperCard>) this.cardDb.wasPrintedInSets(allowedSets);
        List<PaperCard> allCounterSpellsInSets = this.cardDb.getAllCards(this.cardNameCounterspell, printedInSets);
        assertNotNull(allCounterSpellsInSets);
        assertTrue(allCounterSpellsInSets.size() > 0);
        assertTrue(allCounterSpellsInSets.size() > 1);
        for (PaperCard card : allCounterSpellsInSets) {
            assertEquals(card.getName(), this.cardNameCounterspell);
        }
    }

    @Test
    public void testGetAllCardsLegalInSets(){
        List<String> allowedSets = new ArrayList<>();
        allowedSets.add(this.latestArtShivanDragonEdition);
        Predicate<PaperCard> legalInSets = (Predicate<PaperCard>) this.cardDb.isLegal(allowedSets);
        List<PaperCard> allCardsInSet = this.cardDb.getAllCards(legalInSets);
        assertNotNull(allCardsInSet);
        for (PaperCard card : allCardsInSet)
            assertEquals(card.getEdition(), this.latestArtShivanDragonEdition);
    }

    @Test void testGetAllCardsOfaGivenNameAndLegalInSets(){
        List<String> allowedSets = new ArrayList<>(Arrays.asList(this.editionsCounterspell));
        Predicate<PaperCard> legalInSets = (Predicate<PaperCard>) this.cardDb.isLegal(allowedSets);
        List<PaperCard> allCounterSpellsInSets = this.cardDb.getAllCards(this.cardNameCounterspell, legalInSets);
        assertNotNull(allCounterSpellsInSets);
        assertTrue(allCounterSpellsInSets.size() > 0);
        assertTrue(allCounterSpellsInSets.size() > 1);
        for (PaperCard card : allCounterSpellsInSets) {
            assertEquals(card.getName(), this.cardNameCounterspell);
            assertTrue(allowedSets.contains(card.getEdition()));
        }
    }

    /*
     * TEST FOR CARD RETRIEVAL METHODS
     */

    @Test
    public void testGetCardByName() {
        PaperCard legacyCard = this.legacyCardDb.getCard(cardNameShivanDragon);
        PaperCard card = this.cardDb.getCard(cardNameShivanDragon);
        assertNotNull(card);
        assertEquals(card.getName(), cardNameShivanDragon);
        assertNotNull(legacyCard);
        assertEquals(legacyCard.getName(), cardNameShivanDragon);

        //Foil card
        PaperCard legacyFoilCard = this.legacyCardDb.getCard(cardNameFoilLightningDragon);
        PaperCard foildCard = this.cardDb.getCard(cardNameFoilLightningDragon);

        assertNotNull(foildCard);
        assertEquals(foildCard.getName(), cardNameLightningDragon);
        assertTrue(foildCard.isFoil());

        assertNotNull(legacyFoilCard);
        assertEquals(legacyFoilCard.getName(), cardNameLightningDragon);
        assertTrue(legacyFoilCard.isFoil());
    }

    @Test
    public void testGetCardByNameAndSet() {
        PaperCard legacyCard = this.legacyCardDb.getCard(cardNameShivanDragon, editionShivanDragon);
        PaperCard card = this.cardDb.getCard(cardNameShivanDragon, editionShivanDragon);

        assertEquals(card.getName(), cardNameShivanDragon);
        assertEquals(card.getEdition(), editionShivanDragon);
        assertEquals(card.getCollectorNumber(), collNrShivanDragon);

        assertEquals(legacyCard.getName(), cardNameShivanDragon);
        assertEquals(legacyCard.getEdition(), editionShivanDragon);
        assertEquals(legacyCard.getCollectorNumber(), collNrShivanDragon);

        assertEquals(card, legacyCard);

        //Foil card
        PaperCard legacyFoilCard = this.legacyCardDb.getCard(cardNameFoilLightningDragon, editionLightningDragon);
        PaperCard foildCard = this.cardDb.getCard(cardNameFoilLightningDragon, editionLightningDragon);

        assertEquals(foildCard.getName(), cardNameLightningDragon);
        assertEquals(foildCard.getEdition(), editionLightningDragon);
        assertEquals(foildCard.getCollectorNumber(), collNrLightningDragon);
        assertTrue(foildCard.isFoil());

        assertEquals(legacyFoilCard.getName(), cardNameLightningDragon);
        assertEquals(legacyFoilCard.getEdition(), editionLightningDragon);
        assertTrue(legacyFoilCard.isFoil());
        assertEquals(legacyFoilCard.getCollectorNumber(), collNrLightningDragon);

        assertEquals(foildCard, legacyFoilCard);
    }

    @Test
    public void testGetCardByNameSetAndArtIndex() {
        for (int i = 0; i < 4; i++) {
            int artIdx = i + 1;
            PaperCard legacyCard = this.legacyCardDb.getCard(cardNameHymnToTourach, editionHymnToTourach, artIdx);
            PaperCard card = this.cardDb.getCard(cardNameHymnToTourach, editionHymnToTourach, artIdx);

            assertEquals(card.getName(), cardNameHymnToTourach);
            assertEquals(card.getEdition(), editionHymnToTourach);
            assertEquals(card.getCollectorNumber(), collectorNumbersHymnToTourach[i]);
            assertEquals(card.getArtIndex(), artIdx);

            assertEquals(legacyCard.getName(), cardNameHymnToTourach);
            assertEquals(legacyCard.getEdition(), editionHymnToTourach);
            assertEquals(legacyCard.getCollectorNumber(), collectorNumbersHymnToTourach[i]);
            assertEquals(legacyCard.getArtIndex(), artIdx);

            assertEquals(card, legacyCard);
        }
    }

    @Test
    public void testNewMethodGetCardByNameSetAndCollectorNumber() {
        PaperCard card = this.cardDb.getCard(cardNameShivanDragon, editionShivanDragon, collNrShivanDragon);
        assertEquals(card.getName(), cardNameShivanDragon);
        assertEquals(card.getEdition(), editionShivanDragon);
        assertEquals(card.getCollectorNumber(), collNrShivanDragon);

        //Foil card
        PaperCard foildCard = this.cardDb.getCard(cardNameFoilLightningDragon, editionLightningDragon, collNrLightningDragon);
        assertEquals(foildCard.getName(), cardNameLightningDragon);
        assertEquals(foildCard.getEdition(), editionLightningDragon);
        assertEquals(foildCard.getCollectorNumber(), collNrLightningDragon);
        assertTrue(foildCard.isFoil());

        // MultiArt Card
        for (int i = 0; i < 4; i++) {
            card = this.cardDb.getCard(cardNameHymnToTourach, editionHymnToTourach, collectorNumbersHymnToTourach[i]);
            assertEquals(card.getName(), cardNameHymnToTourach);
            assertEquals(card.getEdition(), editionHymnToTourach);
            assertEquals(card.getCollectorNumber(), collectorNumbersHymnToTourach[i]);
            assertEquals(card.getArtIndex(), i + 1);
        }
    }

    @Test
    public void testGetCardByNameSetArtIndexAndCollectorNumber() {
        // MultiArt Card
        PaperCard card;
        for (int i = 0; i < 4; i++) {
            int artIndex = i + 1;
            card = this.cardDb.getCard(cardNameHymnToTourach, editionHymnToTourach, artIndex, collectorNumbersHymnToTourach[i]);
            assertEquals(card.getName(), cardNameHymnToTourach);
            assertEquals(card.getEdition(), editionHymnToTourach);
            assertEquals(card.getCollectorNumber(), collectorNumbersHymnToTourach[i]);
            assertEquals(card.getArtIndex(), artIndex);
        }
    }

    @Test
    public void testNullIsReturnedWithWrongInfo() {
        String wrongEditionCode = "M11";
        PaperCard legacyCard = this.legacyCardDb.getCard(cardNameShivanDragon, wrongEditionCode);
        assertNull(legacyCard);
        PaperCard card = this.cardDb.getCard(cardNameShivanDragon, wrongEditionCode);
        assertNull(card);
        // Wrong Art Index
        legacyCard = this.legacyCardDb.getCard(cardNameShivanDragon, editionShivanDragon, 3);
        assertNull(legacyCard);
        card = this.cardDb.getCard(cardNameShivanDragon, editionShivanDragon, 3);
        assertNull(card);
        // Wrong collector number
        card = this.cardDb.getCard(cardNameShivanDragon, editionShivanDragon, "wrongCN");
        assertNull(card);
        // wrong artIndex or collector number in getCard Full Info
        card = this.cardDb.getCard(cardNameShivanDragon, editionShivanDragon, 3, collNrShivanDragon);
        assertNull(card);
        card = this.cardDb.getCard(cardNameShivanDragon, editionShivanDragon, 1, "wrongCN");
        assertNull(card);
    }

    @Test
    public void testNewGetCardFromSet() {
        CardEdition cardEdition = FModel.getMagicDb().getEditions().get(editionShivanDragon);
        PaperCard card = this.cardDb.getCardFromSet(cardNameShivanDragon, cardEdition, false);
        assertNotNull(card);
        assertEquals(card.getName(), cardNameShivanDragon);
        assertEquals(card.getEdition(), editionShivanDragon);
        assertFalse(card.isFoil());

        card = this.cardDb.getCardFromSet(cardNameShivanDragon, cardEdition, true);
        assertNotNull(card);
        assertTrue(card.isFoil());
    }

    @Test
    public void testNewGetCardFromSetWithCardNameFoilMarker() {
        CardEdition cardEdition = FModel.getMagicDb().getEditions().get(editionLightningDragon);
        PaperCard foilCard = this.cardDb.getCardFromSet(cardNameFoilLightningDragon,
                cardEdition, false);
        assertNotNull(foilCard);
        assertTrue(foilCard.isFoil());
    }

    @Test
    public void testNewGetCardFromSetWithCollectorNumber() {
        CardEdition cardEdition = FModel.getMagicDb().getEditions().get(editionShivanDragon);
        PaperCard card = this.cardDb.getCardFromSet(cardNameShivanDragon, cardEdition, collNrShivanDragon, false);
        assertNotNull(card);
        assertEquals(card.getName(), cardNameShivanDragon);
        assertEquals(card.getEdition(), editionShivanDragon);
        assertFalse(card.isFoil());

        card = this.cardDb.getCardFromSet(cardNameShivanDragon, cardEdition, collNrShivanDragon, true);
        assertNotNull(card);
        assertTrue(card.isFoil());
    }

    @Test
    public void testNewGetCardFromSetWithArtIndex() {
        CardEdition ce = FModel.getMagicDb().getEditions().get(editionHymnToTourach);
        PaperCard card = this.cardDb.getCardFromSet(cardNameHymnToTourach, ce, 2, false);
        assertNotNull(card);
        assertEquals(card.getName(), cardNameHymnToTourach);
        assertEquals(card.getEdition(), editionHymnToTourach);
        assertEquals(card.getCollectorNumber(), collectorNumbersHymnToTourach[1]);
        assertEquals(card.getArtIndex(), 2);
        assertFalse(card.isFoil());

        PaperCard foilCard = this.cardDb.getCardFromSet(cardNameHymnToTourach, ce, 2, true);
        assertNotNull(foilCard);
        assertTrue(foilCard.isFoil());
        assertEquals(card.getArtIndex(), foilCard.getArtIndex());
        assertEquals(card.getName(), foilCard.getName());
        assertEquals(card.getCollectorNumber(), foilCard.getCollectorNumber());
        assertEquals(card.getEdition(), foilCard.getEdition());
    }

    @Test
    public void testNewGetCardFromSetWithAllInfo() {
        CardEdition ce = FModel.getMagicDb().getEditions().get(editionHymnToTourach);
        PaperCard card = this.cardDb.getCardFromSet(cardNameHymnToTourach, ce, 2,
                collectorNumbersHymnToTourach[1], false);
        assertNotNull(card);
        assertEquals(card.getName(), cardNameHymnToTourach);
        assertEquals(card.getEdition(), editionHymnToTourach);
        assertEquals(card.getCollectorNumber(), collectorNumbersHymnToTourach[1]);
        assertEquals(card.getArtIndex(), 2);
        assertFalse(card.isFoil());

        PaperCard foilCard = this.cardDb.getCardFromSet(cardNameHymnToTourach, ce, 2,
                collectorNumbersHymnToTourach[1], true);
        assertNotNull(foilCard);
        assertTrue(foilCard.isFoil());
        assertEquals(card.getArtIndex(), foilCard.getArtIndex());
        assertEquals(card.getName(), foilCard.getName());
        assertEquals(card.getCollectorNumber(), foilCard.getCollectorNumber());
        assertEquals(card.getEdition(), foilCard.getEdition());
    }

    @Test
    public void testGetCardFromEditionsWithCardNameAndCardArtPreference() {
        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference frame = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;

        PaperCard sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEdition);

        PaperCard ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        assertEquals(ldCard.getName(), cardNameLightningDragon);
        assertEquals(ldCard.getEdition(), latestArtLightningDragonEdition);

        // foiled card request
        PaperCard ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestArtLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());

        PaperCard httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEdition);

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        frame = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEdition);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        assertEquals(ldCard.getName(), cardNameLightningDragon);
        assertEquals(ldCard.getEdition(), latestArtLightningDragonEditionNoPromo);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestArtLightningDragonEditionNoPromo);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionNoPromo);

        /* --------------
            Old Print
           -------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEdition);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        assertEquals(ldCard.getName(), cardNameLightningDragon);
        assertEquals(ldCard.getEdition(), originalArtLightningDragonEdition);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEdition);

        /* --------------------
            Old Print No Promo
         ----------------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEdition);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        assertEquals(ldCard.getName(), cardNameLightningDragon);
        assertEquals(ldCard.getEdition(), originalArtLightningDragonEditionNoPromo);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEditionNoPromo);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEditionNoPromo);
    }

    @Test
    public void testGetCardFromEditionsWithCardNameAndCardArtPreferenceComparedWithLegacy() {
        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference frame = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;
        LegacyCardDb.LegacySetPreference setPreference = LegacyCardDb.LegacySetPreference.Latest;

        PaperCard sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        PaperCard sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, setPreference);
        assertEquals(sdCard, sdCardLegacy);

        PaperCard ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        PaperCard ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, setPreference);
        assertEquals(ldCard, ldCardLegacy);

        PaperCard httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        PaperCard httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, setPreference);
        assertEquals(httCard, httCardLegacy);

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        frame = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        setPreference = LegacyCardDb.LegacySetPreference.LatestCoreExp;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, setPreference);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, setPreference);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, setPreference);
        assertEquals(httCard, httCardLegacy);

        /* --------------
            Old Print
           -------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;
        setPreference = LegacyCardDb.LegacySetPreference.Earliest;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, setPreference);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, setPreference);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, setPreference);
        assertEquals(httCard, httCardLegacy);

        /* --------------------
            Old Print No Promo
         ----------------------*/
        frame = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        setPreference = LegacyCardDb.LegacySetPreference.LatestCoreExp;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, setPreference);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, setPreference);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, setPreference);
        assertEquals(httCard, httCardLegacy);
    }

    @Test
    public void testGetCardFromEditionsWithCardNameAndFramePreferenceWithArtIndex() {
        /* NOTE:
         testing case of errors here - will do in a separate test.
         */

        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference frame = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;

        PaperCard httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, 1);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEdition);
        assertEquals(httCard.getArtIndex(), 1);

        // foil card
        PaperCard ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestArtLightningDragonEdition);
        assertEquals(ldFoilCard.getArtIndex(), 1);
        assertTrue(ldFoilCard.isFoil());

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        frame = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, 1);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getArtIndex(), 1);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionNoPromo);

        // foil card
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestArtLightningDragonEditionNoPromo);
        assertEquals(ldFoilCard.getArtIndex(), 1);
        assertTrue(ldFoilCard.isFoil());

        /* --------------
            Old Print
           -------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, artIdx);
            assertEquals(httCard.getName(), cardNameHymnToTourach);
            assertEquals(httCard.getEdition(), originalArtHymnToTourachEdition);
            assertEquals(httCard.getArtIndex(), artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
        }

        // foil card
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEdition);
        assertEquals(ldFoilCard.getArtIndex(), 1);
        assertTrue(ldFoilCard.isFoil());

        /* --------------------
            Old Print No Promo
         ----------------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, artIdx);
            assertEquals(httCard.getName(), cardNameHymnToTourach);
            assertEquals(httCard.getEdition(), originalArtHymnToTourachEdition);
            assertEquals(httCard.getArtIndex(), artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
        }

        // foil card
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEditionNoPromo);
        assertEquals(ldFoilCard.getArtIndex(), 1);
        assertTrue(ldFoilCard.isFoil());
    }

    @Test
    public void testGetCardFromEditionsWrongInputReturnsNull() {
        PaperCard nullCard;
        PaperCard shivanNotExistingDragon;
        for (CardDb.CardArtPreference preference : CardDb.CardArtPreference.values()) {
            nullCard = this.cardDb.getCardFromEditions("ImaginaryMagicCard", preference);
            assertNull(nullCard);

            nullCard = this.cardDb.getCardFromEditions(null, preference);
            assertNull(nullCard);

            shivanNotExistingDragon = this.cardDb.getCardFromEditions(cardNameShivanDragon, preference, 2);
            assertNull(shivanNotExistingDragon);

            nullCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, preference, 5);
            assertNull(nullCard);
        }

        // Passing null preference
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        PaperCard httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, null, null);
        assertNotNull(httCard);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEdition);

        // Changing default value for default card art preference
        this.cardDb.setCardArtPreference(false, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, null, null);
        assertNotNull(httCard);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEdition);
        // restore default
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

    @Test
    public void testGetCardFromEditionsUsingDefaultCardArtPreference(){
        // Test default value first
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        PaperCard shivanDragonCard = this.cardDb.getCardFromEditions(cardNameShivanDragon);
        assertEquals(shivanDragonCard.getEdition(), latestArtShivanDragonEdition);

        // Try changing the policy
        this.cardDb.setCardArtPreference(false, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
        shivanDragonCard = this.cardDb.getCardFromEditions(cardNameShivanDragon);
        assertEquals(shivanDragonCard.getEdition(), originalArtShivanDragonEdition);
        // restore default
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

    // == Specialised Card Art Preference Retrieval and Release Date Constraint (BEFORE)
    @Test
    public void testGetCardFromEditionsWithCardNameAndCardArtPreferenceReleasedBeforeDate() {
        // Set Reference Dates
        Date fromTheVaultReleaseDate = null;
        Date eternalMastersReleaseDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            fromTheVaultReleaseDate = format.parse(releasedBeforeFromTheVaultDate);
            eternalMastersReleaseDate = format.parse(releasedBeforeEternalMastersDate);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference artPreference = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;

        PaperCard sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, fromTheVaultReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEditionReleasedBeforeFromTheVault);

        // foiled card request
        PaperCard ldFoilCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameFoilLightningDragon, artPreference, fromTheVaultReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestArtLightningDragonEditionReleasedBeforeFromTheVault);
        assertTrue(ldFoilCard.isFoil());

        PaperCard httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, eternalMastersReleaseDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionReleasedBeforeEternalMasters);

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        artPreference = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, fromTheVaultReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEditionReleasedBeforeFromTheVaultNoPromo);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameFoilLightningDragon, artPreference, fromTheVaultReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestArtLightningDragonEditionReleasedBeforeFromTheVaultNoPromo);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, eternalMastersReleaseDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionReleasedBeforeEternalMastersNoPromo);

         /* --------------
            Old Print
           -------------*/
        artPreference = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;

        sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, fromTheVaultReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEdition);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameFoilLightningDragon, artPreference, fromTheVaultReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, eternalMastersReleaseDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEdition);

        /* --------------------
            Old Print No Promo
         ----------------------*/
        artPreference = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, fromTheVaultReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEdition);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameFoilLightningDragon, artPreference, fromTheVaultReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEditionNoPromo);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, eternalMastersReleaseDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEditionNoPromo);
    }

    @Test
    public void testGetCardFromEditionsWithCardNameAndCardArtPreferenceReleasedBeforeDateComparedWithLegacy() {
        // Set Reference Dates
        Date sdReleaseDate = null;
        Date httReleaseDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            sdReleaseDate = format.parse(releasedBeforeFromTheVaultDate);
            httReleaseDate = format.parse(releasedBeforeEternalMastersDate);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference artPreference = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;
        LegacyCardDb.LegacySetPreference setPref = LegacyCardDb.LegacySetPreference.Latest;

        PaperCard sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, sdReleaseDate);
        PaperCard sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEditionReleasedBeforeFromTheVault);
        assertEquals(sdCard, sdCardLegacy);

        PaperCard ldCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameLightningDragon, artPreference, sdReleaseDate);
        PaperCard ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), latestArtLightningDragonEditionReleasedBeforeFromTheVault);
        assertEquals(ldCard, ldCardLegacy);

        PaperCard httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, httReleaseDate);
        PaperCard httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionReleasedBeforeEternalMasters);
        assertEquals(httCard, httCardLegacy);

        // Testing without passing explicit card art preference in CardDb
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, 1, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEditionReleasedBeforeFromTheVault);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameLightningDragon, 1, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), latestArtLightningDragonEditionReleasedBeforeFromTheVault);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, 1, httReleaseDate);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionReleasedBeforeEternalMasters);
        assertEquals(httCard, httCardLegacy);

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        artPreference = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        setPref = LegacyCardDb.LegacySetPreference.LatestCoreExp;

        sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEditionReleasedBeforeFromTheVaultNoPromo);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameLightningDragon, artPreference, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), latestArtLightningDragonEditionReleasedBeforeFromTheVaultNoPromo);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, httReleaseDate);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionReleasedBeforeEternalMastersNoPromo);
        assertEquals(httCard, httCardLegacy);

        /* --------------
            Old Print
           -------------*/
        artPreference = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;
        setPref = LegacyCardDb.LegacySetPreference.Earliest;

        sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEdition);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameLightningDragon, artPreference, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), originalArtLightningDragonEdition);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, httReleaseDate);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEdition);
        assertEquals(httCard, httCardLegacy);

        /* --------------------
            Old Print No Promo
         ----------------------*/
        artPreference = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        setPref = LegacyCardDb.LegacySetPreference.EarliestCoreExp;

        sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEdition);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameLightningDragon, artPreference, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), originalArtLightningDragonEditionNoPromo);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, httReleaseDate);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEditionNoPromo);
        assertEquals(httCard, httCardLegacy);
    }

    @Test
    public void testGetCardFromEditionsWithCardNameAndCardArtPreferenceReleasedBeforeDateComparedWithLegacyAlsoIncludingArtIndex() {
        // NOTE: Not passing in ArtIndex (so testing w/ default value) whenever artIndex is irrelevant (already default)
        // Set Reference Dates
        Date sdReleaseDate = null;
        Date httReleaseDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            sdReleaseDate = format.parse(releasedBeforeFromTheVaultDate);
            httReleaseDate = format.parse(releasedBeforeEternalMastersDate);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference artPreference = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;
        LegacyCardDb.LegacySetPreference setPref = LegacyCardDb.LegacySetPreference.Latest;

        PaperCard sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, sdReleaseDate);
        PaperCard sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref, 1);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEditionReleasedBeforeFromTheVault);
        assertEquals(sdCard, sdCardLegacy);

        PaperCard ldCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameLightningDragon, artPreference, sdReleaseDate);
        PaperCard ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref, 1);
        assertEquals(ldCard.getEdition(), latestArtLightningDragonEditionReleasedBeforeFromTheVault);
        assertEquals(ldCard, ldCardLegacy);

        PaperCard httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, httReleaseDate);
        PaperCard httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref, 1);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionReleasedBeforeEternalMasters);
        assertEquals(httCard, httCardLegacy);

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        artPreference = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        setPref = LegacyCardDb.LegacySetPreference.LatestCoreExp;

        sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref, 1);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEditionReleasedBeforeFromTheVaultNoPromo);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameLightningDragon, artPreference, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref, 1);
        assertEquals(ldCard.getEdition(), latestArtLightningDragonEditionReleasedBeforeFromTheVaultNoPromo);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, httReleaseDate);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref, 1);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionReleasedBeforeEternalMastersNoPromo);
        assertEquals(httCard, httCardLegacy);

        /* --------------
            Old Print
           -------------*/
        artPreference = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;
        setPref = LegacyCardDb.LegacySetPreference.Earliest;

        sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref, 1);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEdition);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameLightningDragon, artPreference, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref, 1);
        assertEquals(ldCard.getEdition(), originalArtLightningDragonEdition);
        assertEquals(ldCard, ldCardLegacy);

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, artIdx, httReleaseDate);
            httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref, artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
            assertEquals(httCard, httCardLegacy);
        }

        /* --------------------
            Old Print No Promo
         ----------------------*/
        artPreference = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        setPref = LegacyCardDb.LegacySetPreference.EarliestCoreExp;

        sdCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, artPreference, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEdition);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameLightningDragon, artPreference, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), originalArtLightningDragonEditionNoPromo);
        assertEquals(ldCard, ldCardLegacy);

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artPreference, artIdx, httReleaseDate);
            httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref, artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
            assertEquals(httCard, httCardLegacy);
        }

        // Testing with default value of CardArt Preference with multiple artIndex
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        this.cardDb.setCardArtPreference(false, true);  // Original Print, Filter on Core
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameHymnToTourach, artIdx, httReleaseDate);
            httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref, artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
            assertEquals(httCard, httCardLegacy);
        }

        // Restore Default Card Art Preference, for later use
        this.cardDb.setCardArtPreference(true, false);  // Latest Print, NO Filter
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

    }

    // == Specialised Card Art Preference Retrieval and Release Date Constraint (AFTER)
    @Test
    public void testGetCardFromEditionsWithCardNameAndCardArtPreferenceReleasedAfterDate(){
        // Set Reference Dates
        Date tenthEditionReleaseDate = null;
        Date anthologiesDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            tenthEditionReleaseDate = format.parse(releasedAfterTenthEditionDate);
            anthologiesDate = format.parse(releasedAfterAnthologiesDate);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        // == Original Art
        CardDb.CardArtPreference artPreference = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;

        PaperCard sdCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, artPreference, tenthEditionReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEditionReleasedAfterTenthEdition);

        // foiled card request
        PaperCard ldFoilCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameFoilLightningDragon, artPreference, tenthEditionReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEditionReleasedAfterTenthEdition);
        assertTrue(ldFoilCard.isFoil());

        PaperCard httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artPreference, anthologiesDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEditionReleasedAfterAnthologies);

        // == Original Art NO PROMO
        artPreference = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, artPreference, tenthEditionReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEditionReleasedAfterTenthEditionNoPromo);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameFoilLightningDragon, artPreference, tenthEditionReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEditionReleasedAfterTenthEditionNoPromo);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artPreference, anthologiesDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEditionReleasedAfterAnthologiesNoPromo);

         // == Latest Art
        artPreference = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;

        sdCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, artPreference, tenthEditionReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEdition);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameFoilLightningDragon, artPreference, tenthEditionReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestArtLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artPreference, anthologiesDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEdition);

        // == Latest Art NO PROMO
        artPreference = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, artPreference, tenthEditionReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEdition);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameFoilLightningDragon, artPreference, tenthEditionReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestArtLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artPreference, anthologiesDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionNoPromo);
    }

    @Test
    public void testGetCardFromEditionsWithCardNameAndCardArtPreferenceReleasedAfterDateAlsoIncludingArtIndex(){
        // Set Reference Dates
        Date tenthEditionReleaseDate = null;
        Date anthologiesDate = null;
        Date alphaRelaseDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            tenthEditionReleaseDate = format.parse(releasedAfterTenthEditionDate);
            anthologiesDate = format.parse(releasedAfterAnthologiesDate);
            // used for art index for Hymn to Tourach
            alphaRelaseDate = format.parse(alphaEditionReleaseDate);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        // == Original Art
        CardDb.CardArtPreference artPreference = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;

        PaperCard sdCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, artPreference, 1, tenthEditionReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEditionReleasedAfterTenthEdition);
        assertEquals(sdCard.getArtIndex(), 1);

        // foiled card request
        PaperCard ldFoilCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameFoilLightningDragon, artPreference, 1, tenthEditionReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEditionReleasedAfterTenthEdition);
        assertTrue(ldFoilCard.isFoil());
        assertEquals(ldFoilCard.getArtIndex(), 1);

        PaperCard httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artPreference, 1, anthologiesDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEditionReleasedAfterAnthologies);
        assertEquals(httCard.getArtIndex(), 1);

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artPreference, artIdx, alphaRelaseDate);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
            assertEquals(httCard.getArtIndex(), artIdx);
        }

        // == Original Art NO PROMO
        artPreference = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, artPreference, 1, tenthEditionReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEditionReleasedAfterTenthEditionNoPromo);
        assertEquals(sdCard.getArtIndex(), 1);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameFoilLightningDragon, artPreference, 1, tenthEditionReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEditionReleasedAfterTenthEditionNoPromo);
        assertTrue(ldFoilCard.isFoil());
        assertEquals(ldFoilCard.getArtIndex(), 1);

        httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artPreference, 1, anthologiesDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEditionReleasedAfterAnthologiesNoPromo);
        assertEquals(httCard.getArtIndex(), 1);

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artPreference, artIdx, alphaRelaseDate);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
            assertEquals(httCard.getArtIndex(), artIdx);
        }

        // == Latest Art
        artPreference = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;

        sdCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, artPreference, 1, tenthEditionReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEdition);
        assertEquals(sdCard.getArtIndex(), 1);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameFoilLightningDragon, artPreference, 1, tenthEditionReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestArtLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());
        assertEquals(ldFoilCard.getArtIndex(), 1);

        httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artPreference, 1, anthologiesDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEdition);
        assertEquals(httCard.getArtIndex(), 1);

        // == Latest Art NO PROMO
        artPreference = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, artPreference, 1, tenthEditionReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestArtShivanDragonEdition);
        assertEquals(sdCard.getArtIndex(), 1);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameFoilLightningDragon, artPreference, 1, tenthEditionReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestArtLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());
        assertEquals(ldFoilCard.getArtIndex(), 1);

        httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artPreference, 1, anthologiesDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEditionNoPromo);
        assertEquals(httCard.getArtIndex(), 1);
    }

    @Test
    public void testGetCardFromEditionsAfterReleaseDateUsingDefaultCardArtPreference(){
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        // Set Reference Dates
        Date tenthEditionReleaseDate = null;
        Date anthologiesDate = null;
        Date alphaRelaseDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            tenthEditionReleaseDate = format.parse(releasedAfterTenthEditionDate);
            anthologiesDate = format.parse(releasedAfterAnthologiesDate);
            // used for art index for Hymn to Tourach
            alphaRelaseDate = format.parse(alphaEditionReleaseDate);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        // == NOTE == Avoiding passing in also ArtIndex when it's not relevant (i.e. different than default)

        // == Original Art
        this.cardDb.setCardArtPreference(false, false);  // Original Print, Filter on Core
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);

        PaperCard sdCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, tenthEditionReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEditionReleasedAfterTenthEdition);
        assertEquals(sdCard.getArtIndex(), 1);

        // foiled card request
        PaperCard ldFoilCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameFoilLightningDragon, tenthEditionReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEditionReleasedAfterTenthEdition);
        assertTrue(ldFoilCard.isFoil());
        assertEquals(ldFoilCard.getArtIndex(), 1);

        PaperCard httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, anthologiesDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEditionReleasedAfterAnthologies);
        assertEquals(httCard.getArtIndex(), 1);

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artIdx, alphaRelaseDate);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
            assertEquals(httCard.getArtIndex(), artIdx);
        }

        // == Original Art NO PROMO
        this.cardDb.setCardArtPreference(false, true);  // Original Print, Filter on Core
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        sdCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, tenthEditionReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), originalArtShivanDragonEditionReleasedAfterTenthEditionNoPromo);
        assertEquals(sdCard.getArtIndex(), 1);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameFoilLightningDragon, tenthEditionReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), originalArtLightningDragonEditionReleasedAfterTenthEditionNoPromo);
        assertTrue(ldFoilCard.isFoil());
        assertEquals(ldFoilCard.getArtIndex(), 1);

        httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, anthologiesDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEditionReleasedAfterAnthologiesNoPromo);
        assertEquals(httCard.getArtIndex(), 1);

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameHymnToTourach, artIdx, alphaRelaseDate);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
            assertEquals(httCard.getArtIndex(), artIdx);
        }

        // Restore Default Card Art Preference, for later use
        this.cardDb.setCardArtPreference(true, false);  // Latest Print, NO Filter
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

    @Test
    public void testCounterSpellManyEditionsAlsoWithDateRestrictionsAndCardArtPreferences(){
        // Test fetching counterspell at different editions
        for (String setCode : this.editionsCounterspell){
            PaperCard counterSpell = this.cardDb.getCard(this.cardNameCounterspell, setCode);
            assertEquals(counterSpell.getName(), this.cardNameCounterspell);
            assertEquals(counterSpell.getEdition(), setCode);
            assertFalse(counterSpell.isFoil());
        }

        Date releaseDatebeforeMagicOnlinePromos = null;
        Date releaseDateBeforeEternalMasters = null;
        Date releaseDateAfterBattleRoyale = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            releaseDatebeforeMagicOnlinePromos = format.parse(counterspellReleasedBeforeMagicOnlinePromosDate);
            releaseDateBeforeEternalMasters = format.parse(counterspellReleasedBeforeEternalMastersDate);
            releaseDateAfterBattleRoyale = format.parse(counterspellReleasedAfterBattleRoyaleDate);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        PaperCard counterSpellCard;

        // == LATEST ART All Editions
        counterSpellCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameCounterspell,
                CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS, releaseDatebeforeMagicOnlinePromos);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestArtsReleasedBeforeMagicOnlinePromos[0]);

        counterSpellCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameCounterspell,
                CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS, releaseDateBeforeEternalMasters);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestArtReleasedBeforeEternalMasters[0]);

        // == LATEST ART No Promo
        counterSpellCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameCounterspell,
                CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY, releaseDatebeforeMagicOnlinePromos);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestArtsReleasedBeforeMagicOnlinePromos[1]);

        counterSpellCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameCounterspell,
                CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY, releaseDateBeforeEternalMasters);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestArtReleasedBeforeEternalMasters[1]);

        // == ORIGINAL ART All Editions
        counterSpellCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameCounterspell,
                CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS, releaseDateBeforeEternalMasters);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellOriginalArtReleasedAfterEternalMasters[0]);

        counterSpellCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameCounterspell,
                CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS, releaseDateAfterBattleRoyale);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellOriginalArtReleasedAfterBattleRoyale[0]);

        // == ORIGINAL ART No Promo
        counterSpellCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameCounterspell,
                CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY, releaseDateBeforeEternalMasters);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellOriginalArtReleasedAfterEternalMasters[1]);

        counterSpellCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameCounterspell,
                CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY, releaseDateAfterBattleRoyale);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellOriginalArtReleasedAfterBattleRoyale[1]);

        // Now with setting preferences - so going with default cardArt preference.

        // == Latest Art
        this.cardDb.setCardArtPreference(true, false);
        counterSpellCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameCounterspell, releaseDatebeforeMagicOnlinePromos);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestArtsReleasedBeforeMagicOnlinePromos[0]);

        counterSpellCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameCounterspell, releaseDateBeforeEternalMasters);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestArtReleasedBeforeEternalMasters[0]);

        // == Latest Art No Promo
        this.cardDb.setCardArtPreference(true, true);
        counterSpellCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameCounterspell, releaseDatebeforeMagicOnlinePromos);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestArtsReleasedBeforeMagicOnlinePromos[1]);

        counterSpellCard = this.cardDb.getCardFromEditionsReleasedBefore(cardNameCounterspell, releaseDateBeforeEternalMasters);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestArtReleasedBeforeEternalMasters[1]);

        // == Original Art
        this.cardDb.setCardArtPreference(false, false);
        counterSpellCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameCounterspell, releaseDateBeforeEternalMasters);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellOriginalArtReleasedAfterEternalMasters[0]);

        counterSpellCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameCounterspell, releaseDateAfterBattleRoyale);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellOriginalArtReleasedAfterBattleRoyale[0]);

        // == Original Art No Promo
        this.cardDb.setCardArtPreference(false, true);
        counterSpellCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameCounterspell, releaseDateBeforeEternalMasters);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellOriginalArtReleasedAfterEternalMasters[1]);

        counterSpellCard = this.cardDb.getCardFromEditionsReleasedAfter(cardNameCounterspell, releaseDateAfterBattleRoyale);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellOriginalArtReleasedAfterBattleRoyale[1]);

        // restore default
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }


    // == Testing other Non-Correct Input Parameter values
    @Test
    public void testGetCardByNameWithNull(){
        PaperCard nullCard = this.cardDb.getCard(null);
        assertNull(nullCard);
    }

    @Test
    public void testGetCardByNameAndSetWithNullSet(){
        /*If no set is specified, the method will ultimately resort to be using the
         * CardArtPreference policy to retrieve a copy of the card requested.
         */
        PaperCard httCard = this.cardDb.getCard(cardNameHymnToTourach, null);
        assertNotNull(httCard);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        // If not specified, card art preference should be LatestPrint
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        assertEquals(httCard.getEdition(), latestArtHymnToTourachEdition);
        // Try changing the policy
        this.cardDb.setCardArtPreference(false, true);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);
        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), originalArtHymnToTourachEditionNoPromo);
        // restore default
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

    @Test
    public void testGetCardByNameAndSetWitNegativeArtIndex(){
        PaperCard httCard = this.cardDb.getCard(cardNameHymnToTourach, editionHymnToTourach, -10);
        assertNotNull(httCard);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), editionHymnToTourach);
        assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[0]);
        assertEquals(httCard.getArtIndex(), IPaperCard.DEFAULT_ART_INDEX);
    }

    @Test
    public void testThatCardRequestPassedInHaveNoSideEffectAndThatAreCorrectlyProcessed(){
        String cardName = this.cardNameHymnToTourach;
        String httEdition = this.originalArtHymnToTourachEdition;
        int artIndexFEM = 3;
        String requestInfo = CardDb.CardRequest.compose(cardName, httEdition, artIndexFEM);

        // assert default condition
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        // === Get Card
        // == 1. Pass in Request Info
        PaperCard hymnToTourachCard = this.cardDb.getCard(requestInfo);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        assertEquals(hymnToTourachCard.getArtIndex(), artIndexFEM);

        // == 2. Pass in Card Name, Set Code, ArtIndex
        hymnToTourachCard = this.cardDb.getCard(cardName, httEdition, artIndexFEM);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        assertEquals(hymnToTourachCard.getArtIndex(), artIndexFEM);

        // == 3. Pass in RequestInfo as Card Name
        hymnToTourachCard = this.cardDb.getCard(requestInfo, httEdition, artIndexFEM);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        assertEquals(hymnToTourachCard.getArtIndex(), artIndexFEM);

        // == 4. Pass in RequestInfo as CardName but then requesting for different artIndex
        hymnToTourachCard = this.cardDb.getCard(requestInfo, httEdition, 2);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        assertEquals(hymnToTourachCard.getArtIndex(), 2);

        // == 5. Pass in RequestInfo as CardName but then requesting for different edition and artIndex
        hymnToTourachCard = this.cardDb.getCard(requestInfo, latestArtHymnToTourachEdition, 1);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), latestArtHymnToTourachEdition);
        assertEquals(hymnToTourachCard.getArtIndex(), 1);

        // === Get Card From Set

        // == 1. Reference with all expected params
        hymnToTourachCard = this.cardDb.getCardFromSet(cardName, StaticData.instance().getCardEdition(httEdition), artIndexFEM, false);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        assertEquals(hymnToTourachCard.getArtIndex(), artIndexFEM);

        // == 2. Pass in RequestInfo as Card Name
        hymnToTourachCard = this.cardDb.getCardFromSet(requestInfo, StaticData.instance().getCardEdition(httEdition), artIndexFEM, false);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        assertEquals(hymnToTourachCard.getArtIndex(), artIndexFEM);

        // == 3. Pass in RequestInfo but request for a different art Index
        hymnToTourachCard = this.cardDb.getCardFromSet(requestInfo, StaticData.instance().getCardEdition(httEdition), 2, false);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        assertEquals(hymnToTourachCard.getArtIndex(), 2);

        // == 4. Pass in RequestInfo as Card Name but request for a different art Index and Edition
        hymnToTourachCard = this.cardDb.getCardFromSet(requestInfo,
                StaticData.instance().getCardEdition(latestArtHymnToTourachEdition), 1, false);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), latestArtHymnToTourachEdition);
        assertEquals(hymnToTourachCard.getArtIndex(), 1);

        // === Get Card From Editions

        // == 1. Reference case
        hymnToTourachCard = this.cardDb.getCardFromEditions(cardName, CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        assertEquals(hymnToTourachCard.getArtIndex(), 1);

        // == 2. Pass in Request String as CardName
        hymnToTourachCard = this.cardDb.getCardFromEditions(requestInfo, CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        // expecting this because artIndex is already in Request Info
        assertEquals(hymnToTourachCard.getArtIndex(), artIndexFEM);

        // == 3. Changing CardArtPreference so that it would not be compliant with request
        // STILL expecting to get in return whatever is in request as no extra param has been provided.
        hymnToTourachCard = this.cardDb.getCardFromEditions(requestInfo, CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        // expecting this edition as present in request info
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        // expecting this because artIndex is already in Request Info
        assertEquals(hymnToTourachCard.getArtIndex(), 3);

        // == 4. Changing Art Index (not default) so still requesting card via request String
        hymnToTourachCard = this.cardDb.getCardFromEditions(requestInfo,
                CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS, 2);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        // expecting this edition as present in request info
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        // artIndex should be overwritten this time, as it's provided and not default
        assertEquals(hymnToTourachCard.getArtIndex(), 2);

        // == 4. Changing Art Index (this time with default) = so initially requested artIndex won't get changed!
        hymnToTourachCard = this.cardDb.getCardFromEditions(requestInfo,
                CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS, 1);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        // expecting this edition as present in request info
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        // artIndex should still be the one requested in CardRequest as value passed is default
        assertEquals(hymnToTourachCard.getArtIndex(), artIndexFEM);

        // == 5. Passing in Card Name Only
        hymnToTourachCard = this.cardDb.getCardFromEditions(cardName, CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        // expecting this edition as returned due to CardArtPreference
        assertEquals(hymnToTourachCard.getEdition(), latestArtHymnToTourachEdition);
        // artIndex should be overwritten this time, as it's provided and not default
        assertEquals(hymnToTourachCard.getArtIndex(), 1);

        // == 6. Forcing in a specific Art Index will overrule Art Preference
        hymnToTourachCard = this.cardDb.getCardFromEditions(cardName,
                CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS, artIndexFEM);
        assertNotNull(hymnToTourachCard);
        assertEquals(hymnToTourachCard.getName(), cardName);
        // expecting this edition as returned due to CardArtPreference
        assertEquals(hymnToTourachCard.getEdition(), httEdition);
        // artIndex should be overwritten this time, as it's provided and not default
        assertEquals(hymnToTourachCard.getArtIndex(), artIndexFEM);
    }

    @Test
    public void testGetCardByNameAndSetWithWrongORNullCollectorNumber(){
        PaperCard httCard = this.cardDb.getCard(cardNameHymnToTourach, editionHymnToTourach, "589b");
        assertNull(httCard);

        httCard = this.cardDb.getCard(cardNameHymnToTourach, editionHymnToTourach, null);
        assertNotNull(httCard);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        assertEquals(httCard.getEdition(), editionHymnToTourach);
        assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[0]);
    }

    @Test
    public void testGetCardFromSetWithNullValues(){
        CardEdition cardEdition = FModel.getMagicDb().getEditions().get(editionShivanDragon);

        PaperCard nullCard = this.cardDb.getCardFromSet(null, cardEdition, false);
        assertNull(nullCard);

        nullCard = this.cardDb.getCardFromSet(cardNameShivanDragon, null, false);
        assertNull(nullCard);

        // null collector number
        PaperCard shivanCard = this.cardDb.getCardFromSet(cardNameShivanDragon, cardEdition, null,false);
        assertEquals(shivanCard.getArtIndex(), 1);
        assertEquals(shivanCard.getName(), cardNameShivanDragon);
        assertEquals(shivanCard.getEdition(), editionShivanDragon);
        assertEquals(shivanCard.getCollectorNumber(), collNrShivanDragon);

        // negative artIndex
        shivanCard = this.cardDb.getCardFromSet(cardNameShivanDragon, cardEdition, -20,false);
        assertEquals(shivanCard.getArtIndex(), 1);
        assertEquals(shivanCard.getName(), cardNameShivanDragon);
        assertEquals(shivanCard.getEdition(), editionShivanDragon);
        assertEquals(shivanCard.getCollectorNumber(), collNrShivanDragon);

        // both above cases
        shivanCard = this.cardDb.getCardFromSet(cardNameShivanDragon, cardEdition, -20, null,false);
        assertEquals(shivanCard.getArtIndex(), 1);
        assertEquals(shivanCard.getName(), cardNameShivanDragon);
        assertEquals(shivanCard.getEdition(), editionShivanDragon);
        assertEquals(shivanCard.getCollectorNumber(), collNrShivanDragon);

        shivanCard = this.cardDb.getCardFromSet(cardNameShivanDragon, cardEdition, null,false);
        assertEquals(shivanCard.getArtIndex(), 1);
        assertEquals(shivanCard.getName(), cardNameShivanDragon);
        assertEquals(shivanCard.getEdition(), editionShivanDragon);
        assertEquals(shivanCard.getCollectorNumber(), collNrShivanDragon);
    }

    @Test
    public void testNullAndBoundaryDateValuesForGetCardFromEditionsWithDateRestrictions(){
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        PaperCard shivanDragon = this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, null);
        assertNotNull(shivanDragon);
        assertEquals(shivanDragon.getName(), cardNameShivanDragon);
        assertEquals(shivanDragon.getEdition(), latestArtShivanDragonEdition);

        shivanDragon = this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, null);
        assertNotNull(shivanDragon);
        assertEquals(shivanDragon.getName(), cardNameShivanDragon);
        assertEquals(shivanDragon.getEdition(), latestArtShivanDragonEdition);

        Date alphaRelaseDate = null;
        Date currentDate = Date.from(Instant.now());
        Date latestShivanDragonReleaseDateToDate = null;  // latest print to date for Shivan is in M20
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            alphaRelaseDate = format.parse(alphaEditionReleaseDate);
            latestShivanDragonReleaseDateToDate = format.parse("2019-07-12");
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        assertNull(this.cardDb.getCardFromEditionsReleasedBefore(cardNameShivanDragon, alphaRelaseDate));
        assertNull(this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, currentDate));
        assertNull(this.cardDb.getCardFromEditionsReleasedAfter(cardNameShivanDragon, latestShivanDragonReleaseDateToDate));
    }

    @Test
    public void testGetMaxArtIndex() {
        int maxArtIndex = this.cardDb.getMaxArtIndex(cardNameHymnToTourach);
        assertEquals(maxArtIndex, 4);

        int nonExistingCardArtIndex = this.cardDb.getMaxArtIndex("ImaginaryMagicCard");
        assertEquals(nonExistingCardArtIndex, IPaperCard.NO_ART_INDEX);

        int nullCardInInput = this.cardDb.getMaxArtIndex(null);
        assertEquals(nullCardInInput, IPaperCard.NO_ART_INDEX);

        // Compare with LegacyDB
        int maxArtCountLegacy = this.legacyCardDb.getMaxPrintCount(cardNameHymnToTourach);
        assertEquals(maxArtCountLegacy, maxArtIndex);
    }

    @Test
    public void testGetArtCount() {
        int httFEMArtCount = this.cardDb.getArtCount(cardNameHymnToTourach, editionHymnToTourach);
        assertEquals(httFEMArtCount, 4);

        int lightningDragonPUSGArtCount = this.cardDb.getArtCount(cardNameLightningDragon, editionLightningDragon);
        assertEquals(lightningDragonPUSGArtCount, 1);

        int nonExistingCardArtCount = this.cardDb.getArtCount(cardNameShivanDragon, editionLightningDragon);
        assertEquals(nonExistingCardArtCount, 0);

        // Compare with LegacyDB
        int httFEMArtCountLegacy = this.legacyCardDb.getPrintCount(cardNameHymnToTourach, editionHymnToTourach);
        assertEquals(httFEMArtCountLegacy, httFEMArtCount);
        assertEquals(this.legacyCardDb.getArtCount(cardNameHymnToTourach, editionHymnToTourach), httFEMArtCount);
    }

    @Test
    public void testSetCardArtPreference() {
        // First Off try and see if using constants returned by CardArtPreference.getPreferences will work
        CardDb.CardArtPreference[] prefs = {CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS,
                                            CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY,
                                            CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS,
                                            CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY};
        for (int i = 0; i < 4; i++){
            boolean latest = prefs[i].latestFirst;
            boolean coreExpFilter = prefs[i].filterSets;
            this.cardDb.setCardArtPreference(latest, coreExpFilter);
            assertEquals(this.cardDb.getCardArtPreference(), prefs[i]);
        }

        // Test different wording and Legacy Options (esp. for legacy w/ Mobile)

        // LEGACY OPTIONS
        this.cardDb.setCardArtPreference("LatestCoreExp");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        this.cardDb.setCardArtPreference("Earliest");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);

        this.cardDb.setCardArtPreference("EarliestCoreExp");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        this.cardDb.setCardArtPreference("Latest");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        this.cardDb.setCardArtPreference("Random");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        // DIFFERENT WORDINGS
        this.cardDb.setCardArtPreference("Earliest Editions");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);

        this.cardDb.setCardArtPreference("Earliest Editions (Core Expansions Reprint)");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        this.cardDb.setCardArtPreference("Old Card Frame");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        this.cardDb.setCardArtPreference("Latest Editions");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        this.cardDb.setCardArtPreference("Latest Editions (Core Expansions Reprint)");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        this.cardDb.setCardArtPreference("Latest (All Editions)");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        this.cardDb.setCardArtPreference("LATEST_ART_ALL_EDITIONS");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        this.cardDb.setCardArtPreference("ORIGINAL_ART_ALL_EDITIONS");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);

        this.cardDb.setCardArtPreference("LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        this.cardDb.setCardArtPreference("ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY");
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        // Test non existing option
        this.cardDb.setCardArtPreference("Non existing option");
        // default goes to Latest Art All Editions
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

    @Test
    public void testSnowCoveredBasicLandsWithCartArtPreference(){
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String snowCoveredLand = "Snow-Covered Island";
        PaperCard landCard = this.cardDb.getCard(snowCoveredLand);
        assertNotNull(landCard);
        assertEquals(landCard.getName(), snowCoveredLand);
        assertEquals(landCard.getEdition(), "KHM");

        this.cardDb.setCardArtPreference(true, true);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        landCard = this.cardDb.getCard(snowCoveredLand);
        assertNotNull(landCard);
        assertEquals(landCard.getName(), snowCoveredLand);
        assertEquals(landCard.getEdition(), "KHM");

        // reset default
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

    /**
     * This set is crucial to test Card Art Preference and Strict Policies.
     * In particular, we wish to test whether the DB is robust enough to retrieve
     * the card even if Art Preference is too strict, that is: the card is only
     * available in Filtered sets.
     *
     * When this happens, we also want to be sure that retrieved card will be
     * still compliant with Art Preference, when multiple candidates are possible
     * (therefore, latest or original art first)
     *
     * For this test we will use the following card/editions as fixtures:
     * - Militant Angel: ONLY available in forge in Game Night
     * - Loyal Unicorn: Available in Forge in The List, and COMMANDER 2018
     * - Selfless Squire: Available in Forge in COMMANDER 2021, Treasure Chest, and COMMANDER 2016
     * - Atog: Test card available in Promo and Non-Promo Print. We will use this card as reference
     *         which will have multiple editions returned over the preference selections.
     */
    @Test
    public void testCardsAlwaysReturnedEvenIfCardArtPreferenceIsTooStrictAlsoComparedWithLegacyDb(){
        // == 1. REFERENCE CASE - Latest Art NO FILTER
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        String cnAtog = "Atog";
        PaperCard atog = this.cardDb.getCard(cnAtog);
        assertNotNull(atog);
        assertEquals(atog.getEdition(), "ME4");  // Masters Edition IV

        PaperCard legacyAtog = this.legacyCardDb.getCardFromEdition(cnAtog, LegacyCardDb.LegacySetPreference.Latest);
        if (legacyAtog != null)
            assertEquals(atog, legacyAtog);

        String cnMilitantAngel = "Militant Angel";
        PaperCard militantAngel = this.cardDb.getCard(cnMilitantAngel);
        assertNotNull(militantAngel);
        assertEquals(militantAngel.getEdition(), "GNT");  // Game Night

        PaperCard legacyMilitantAngel = this.legacyCardDb.getCardFromEdition(cnMilitantAngel,
                LegacyCardDb.LegacySetPreference.Latest);
        if (legacyMilitantAngel != null)
            assertEquals(militantAngel, legacyMilitantAngel);

        // Loyal Unicorn: Available in Forge in The List and COMMANDER 2018
        String cnLoyalUnicorn = "Loyal Unicorn";
        PaperCard loyalUnicorn = this.cardDb.getCard(cnLoyalUnicorn);
        assertNotNull(loyalUnicorn);
        assertEquals(loyalUnicorn.getEdition(), "PLIST");  // The List

        PaperCard legacyLoyalUnicorn = this.legacyCardDb.getCardFromEdition(cnLoyalUnicorn,
                LegacyCardDb.LegacySetPreference.Latest);
        if (legacyLoyalUnicorn != null)
            assertEquals(loyalUnicorn, legacyLoyalUnicorn);

        // Selfless Squire: Available in Forge in COMMANDER 2021; Treasure Chest; COMMANDER 2016
        String cnSelflessSquire = "Selfless Squire";
        PaperCard selflessSquire = this.cardDb.getCard(cnSelflessSquire);
        assertNotNull(selflessSquire);
        assertEquals(selflessSquire.getEdition(), "C21");  // Commander 2021

        PaperCard legacySelflessSquire = this.legacyCardDb.getCardFromEdition(cnSelflessSquire,
                LegacyCardDb.LegacySetPreference.Latest);
        if (legacySelflessSquire != null)
            assertEquals(selflessSquire, legacySelflessSquire);

        // == 2. Set Strictness to Expansions and Reprint Only (LATEST)
        this.cardDb.setCardArtPreference(true, true);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        // ONLY CHANGE HERE IS FOR ATOG
        atog = this.cardDb.getCard(cnAtog);
        assertNotNull(atog);
        assertEquals(atog.getEdition(), "MRD");

        legacyAtog = this.legacyCardDb.getCardFromEdition(cnAtog, LegacyCardDb.LegacySetPreference.LatestCoreExp);
        if (legacyAtog != null)
            assertEquals(atog, legacyAtog);

        militantAngel = this.cardDb.getCard(cnMilitantAngel);
        assertNotNull(militantAngel);
        assertEquals(militantAngel.getEdition(), "GNT");

        legacyMilitantAngel = this.legacyCardDb.getCardFromEdition(cnMilitantAngel,
                LegacyCardDb.LegacySetPreference.LatestCoreExp);
        if (legacyMilitantAngel != null)
            assertEquals(militantAngel, legacyMilitantAngel);

        // Loyal Unicorn: Available in Forge in The List and COMMANDER 2018
        loyalUnicorn = this.cardDb.getCard(cnLoyalUnicorn);
        assertNotNull(loyalUnicorn);
        assertEquals(loyalUnicorn.getEdition(), "PLIST");

        legacyLoyalUnicorn = this.legacyCardDb.getCardFromEdition(cnLoyalUnicorn,
                LegacyCardDb.LegacySetPreference.LatestCoreExp);
        if (legacyLoyalUnicorn != null)
            assertEquals(loyalUnicorn, legacyLoyalUnicorn);

        // Selfless Squire: Available in Forge in COMMANDER 2021; Treasure Chest; COMMANDER 2016
        selflessSquire = this.cardDb.getCard(cnSelflessSquire);
        assertNotNull(selflessSquire);
        assertEquals(selflessSquire.getEdition(), "C21");

        legacySelflessSquire = this.legacyCardDb.getCardFromEdition(cnSelflessSquire,
                LegacyCardDb.LegacySetPreference.LatestCoreExp);
        if (legacySelflessSquire != null)
            assertEquals(selflessSquire, legacySelflessSquire);

        // == 3. Set Strictness to ORIGINAL ART NO FILTER
        this.cardDb.setCardArtPreference(false, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);

        atog = this.cardDb.getCard(cnAtog);
        assertNotNull(atog);
        assertEquals(atog.getEdition(), "ATQ");

        legacyAtog = this.legacyCardDb.getCardFromEdition(cnAtog, LegacyCardDb.LegacySetPreference.Earliest);
        if (legacyAtog != null)
            assertEquals(atog, legacyAtog);

        militantAngel = this.cardDb.getCard(cnMilitantAngel);
        assertNotNull(militantAngel);
        assertEquals(militantAngel.getEdition(), "GNT");

        legacyMilitantAngel = this.legacyCardDb.getCardFromEdition(cnMilitantAngel,
                LegacyCardDb.LegacySetPreference.Earliest);
        if (legacyMilitantAngel != null)
            assertEquals(militantAngel, legacyMilitantAngel);

        // Loyal Unicorn: Available in Forge in The List and COMMANDER 2018
        loyalUnicorn = this.cardDb.getCard(cnLoyalUnicorn);
        assertNotNull(loyalUnicorn);
        assertEquals(loyalUnicorn.getEdition(), "C18");

        legacyLoyalUnicorn = this.legacyCardDb.getCardFromEdition(cnLoyalUnicorn,
                LegacyCardDb.LegacySetPreference.Earliest);
        if (legacyLoyalUnicorn != null)
            assertEquals(loyalUnicorn, legacyLoyalUnicorn);

        // Selfless Squire: Available in Forge in COMMANDER 2021; Treasure Chest; COMMANDER 2016
        selflessSquire = this.cardDb.getCard(cnSelflessSquire);
        assertNotNull(selflessSquire);
        assertEquals(selflessSquire.getEdition(), "C16");

        legacySelflessSquire = this.legacyCardDb.getCardFromEdition(cnSelflessSquire,
                LegacyCardDb.LegacySetPreference.Earliest);
        if (legacySelflessSquire != null)
            assertEquals(selflessSquire, legacySelflessSquire);

        // == 4. Set Strictness to ORIGINAL ART WITH FILTER (*only*)
        this.cardDb.setCardArtPreference(false, true);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        atog = this.cardDb.getCard(cnAtog);
        assertNotNull(atog);
        assertEquals(atog.getEdition(), "ATQ");

        legacyAtog = this.legacyCardDb.getCardFromEdition(cnAtog, LegacyCardDb.LegacySetPreference.EarliestCoreExp);
        if (legacyAtog != null)
            assertEquals(atog, legacyAtog);

        militantAngel = this.cardDb.getCard(cnMilitantAngel);
        assertNotNull(militantAngel);
        assertEquals(militantAngel.getEdition(), "GNT");

        legacyMilitantAngel = this.legacyCardDb.getCardFromEdition(cnMilitantAngel,
                LegacyCardDb.LegacySetPreference.EarliestCoreExp);
        if (legacyMilitantAngel != null)
            assertEquals(militantAngel, legacyMilitantAngel);

        loyalUnicorn = this.cardDb.getCard(cnLoyalUnicorn);
        assertNotNull(loyalUnicorn);
        assertEquals(loyalUnicorn.getEdition(), "PLIST");

        legacyLoyalUnicorn = this.legacyCardDb.getCardFromEdition(cnLoyalUnicorn,
                LegacyCardDb.LegacySetPreference.EarliestCoreExp);
        if (legacyLoyalUnicorn != null)
            assertEquals(loyalUnicorn, legacyLoyalUnicorn);

        selflessSquire = this.cardDb.getCard(cnSelflessSquire);
        assertNotNull(selflessSquire);
        assertEquals(selflessSquire.getEdition(), "C16");

        legacySelflessSquire = this.legacyCardDb.getCardFromEdition(cnSelflessSquire,
                LegacyCardDb.LegacySetPreference.EarliestCoreExp);
        if (legacySelflessSquire != null)
            assertEquals(selflessSquire, legacySelflessSquire);

        // Set Art Preference back to default
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

    @Test
    public void testGetCardFromUnknownSet(){
        String unknownCardName = "Unknown Card Name";
        PaperCard unknownCard = new PaperCard(CardRules.getUnsupportedCardNamed(unknownCardName),
                                              CardEdition.UNKNOWN.getCode(), CardRarity.Unknown);
        this.cardDb.addCard(unknownCard);
        assertTrue(this.cardDb.getAllCards().contains(unknownCard));
        assertNotNull(this.cardDb.getAllCards(unknownCardName));
        assertEquals(this.cardDb.getAllCards(unknownCardName).size(), 1);

        PaperCard retrievedPaperCard = this.cardDb.getCard(unknownCardName);
        assertNotNull(retrievedPaperCard);
        assertEquals(retrievedPaperCard.getName(), unknownCardName);
        assertEquals(retrievedPaperCard.getEdition(), CardEdition.UNKNOWN.getCode());
    }

    @Test
    public void testGetCardFromWrongEditionOrNonExistingEditionReturnsNullResult(){
        String cardName = "Blinding Angel";
        String wrongSetCode = "LEA";  // obiviously wrong

        String requestInfo = CardDb.CardRequest.compose(cardName, wrongSetCode);
        PaperCard blindingAngelCard = this.cardDb.getCard(requestInfo);
        PaperCard legacyBlindingAngelCard = this.legacyCardDb.getCard(requestInfo);
        assertNull(legacyBlindingAngelCard);  // be sure behaviour is the same
        assertNull(blindingAngelCard);

        String nonExistingSetCode = "9TH";  // non-existing, should be 9ED
        requestInfo = CardDb.CardRequest.compose(cardName, nonExistingSetCode);
        blindingAngelCard = this.cardDb.getCard(requestInfo);
        legacyBlindingAngelCard = this.legacyCardDb.getCard(requestInfo);
        assertNull(legacyBlindingAngelCard);  // be sure behaviour is the same
        assertNull(blindingAngelCard);
    }

    // Case Insensitive Search/Retrieval Tests
    @Test
    public void testUpdatedCardDBAPICardNameWithWrongCaseWillStillReturnTheCorrectCard() {
        String cardName = "AEther baRRIER"; // wrong case
        String setCode = "NMS";
        String requestInfo = CardDb.CardRequest.compose(cardName, setCode);
        PaperCard aetherBarrierCard = this.cardDb.getCard(requestInfo);
        assertNotNull(aetherBarrierCard);
        assertEquals(aetherBarrierCard.getName(), "Aether Barrier");
        assertEquals(aetherBarrierCard.getEdition(), "NMS");

        // Compare w/ LegacyDb
        PaperCard legacyAetherBarrierCard = this.legacyCardDb.getCard(requestInfo);
        assertEquals(aetherBarrierCard, legacyAetherBarrierCard);
    }

    @Test
    public void testWrongCaseInEditionSetCodeReturnsNull(){
        String cardName = "Aether Barrier"; // correct name
        String setCode = "nmS";  // wrong case, non-existing
        String requestInfo = CardDb.CardRequest.compose(cardName, setCode);
        PaperCard aetherBarrierCard = this.cardDb.getCard(requestInfo);
        assertNotNull(aetherBarrierCard);
        assertEquals(aetherBarrierCard.getName(), cardName);
        assertEquals(aetherBarrierCard.getEdition(), setCode.toUpperCase());

        // Compare w/ LegacyDb
        PaperCard legacyAetherBarrierCard = this.legacyCardDb.getCard(requestInfo);
        assertNotNull(legacyAetherBarrierCard);
        assertEquals(legacyAetherBarrierCard, aetherBarrierCard);
    }

    // "Problematic" Card names
    @Test
    public void testRetrievingBorrowing100_000ArrowsCard(){
        String cardName = "Borrowing 100,000 Arrows";
        PaperCard borrowingCard = this.cardDb.getCard(cardName);
        assertNotNull(borrowingCard);
        assertEquals(borrowingCard.getName(), cardName);

        // Compare w/ LegacyDb
        PaperCard legacyBorrowingCard = this.legacyCardDb.getCardFromEdition(cardName, LegacyCardDb.LegacySetPreference.Latest);
        assertEquals(legacyBorrowingCard, borrowingCard);
    }

    @Test
    public void testGetCardWithDashInNameAndWrongCaseToo(){
        String requestInfo = "Ainok Bond-kin|KTK";  // wrong case for last 'k' and dash in name
        PaperCard ainokCard = this.cardDb.getCard(requestInfo);
        assertNotNull(ainokCard);
        assertEquals(ainokCard.getName(), "Ainok Bond-Kin");
        assertEquals(ainokCard.getEdition(), "KTK");

        // Compare w/ LegacyDb
        PaperCard legacyAinokCard = this.legacyCardDb.getCard(requestInfo);
        assertEquals(legacyAinokCard, ainokCard);
    }

    @Test
    public void testGetIslandsFromEditionsWithSpecificArtIndex(){
        String cardName = "Island";
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        PaperCard islandLatest = this.cardDb.getCardFromEditions(cardName, CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS, 12);
        assertNotNull(islandLatest);
        assertEquals(islandLatest.getName(), "Island");
        assertEquals(islandLatest.getEdition(), "SLD");
        assertEquals(islandLatest.getArtIndex(), 12);

        // SLD
        PaperCard islandOriginal = this.cardDb.getCardFromEditions(cardName, CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY, 12);
        assertNotNull(islandOriginal);
        assertEquals(islandOriginal.getName(), "Island");
        assertEquals(islandOriginal.getEdition(), "SLD");
        assertEquals(islandOriginal.getArtIndex(), 12);
    }

    @Test
    public void testMaxArtCountForBasicLand(){
        int maxArtIndex = this.cardDb.getMaxArtIndex("Island");
        assertTrue(maxArtIndex >= 14);
    }

    @Test
    public void testGetCardFromEditionsWithFilteredPool(){
        // test initial conditions
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        // Set Allowed Editions
        List<String> allowedSets = new ArrayList<>();
        allowedSets.add(this.latestArtShivanDragonEdition);
        allowedSets.add(this.originalArtShivanDragonEdition);
        Predicate<PaperCard> printedInSetPredicate = (Predicate<PaperCard>) this.cardDb.wasPrintedInSets(allowedSets);
        PaperCard shivanDragonCard = this.cardDb.getCardFromEditions(this.cardNameShivanDragon, printedInSetPredicate);
        assertNotNull(shivanDragonCard);
        assertEquals(shivanDragonCard.getName(), this.cardNameShivanDragon);
        assertEquals(shivanDragonCard.getEdition(), this.latestArtShivanDragonEdition);

        // Use Original Art Preference Now
        shivanDragonCard = this.cardDb.getCardFromEditions(this.cardNameShivanDragon, CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS, printedInSetPredicate);
        assertNotNull(shivanDragonCard);
        assertEquals(shivanDragonCard.getName(), this.cardNameShivanDragon);
        assertEquals(shivanDragonCard.getEdition(), this.originalArtShivanDragonEdition);

        // Testing null cards
        allowedSets.clear();
        allowedSets.add(this.originalArtHymnToTourachEdition);  // FEM - it does not exist a shivan in FEM
        printedInSetPredicate = (Predicate<PaperCard>) this.cardDb.wasPrintedInSets(allowedSets);
        shivanDragonCard = this.cardDb.getCardFromEditions(this.cardNameShivanDragon, printedInSetPredicate);
        assertNull(shivanDragonCard);
    }

    @Test
    public void testGetCardsFromEditionsReleasedBeforeDateWithFilter(){
        // test initial conditions
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        Date afterTenthEdition = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            afterTenthEdition = format.parse(releasedAfterTenthEditionDate);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        // Set Allowed Editions
        List<String> allowedSets = new ArrayList<>();
        allowedSets.add(this.latestArtShivanDragonEdition);
        allowedSets.add(this.originalArtShivanDragonEdition);
        allowedSets.add(this.originalArtShivanDragonEditionReleasedAfterTenthEditionNoPromo);
        Predicate<PaperCard> legalInSetFilter = (Predicate<PaperCard>) this.cardDb.isLegal(allowedSets);
        PaperCard shivanDragonCard = this.cardDb.getCardFromEditionsReleasedAfter(this.cardNameShivanDragon, CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS,
                                                                                  afterTenthEdition, legalInSetFilter);
        assertNotNull(shivanDragonCard);
        assertEquals(shivanDragonCard.getName(), cardNameShivanDragon);
        assertEquals(shivanDragonCard.getEdition(), latestArtShivanDragonEdition);

        // Original Art Should be excluded by date filter
        shivanDragonCard = this.cardDb.getCardFromEditionsReleasedAfter(this.cardNameShivanDragon, CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS,
                afterTenthEdition, legalInSetFilter);
        assertNotNull(shivanDragonCard);
        assertEquals(shivanDragonCard.getName(), cardNameShivanDragon);
        assertEquals(shivanDragonCard.getEdition(), originalArtShivanDragonEditionReleasedAfterTenthEditionNoPromo);

        // == Try same but with Released Before
        shivanDragonCard = this.cardDb.getCardFromEditionsReleasedBefore(this.cardNameShivanDragon, CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS,
                afterTenthEdition, legalInSetFilter);
        assertNotNull(shivanDragonCard);
        assertEquals(shivanDragonCard.getName(), cardNameShivanDragon);
        assertEquals(shivanDragonCard.getEdition(), originalArtShivanDragonEdition);

        // Original Art Should be excluded by date filter
        shivanDragonCard = this.cardDb.getCardFromEditionsReleasedBefore(this.cardNameShivanDragon, CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS,
                afterTenthEdition, legalInSetFilter);
        assertNotNull(shivanDragonCard);
        assertEquals(shivanDragonCard.getName(), cardNameShivanDragon);
        assertEquals(shivanDragonCard.getEdition(), originalArtShivanDragonEdition);
    }

    @Test void testCardRequestWithSetCodeAllInLowercase(){
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        PaperCard counterSpellCard = this.cardDb.getCard(this.cardNameCounterspell, "tmp");
        assertEquals(counterSpellCard.getEdition(), "TMP");
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
    }

    @Test void prepareTestCaseForSetPreferredArtTest(){
        String setCode = this.editionsCounterspell[0];
        int artIndex = 4;  // non-existing
        String cardRequest = CardDb.CardRequest.compose(this.cardNameCounterspell, setCode, artIndex);
        PaperCard nonExistingCounterSpell = this.cardDb.getCard(cardRequest);
        assertNull(nonExistingCounterSpell);
    }

    @Test void setPreferredArtForCard(){
        String cardName = "Mountain";
        String setCode = "3ED";
        int artIndex = 5;
        assertFalse(this.cardDb.setPreferredArt(cardName, setCode, artIndex));
        assertTrue(this.cardDb.setPreferredArt(cardName, setCode, 1));
    }


    @Test void testThatWithCardPreferenceSetAndNoRequestForSpecificEditionAlwaysReturnsPreferredArt(){
        String cardRequest = CardDb.CardRequest.compose("Island", "MIR", 3);
        PaperCard islandCard = this.cardDb.getCard(cardRequest);
        assertNotNull(islandCard);
        assertEquals(islandCard.getName(), "Island");
        assertEquals(islandCard.getEdition(), "MIR");
        assertEquals(islandCard.getArtIndex(), 3);

        // now set preferred art
        assertTrue(this.cardDb.setPreferredArt("Island", "MIR", 3));
        // Now requesting for a different Island
        cardRequest = CardDb.CardRequest.compose("Island", "TMP", 1);
        islandCard = this.cardDb.getCard(cardRequest);
        assertNotNull(islandCard);
        // Now card should be from the preferred art no matter the request
        assertEquals(islandCard.getName(), "Island");
        assertEquals(islandCard.getEdition(), "TMP");
        assertEquals(islandCard.getArtIndex(), 1);

        // Now just asking for an Island
        islandCard = this.cardDb.getCard("Island");
        assertNotNull(islandCard);
        assertEquals(islandCard.getName(), "Island");
        assertEquals(islandCard.getEdition(), "MIR");
        assertEquals(islandCard.getArtIndex(), 3);

        // Now asking for a foiled island - I will get the one from preferred art - but foiled
        cardRequest = CardDb.CardRequest.compose("Island", true);
        islandCard = this.cardDb.getCard(cardRequest);
        assertNotNull(islandCard);
        assertEquals(islandCard.getName(), "Island");
        assertEquals(islandCard.getEdition(), "MIR");
        assertEquals(islandCard.getArtIndex(), 3);
        assertTrue(islandCard.isFoil());
    }

    @Test void testGetDualAndDoubleCards(){
        String fireAndIce = "Fire // Ice";
        PaperCard fireAndIceCard = this.cardDb.getCard(fireAndIce);
        assertNotNull(fireAndIceCard);
        assertEquals(fireAndIceCard.getName(), fireAndIce);

        String deserterRansacker = "Afflicted Deserter // Werewolf Ransacker";
        PaperCard adCard = this.cardDb.getCard(deserterRansacker);
        assertNull(adCard);

        String deserterSide = "Afflicted Deserter";
        adCard = this.cardDb.getCard(deserterSide);
        assertNotNull(adCard);
        assertEquals(adCard.getName(), deserterSide);

        String werewolfSide = "Werewolf Ransacker";
        adCard = this.cardDb.getCard(werewolfSide);
        assertNotNull(adCard);
        assertEquals(adCard.getName(), deserterSide);
    }

}