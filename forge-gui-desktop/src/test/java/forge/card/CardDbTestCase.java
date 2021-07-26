package forge.card;

import forge.StaticData;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.model.FModel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    protected final String oldFrameShivanDragonEdition = "LEA";
    protected final String newFrameShivanDragonEdition = "M20";

    protected final String oldFrameLightningDragonEdition = "USG";
    protected final String oldFrameLightningDragonEditionNoPromo = "USG";

    protected final String newFrameLightningDragonEdition = "VMA";
    protected final String newFrameLightningDragonEditionNoPromo = "USG";

    protected final String newFrameHymnToTourachEdition = "EMA";
    protected final String newFrameHymnToTourachEditionNoPromo = "EMA";
    protected final String oldFrameHymnToTourachEdition = "FEM";
    protected final String oldFrameHymnToTourachEditionNoPromo = "FEM";

    // Test Dates and Editions
    protected final String printedBeforeFromTheVaultDate = "2008-10-01";
    protected final String latestFrameShivanDragonEditionBefore = "DRB";
    protected final String latestFrameShivanDragonEditionBeforeNoPromo = "10E";
    protected final String latestFrameLightningDragonEditionBefore = "MBP";
    protected final String latestFrameLightningDragonEditionBeforeNoPromo = "USG";
    protected final String printedBeforeEternalMasters = "2015-01-01";
    protected final String latestFrameHymnToTourachEditionBefore = "VMA";
    protected final String latestFrameHymnToTourachEditionBeforeNoPromo = "FEM";

    // Get a card that has lots of editions so that we can test fetching for specific editions and print dates
    protected final String cardNameCounterspell = "Counterspell";
    protected final String[] editionsCounterspell = {"3ED", "4ED", "ICE", "5ED", "TMP", "S99", "MMQ", "A25", "MH2"};
    protected final String counterspellPrintedBeforeMasters25 = "2018-03-15";  // One day before Master25 release
    protected final String[] counterspellLatestBeforeMasters25 = {"MPS_AKH", "EMA"};
    protected final String counterspellPrintedBeforeEternalMasters = "2016-06-09";  // One day before Eternal Masters release
    protected final String[] counterspellLatestBeforeEternalMasters = {"TPR", "7ED"};

    @BeforeMethod
    public void setup(){
        StaticData data = FModel.getMagicDb();
        this.cardDb = data.getCommonCards();
        this.legacyCardDb = new LegacyCardDb(data.getCommonCards().getAllCards(), data.getEditions());
    }

    @Test
    public void testGetCardbyName() {
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
    public void testGetCardFromEditionsWithCardNameAndFramePreference() {
        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference frame = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;

        PaperCard sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), newFrameShivanDragonEdition);

        PaperCard ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        assertEquals(ldCard.getName(), cardNameLightningDragon);
        assertEquals(ldCard.getEdition(), newFrameLightningDragonEdition);

        // foiled card request
        PaperCard ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), newFrameLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());

        PaperCard httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), newFrameHymnToTourachEdition);

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        frame = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), newFrameShivanDragonEdition);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        assertEquals(ldCard.getName(), cardNameLightningDragon);
        assertEquals(ldCard.getEdition(), newFrameLightningDragonEditionNoPromo);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), newFrameLightningDragonEditionNoPromo);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), newFrameHymnToTourachEditionNoPromo);

        /* --------------
            Old Print
           -------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), oldFrameShivanDragonEdition);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        assertEquals(ldCard.getName(), cardNameLightningDragon);
        assertEquals(ldCard.getEdition(), oldFrameLightningDragonEdition);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), oldFrameLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), oldFrameHymnToTourachEdition);

        /* --------------------
            Old Print No Promo
         ----------------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), oldFrameShivanDragonEdition);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame);
        assertEquals(ldCard.getName(), cardNameLightningDragon);
        assertEquals(ldCard.getEdition(), oldFrameLightningDragonEditionNoPromo);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), oldFrameLightningDragonEditionNoPromo);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), oldFrameHymnToTourachEditionNoPromo);
    }

    @Test
    public void testGetCardFromEditionsWithCardNameAndFramePreferenceComparedWithLegacy() {
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
    public void testGetCardFromEditionsWithCardNameAndFramePreferenceWithDate() {
        // Set Reference Dates
        Date sdReleaseDate = null;
        Date httReleaseDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            sdReleaseDate = format.parse(printedBeforeFromTheVaultDate);
            httReleaseDate = format.parse(printedBeforeEternalMasters);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference frame = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;

        PaperCard sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestFrameShivanDragonEditionBefore);

        // foiled card request
        PaperCard ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, sdReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestFrameLightningDragonEditionBefore);
        assertTrue(ldFoilCard.isFoil());

        PaperCard httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, httReleaseDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestFrameHymnToTourachEditionBefore);

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        frame = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestFrameShivanDragonEditionBeforeNoPromo);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, sdReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestFrameLightningDragonEditionBeforeNoPromo);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, httReleaseDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestFrameHymnToTourachEditionBeforeNoPromo);

         /* --------------
            Old Print
           -------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), oldFrameShivanDragonEdition);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, sdReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), oldFrameLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, httReleaseDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), oldFrameHymnToTourachEdition);

        /* --------------------
            Old Print No Promo
         ----------------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), oldFrameShivanDragonEdition);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, sdReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), oldFrameLightningDragonEditionNoPromo);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, httReleaseDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), oldFrameHymnToTourachEditionNoPromo);
    }

    @Test
    public void testGetCardFromEditionsWithCardNameAndFramePreferenceWithDateCompareWithLegacy() {
        // Set Reference Dates
        Date sdReleaseDate = null;
        Date httReleaseDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            sdReleaseDate = format.parse(printedBeforeFromTheVaultDate);
            httReleaseDate = format.parse(printedBeforeEternalMasters);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference frame = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;
        LegacyCardDb.LegacySetPreference setPref = LegacyCardDb.LegacySetPreference.Latest;

        PaperCard sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        PaperCard sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), latestFrameShivanDragonEditionBefore);
        assertEquals(sdCard, sdCardLegacy);

        PaperCard ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame, sdReleaseDate);
        PaperCard ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), latestFrameLightningDragonEditionBefore);
        assertEquals(ldCard, ldCardLegacy);

        PaperCard httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, httReleaseDate);
        PaperCard httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref);
        assertEquals(httCard.getEdition(), latestFrameHymnToTourachEditionBefore);
        assertEquals(httCard, httCardLegacy);

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        frame = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        setPref = LegacyCardDb.LegacySetPreference.LatestCoreExp;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), latestFrameShivanDragonEditionBeforeNoPromo);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), latestFrameLightningDragonEditionBeforeNoPromo);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, httReleaseDate);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref);
        assertEquals(httCard.getEdition(), latestFrameHymnToTourachEditionBeforeNoPromo);
        assertEquals(httCard, httCardLegacy);

        /* --------------
            Old Print
           -------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;
        setPref = LegacyCardDb.LegacySetPreference.Earliest;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), oldFrameShivanDragonEdition);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), oldFrameLightningDragonEdition);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, httReleaseDate);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref);
        assertEquals(httCard.getEdition(), oldFrameHymnToTourachEdition);
        assertEquals(httCard, httCardLegacy);

        /* --------------------
            Old Print No Promo
         ----------------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        setPref = LegacyCardDb.LegacySetPreference.EarliestCoreExp;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), oldFrameShivanDragonEdition);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), oldFrameLightningDragonEditionNoPromo);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, httReleaseDate);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref);
        assertEquals(httCard.getEdition(), oldFrameHymnToTourachEditionNoPromo);
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
        assertEquals(httCard.getEdition(), newFrameHymnToTourachEdition);
        assertEquals(httCard.getArtIndex(), 1);

        // foil card
        PaperCard ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), newFrameLightningDragonEdition);
        assertEquals(ldFoilCard.getArtIndex(), 1);
        assertTrue(ldFoilCard.isFoil());

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        frame = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, 1);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getArtIndex(), 1);
        assertEquals(httCard.getEdition(), newFrameHymnToTourachEditionNoPromo);

        // foil card
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), newFrameLightningDragonEditionNoPromo);
        assertEquals(ldFoilCard.getArtIndex(), 1);
        assertTrue(ldFoilCard.isFoil());

        /* --------------
            Old Print
           -------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, artIdx);
            assertEquals(httCard.getName(), cardNameHymnToTourach);
            assertEquals(httCard.getEdition(), oldFrameHymnToTourachEdition);
            assertEquals(httCard.getArtIndex(), artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
        }

        // foil card
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), oldFrameLightningDragonEdition);
        assertEquals(ldFoilCard.getArtIndex(), 1);
        assertTrue(ldFoilCard.isFoil());

        /* --------------------
            Old Print No Promo
         ----------------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, artIdx);
            assertEquals(httCard.getName(), cardNameHymnToTourach);
            assertEquals(httCard.getEdition(), oldFrameHymnToTourachEdition);
            assertEquals(httCard.getArtIndex(), artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
        }

        // foil card
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), oldFrameLightningDragonEditionNoPromo);
        assertEquals(ldFoilCard.getArtIndex(), 1);
        assertTrue(ldFoilCard.isFoil());
    }

    @Test
    public void testGetCardFromEditionsWithCardNameAndFramePreferenceWithDateAndArtIndex() {
        /* NOTE:
         testing case of errors here - will do in a separate test.
         */

        // Set Reference Dates
        Date sdReleaseDate = null;
        Date httReleaseDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            sdReleaseDate = format.parse(printedBeforeFromTheVaultDate);
            httReleaseDate = format.parse(printedBeforeEternalMasters);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference frame = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;

        PaperCard sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, 1, sdReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestFrameShivanDragonEditionBefore);

        // foiled card request
        PaperCard ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1, sdReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestFrameLightningDragonEditionBefore);
        assertTrue(ldFoilCard.isFoil());

        PaperCard httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, 1, httReleaseDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestFrameHymnToTourachEditionBefore);

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        frame = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), latestFrameShivanDragonEditionBeforeNoPromo);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1, sdReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), latestFrameLightningDragonEditionBeforeNoPromo);
        assertTrue(ldFoilCard.isFoil());

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, 1, httReleaseDate);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), latestFrameHymnToTourachEditionBeforeNoPromo);

        /* --------------
            Old Print
           -------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, 1, sdReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), oldFrameShivanDragonEdition);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, 1, sdReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), oldFrameLightningDragonEdition);
        assertTrue(ldFoilCard.isFoil());

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, artIdx, httReleaseDate);
            assertEquals(httCard.getName(), cardNameHymnToTourach);
            assertEquals(httCard.getEdition(), oldFrameHymnToTourachEdition);
            assertEquals(httCard.getArtIndex(), artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
        }

        /* --------------------
            Old Print No Promo
         ----------------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        assertEquals(sdCard.getName(), cardNameShivanDragon);
        assertEquals(sdCard.getEdition(), oldFrameShivanDragonEdition);

        // foiled card request
        ldFoilCard = this.cardDb.getCardFromEditions(cardNameFoilLightningDragon, frame, sdReleaseDate);
        assertEquals(ldFoilCard.getName(), cardNameLightningDragon);
        assertEquals(ldFoilCard.getEdition(), oldFrameLightningDragonEditionNoPromo);
        assertTrue(ldFoilCard.isFoil());

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, artIdx, httReleaseDate);
            assertEquals(httCard.getName(), cardNameHymnToTourach);
            assertEquals(httCard.getEdition(), oldFrameHymnToTourachEditionNoPromo);
            assertEquals(httCard.getArtIndex(), artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
        }
    }

    @Test
    public void testGetCardFromEditionsWithCardNameAndFramePreferenceWithDateAndArtIndexComparedWithLegacy() {
        // Set Reference Dates
        Date sdReleaseDate = null;
        Date httReleaseDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            sdReleaseDate = format.parse(printedBeforeFromTheVaultDate);
            httReleaseDate = format.parse(printedBeforeEternalMasters);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        /* --------------
            Latest Print
           -------------*/
        CardDb.CardArtPreference frame = CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS;
        LegacyCardDb.LegacySetPreference setPref = LegacyCardDb.LegacySetPreference.Latest;

        PaperCard sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, 1, sdReleaseDate);
        PaperCard sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref, 1);
        assertEquals(sdCard.getEdition(), latestFrameShivanDragonEditionBefore);
        assertEquals(sdCard, sdCardLegacy);

        PaperCard ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame, 1, sdReleaseDate);
        PaperCard ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref, 1);
        assertEquals(ldCard.getEdition(), latestFrameLightningDragonEditionBefore);
        assertEquals(ldCard, ldCardLegacy);

        PaperCard httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, 1, httReleaseDate);
        PaperCard httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref, 1);
        assertEquals(httCard.getEdition(), latestFrameHymnToTourachEditionBefore);
        assertEquals(httCard, httCardLegacy);

        /* ----------------------
            Latest Print No Promo
           ----------------------*/
        frame = CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        setPref = LegacyCardDb.LegacySetPreference.LatestCoreExp;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, 1, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref, 1);
        assertEquals(sdCard.getEdition(), latestFrameShivanDragonEditionBeforeNoPromo);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame, 1, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref, 1);
        assertEquals(ldCard.getEdition(), latestFrameLightningDragonEditionBeforeNoPromo);
        assertEquals(ldCard, ldCardLegacy);

        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, 1, httReleaseDate);
        httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref, 1);
        assertEquals(httCard.getEdition(), latestFrameHymnToTourachEditionBeforeNoPromo);
        assertEquals(httCard, httCardLegacy);

        /* --------------
            Old Print
           -------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS;
        setPref = LegacyCardDb.LegacySetPreference.Earliest;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, 1, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref, 1);
        assertEquals(sdCard.getEdition(), oldFrameShivanDragonEdition);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame, 1, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref, 1);
        assertEquals(ldCard.getEdition(), oldFrameLightningDragonEdition);
        assertEquals(ldCard, ldCardLegacy);

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, artIdx, httReleaseDate);
            httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref, artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
            assertEquals(httCard, httCardLegacy);
        }

        /* --------------------
            Old Print No Promo
         ----------------------*/
        frame = CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        setPref = LegacyCardDb.LegacySetPreference.EarliestCoreExp;

        sdCard = this.cardDb.getCardFromEditions(cardNameShivanDragon, frame, sdReleaseDate);
        sdCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameShivanDragon, sdReleaseDate, setPref);
        assertEquals(sdCard.getEdition(), oldFrameShivanDragonEdition);
        assertEquals(sdCard, sdCardLegacy);

        ldCard = this.cardDb.getCardFromEditions(cardNameLightningDragon, frame, sdReleaseDate);
        ldCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameLightningDragon, sdReleaseDate, setPref);
        assertEquals(ldCard.getEdition(), oldFrameLightningDragonEditionNoPromo);
        assertEquals(ldCard, ldCardLegacy);

        for (int artIdx = 1; artIdx <= 4; artIdx++) {
            httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, frame, artIdx, httReleaseDate);
            httCardLegacy = this.legacyCardDb.getCardFromEdition(cardNameHymnToTourach, httReleaseDate, setPref, artIdx);
            assertEquals(httCard.getCollectorNumber(), collectorNumbersHymnToTourach[artIdx-1]);
            assertEquals(httCard, httCardLegacy);
        }
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

        Date beforeMaster25Date = null;
        Date beforeEternalMastersDate = null;

        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            beforeMaster25Date = format.parse(counterspellPrintedBeforeMasters25);
            beforeEternalMastersDate = format.parse(counterspellPrintedBeforeEternalMasters);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        // Before Master25
        PaperCard counterSpellCard = null;
        // All Editions
        counterSpellCard = this.cardDb.getCardFromEditions(cardNameCounterspell,
                CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS, beforeMaster25Date);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestBeforeMasters25[0]);

        counterSpellCard = this.cardDb.getCardFromEditions(cardNameCounterspell,
                CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS, beforeEternalMastersDate);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestBeforeEternalMasters[0]);

        // No Promo
        counterSpellCard = this.cardDb.getCardFromEditions(cardNameCounterspell,
                CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY, beforeMaster25Date);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestBeforeMasters25[1]);

        counterSpellCard = this.cardDb.getCardFromEditions(cardNameCounterspell,
                CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY, beforeEternalMastersDate);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestBeforeEternalMasters[1]);

        // Now with setting preferences - so going with default cardArt preference.

        this.cardDb.setCardArtPreference(true, false);
        counterSpellCard = this.cardDb.getCardFromEditions(cardNameCounterspell, beforeMaster25Date);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestBeforeMasters25[0]);

        counterSpellCard = this.cardDb.getCardFromEditions(cardNameCounterspell, beforeEternalMastersDate);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestBeforeEternalMasters[0]);

        this.cardDb.setCardArtPreference(true, true);
        counterSpellCard = this.cardDb.getCardFromEditions(cardNameCounterspell, beforeMaster25Date);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestBeforeMasters25[1]);

        counterSpellCard = this.cardDb.getCardFromEditions(cardNameCounterspell, beforeEternalMastersDate);
        assertEquals(counterSpellCard.getName(), cardNameCounterspell);
        assertEquals(counterSpellCard.getEdition(), counterspellLatestBeforeEternalMasters[1]);

        // restore default
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

    @Test
    public void testGetCardFromEditionsWrongInputReturnsNull() {
        Date preMagicReleaseDate = null;
        Date httPrereleaseDate = null;
        try {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            preMagicReleaseDate = format.parse("1993-08-05");
            httPrereleaseDate = format.parse(printedBeforeEternalMasters);
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }

        PaperCard nullCard;
        PaperCard shivanNotExistingDragon;
        for (CardDb.CardArtPreference preference : CardDb.CardArtPreference.values()) {
            nullCard = this.cardDb.getCardFromEditions("ImaginaryMagicCard", preference);
            assertNull(nullCard);

            nullCard = this.cardDb.getCardFromEditions(null, preference);
            assertNull(nullCard);

            shivanNotExistingDragon = this.cardDb.getCardFromEditions(cardNameShivanDragon, preference, 2);
            assertNull(shivanNotExistingDragon);

            shivanNotExistingDragon = this.cardDb.getCardFromEditions(cardNameShivanDragon, preference, preMagicReleaseDate);
            assertNull(shivanNotExistingDragon);

            nullCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, preference, 5, httPrereleaseDate);
            assertNull(nullCard);
        }

        // Passing null preference
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        PaperCard httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, null, null);
        assertNotNull(httCard);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), newFrameHymnToTourachEdition);

        // Changing default value for default card art preference
        this.cardDb.setCardArtPreference(false, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach, null, null);
        assertNotNull(httCard);
        assertEquals(httCard.getName(), cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), oldFrameHymnToTourachEdition);
        // restore default
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

    @Test
    public void testGetCardFromEditionsUsingDefaultCardArtPreference(){
        // Test default value first
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
        PaperCard shivanDragonCard = this.cardDb.getCardFromEditions(cardNameShivanDragon);
        assertEquals(shivanDragonCard.getEdition(), newFrameShivanDragonEdition);

        // Try changing the policy
        this.cardDb.setCardArtPreference(false, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
        shivanDragonCard = this.cardDb.getCardFromEditions(cardNameShivanDragon);
        assertEquals(shivanDragonCard.getEdition(), oldFrameShivanDragonEdition);
        // restore default
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

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
        assertEquals(httCard.getEdition(), newFrameHymnToTourachEdition);
        // Try changing the policy
        this.cardDb.setCardArtPreference(false, true);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);
        httCard = this.cardDb.getCardFromEditions(cardNameHymnToTourach);
        assertEquals(httCard.getEdition(), oldFrameHymnToTourachEditionNoPromo);
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
        assertTrue(nonExistingCardArtCount == 0);

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
    public void testCardsAlwaysReturnedEvenIfCardArtPreferenceIsTooStrict(){
        // REFERENCE CASE - NO FILTER
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        PaperCard atog = this.cardDb.getCard("Atog");
        assertNotNull(atog);
        assertEquals(atog.getEdition(), "ME4");  // Game Night

        PaperCard militantAngel = this.cardDb.getCard("Militant Angel");
        assertNotNull(militantAngel);
        assertEquals(militantAngel.getEdition(), "GNT");  // Game Night

        // Loyal Unicorn: Available in Forge in The List and COMMANDER 2018
        PaperCard loyalUnicorn = this.cardDb.getCard("Loyal Unicorn");
        assertNotNull(loyalUnicorn);
        assertEquals(loyalUnicorn.getEdition(), "PLIST");  // The List

        // Selfless Squire: Available in Forge in COMMANDER 2021; Treasure Chest; COMMANDER 2016
        PaperCard selflessSquire = this.cardDb.getCard("Selfless Squire");
        assertNotNull(selflessSquire);
        assertEquals(selflessSquire.getEdition(), "C21");  // The List

        // Set Strictness to Expansions and Reprint Only (LATEST)
        this.cardDb.setCardArtPreference(true, true);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        // ONLY CHANGE HERE IS FOR ATOG
        atog = this.cardDb.getCard("Atog");
        assertNotNull(atog);
        assertEquals(atog.getEdition(), "MRD");  // Game Night

        militantAngel = this.cardDb.getCard("Militant Angel");
        assertNotNull(militantAngel);
        assertEquals(militantAngel.getEdition(), "GNT");  // Game Night

        // Loyal Unicorn: Available in Forge in The List and COMMANDER 2018
        loyalUnicorn = this.cardDb.getCard("Loyal Unicorn");
        assertNotNull(loyalUnicorn);
        assertEquals(loyalUnicorn.getEdition(), "PLIST");  // The List

        // Selfless Squire: Available in Forge in COMMANDER 2021; Treasure Chest; COMMANDER 2016
        selflessSquire = this.cardDb.getCard("Selfless Squire");
        assertNotNull(selflessSquire);
        assertEquals(selflessSquire.getEdition(), "C21");  // The List

        // Set Strictness to ORIGINAL ART NO FILTER - Ref case
        this.cardDb.setCardArtPreference(false, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);

        // ONLY CHANGE HERE IS FOR ATOG
        atog = this.cardDb.getCard("Atog");
        assertNotNull(atog);
        assertEquals(atog.getEdition(), "ATQ");  // Game Night

        militantAngel = this.cardDb.getCard("Militant Angel");
        assertNotNull(militantAngel);
        assertEquals(militantAngel.getEdition(), "GNT");

        // Loyal Unicorn: Available in Forge in The List and COMMANDER 2018
        loyalUnicorn = this.cardDb.getCard("Loyal Unicorn");
        assertNotNull(loyalUnicorn);
        assertEquals(loyalUnicorn.getEdition(), "C18");

        // Selfless Squire: Available in Forge in COMMANDER 2021; Treasure Chest; COMMANDER 2016
        selflessSquire = this.cardDb.getCard("Selfless Squire");
        assertNotNull(selflessSquire);
        assertEquals(selflessSquire.getEdition(), "C16");

        // Set Strictness to ORIGINAL ART NO FILTER - Ref case
        this.cardDb.setCardArtPreference(false, true);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY);

        // ONLY CHANGE HERE IS FOR ATOG
        atog = this.cardDb.getCard("Atog");
        assertNotNull(atog);
        assertEquals(atog.getEdition(), "ATQ");  // Game Night

        militantAngel = this.cardDb.getCard("Militant Angel");
        assertNotNull(militantAngel);
        assertEquals(militantAngel.getEdition(), "GNT");

        // Loyal Unicorn: Available in Forge in The List and COMMANDER 2018
        loyalUnicorn = this.cardDb.getCard("Loyal Unicorn");
        assertNotNull(loyalUnicorn);
        assertEquals(loyalUnicorn.getEdition(), "PLIST");  // This is returned as this is a REPRINT Set!!

        // Selfless Squire: Available in Forge in COMMANDER 2021; Treasure Chest; COMMANDER 2016
        selflessSquire = this.cardDb.getCard("Selfless Squire");
        assertNotNull(selflessSquire);
        assertEquals(selflessSquire.getEdition(), "C16");

        // Set Art Preference back to default
        this.cardDb.setCardArtPreference(true, false);
        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
    }

}

