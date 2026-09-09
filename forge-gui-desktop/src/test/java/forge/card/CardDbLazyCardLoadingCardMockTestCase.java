package forge.card;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.StaticData;
import forge.gamesimulationtests.util.CardDatabaseHelper;
import forge.item.PaperCard;
import forge.model.FModel;

public class CardDbLazyCardLoadingCardMockTestCase extends CardMockTestCase {

    protected CardDb cardDb;

    @BeforeMethod
    public void setup() {
        StaticData data = FModel.getMagicDb();
        this.cardDb = data.getCommonCards();
    }

    @Override
    protected void initializeStaticData() {
        // A database of this class's own, loaded lazily. The process-wide one may already
        // have been built eagerly by whichever test class ran first, which would make every
        // assertNull below fail.
        StaticData data = CardDatabaseHelper.createStaticData("CardDbLazyCardLoadingCardMockTestCase", true);
        fModelMock.when(FModel::getMagicDb).thenReturn(data);
    }

    @Test
    public void testLoadAndGetBorrowing100_000ArrowsCardFromAllEditions() {
        String cardName = "Borrowing 100,000 Arrows";
        String[] allAvailableEds = new String[] { "PTK", "ME3", "C13", "CMA", "A25", "PLST" };

        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        PaperCard borrowingCard = this.cardDb.getCard(cardName);
        assertNull(borrowingCard);

        // Load the Card (just card name
        FModel.getMagicDb().attemptToLoadCard(cardName);

        borrowingCard = this.cardDb.getCard(cardName);
        assertNotNull(borrowingCard);
        assertEquals(borrowingCard.getName(), cardName);
        assertEquals(borrowingCard.getEdition(), "PLST");

        // Now get card from all the specified editions
        for (String setCode : allAvailableEds) {
            borrowingCard = this.cardDb.getCard(cardName, setCode);
            assertNotNull(borrowingCard);
            assertEquals(borrowingCard.getName(), cardName);
            assertEquals(borrowingCard.getEdition(), setCode);
        }
    }

    @Test
    public void testLoadAndGetAinokBondKinFromKTKWithCaseInsensitiveCardName() {
        String cardName = "aiNOk Bond-kin"; // wrong case
        String expectedCardName = "Ainok Bond-Kin";
        String setCode = "KTK";

        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        PaperCard borrowingCard = this.cardDb.getCard(cardName);
        assertNull(borrowingCard);

        // Load the Card (just card name
        FModel.getMagicDb().attemptToLoadCard(cardName, setCode);

        borrowingCard = this.cardDb.getCard(cardName);
        assertNotNull(borrowingCard);
        assertEquals(borrowingCard.getName(), expectedCardName);
        assertEquals(borrowingCard.getEdition(), setCode);

        // The card is now in the DB so we can update this test
        CardEdition ima = FModel.getMagicDb().getCardEdition("IMA");
        assertNotNull(ima);
        assertNull(this.cardDb.getCardFromSet(expectedCardName, ima, false));
        // And the lenient call falls back to the one printing that was loaded.
        assertEquals(this.cardDb.getCard(cardName, "IMA").getEdition(), setCode);
    }

    @Test
    public void tesLoadAndGetAetherVialWithWrongCase() {
        String cardName = "AEther vial"; // wrong case
        String expectedCardName = "Aether Vial";
        PaperCard aetherVialCard = this.cardDb.getCard(cardName);
        assertNull(aetherVialCard);

        // Load the Card (just card name
        FModel.getMagicDb().attemptToLoadCard(cardName);

        aetherVialCard = this.cardDb.getCard(cardName);
        assertNotNull(aetherVialCard);
        assertEquals(aetherVialCard.getName(), expectedCardName);
    }

    @Test
    public void tesLoadAndGetUnsupportedCardHavingWrongSetCode() {
        String cardName = "Dominating Licid";
        String wrongSetCode = "AA";
        String expectedSetCode = "EXO"; // Exodus
        CardRarity expectedCardRarity = CardRarity.Rare;

        PaperCard dominatingLycidCard = this.cardDb.getCard(cardName);
        assertNull(dominatingLycidCard);

        // Load the Card (just card name
        FModel.getMagicDb().attemptToLoadCard(cardName, wrongSetCode);

        dominatingLycidCard = this.cardDb.getCard(cardName);
        assertNotNull(dominatingLycidCard);
        assertEquals(dominatingLycidCard.getName(), cardName);
        assertEquals(dominatingLycidCard.getEdition(), expectedSetCode);
        assertEquals(dominatingLycidCard.getRarity(), expectedCardRarity);
    }
}
