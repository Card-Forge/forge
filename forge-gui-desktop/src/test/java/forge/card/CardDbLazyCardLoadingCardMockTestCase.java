package forge.card;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
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
        // A database of this class's own, loaded lazily, and a fresh one for every method.
        //
        // Every test here asserts a card is not yet loaded, loads it, then asserts it is, so a
        // database shared between methods stops being pristine as soon as the first one runs.
        // This deliberately does not use the keyed cache CardDatabaseHelper offers the other
        // CardDb test classes: lazy loading only indexes card names rather than parsing all
        // 33,000 card scripts, so rebuilding it per method costs well under a second.
        StaticData data = CardDatabaseHelper.createStaticData(true);
        fModelMock.when(FModel::getMagicDb).thenReturn(data);
    }

    @Test
    public void testLoadAndGetBorrowing100_000ArrowsCardFromAllEditions() {
        String cardName = "Borrowing 100,000 Arrows";
        String[] allAvailableEds = new String[] { "PTK", "ME3", "C13", "CMA", "A25", "PLST" };

        assertEquals(this.cardDb.getCardArtPreference(), CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);

        // #11763 made every CardDb lookup load the card on demand, so getCard() no longer
        // reports whether a card has been loaded yet. contains() reads the loaded-card index
        // without triggering a load, which is what this pre-condition has always meant.
        assertFalse(this.cardDb.contains(cardName));

        // Load the Card (just card name
        FModel.getMagicDb().attemptToLoadCard(cardName);

        PaperCard borrowingCard = this.cardDb.getCard(cardName);
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

        // #11763 made every CardDb lookup load the card on demand, so getCard() no longer
        // reports whether a card has been loaded yet. contains() reads the loaded-card index
        // without triggering a load, which is what this pre-condition has always meant.
        assertFalse(this.cardDb.contains(cardName));

        // Load the Card (just card name
        FModel.getMagicDb().attemptToLoadCard(cardName, setCode);

        PaperCard borrowingCard = this.cardDb.getCard(cardName);
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
        // #11763 made every CardDb lookup load the card on demand, so getCard() no longer
        // reports whether a card has been loaded yet. contains() reads the loaded-card index
        // without triggering a load, which is what this pre-condition has always meant.
        assertFalse(this.cardDb.contains(cardName));

        // Load the Card (just card name
        FModel.getMagicDb().attemptToLoadCard(cardName);

        PaperCard aetherVialCard = this.cardDb.getCard(cardName);
        assertNotNull(aetherVialCard);
        assertEquals(aetherVialCard.getName(), expectedCardName);
    }

    @Test
    public void tesLoadAndGetUnsupportedCardHavingWrongSetCode() {
        String cardName = "Dominating Licid";
        String wrongSetCode = "AA";
        String expectedSetCode = "EXO"; // Exodus
        CardRarity expectedCardRarity = CardRarity.Rare;

        // #11763 made every CardDb lookup load the card on demand, so getCard() no longer
        // reports whether a card has been loaded yet. contains() reads the loaded-card index
        // without triggering a load, which is what this pre-condition has always meant.
        assertFalse(this.cardDb.contains(cardName));

        // Load the Card (just card name
        FModel.getMagicDb().attemptToLoadCard(cardName, wrongSetCode);

        PaperCard dominatingLycidCard = this.cardDb.getCard(cardName);
        assertNotNull(dominatingLycidCard);
        assertEquals(dominatingLycidCard.getName(), cardName);
        assertEquals(dominatingLycidCard.getEdition(), expectedSetCode);
        assertEquals(dominatingLycidCard.getRarity(), expectedCardRarity);
    }
}
