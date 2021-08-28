package forge.card;

import forge.card.CardDb.CardRequest;
import forge.item.IPaperCard;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

@Test(timeOut = 1000, enabled = true)
public class CardRequestTestCase {

    private String cardName;
    private String edition;
    private String collNr;
    private String foilCardNameFoil;
    private String foilCardName;
    private String foilEdition;
    private String foilCollNr;
    private final char sep = CardDb.NameSetSeparator;

    @BeforeTest
    public void setup(){
        cardName = "Shivan Dragon";
        edition = "2ED";
        collNr = "175";

        foilCardName = "Lightning Dragon";
        foilCardNameFoil = "Lightning Dragon+";
        foilEdition = "PUSG";
        foilCollNr = "202";
    }

    public void testComposeCardNameAndSet(){
        // OK request
        String requestInfo = CardRequest.compose(cardName, edition);
        String expected = cardName + sep + edition;
        assertEquals(requestInfo, expected);

        // CardName null
        String requestCardNameNull = CardRequest.compose(null, edition);
        assertEquals(requestCardNameNull, sep + edition);

        // SetCode null
        String requestCardNameAndSetNull = CardRequest.compose(null, null);
        assertEquals(requestCardNameAndSetNull, "" + sep + "");

        // CardNameFoil
        String requestInfoFoil = CardRequest.compose(foilCardName, foilEdition);
        assertEquals(requestInfoFoil, foilCardName + sep + foilEdition);
    }

    public void testComposeCardNameSetAndArtIndex(){
        String requestInfo = CardRequest.compose(cardName, edition, 2);
        String expected = cardName + sep + edition + sep + 2;
        assertEquals(requestInfo, expected);

        // negative Art Index
        String requestNegativeArtIndex = CardRequest.compose(cardName, edition, -3);
        expected = cardName + sep + edition + sep + 1;
        assertEquals(requestNegativeArtIndex, expected);
    }

    public void testComposeCardNameSetAndCollectorNumber(){
        String requestInfo = CardRequest.compose(cardName, edition, collNr);
        String expCN = "[" + collNr + "]";
        String expected = cardName + sep + edition + sep + expCN;
        assertEquals(requestInfo, expected);

        // collNr only one bracket
        requestInfo = CardRequest.compose(cardName, edition, "["+collNr);
        assertEquals(requestInfo, expected);

        requestInfo = CardRequest.compose(cardName, edition, collNr+"]");
        assertEquals(requestInfo, expected);

        // collNr with leading spaces, as possible result from a wrong parsing in a deck file
        requestInfo = CardRequest.compose(cardName, edition, "\t\t 175   ");
        assertEquals(requestInfo, expected);

        // collNr is null
        requestInfo = CardRequest.compose(cardName, edition, null);
        assertEquals(requestInfo, cardName + sep + edition + sep);
    }

    public void testComposeFullRequest(){
        String requestInfo = CardRequest.compose(cardName, edition, 1, collNr);
        String expected = cardName + sep + edition + sep + 1 + sep + "[" + collNr + "]";
        assertEquals(requestInfo, expected);
    }

    public void testFromStringCardNameOnly(){
        CardRequest request = CardRequest.fromString(cardName);
        assertEquals(request.cardName, cardName);
        assertEquals(request.artIndex, IPaperCard.DEFAULT_ART_INDEX);
        assertNull(request.edition);
        assertEquals(request.collectorNumber, IPaperCard.NO_COLLECTOR_NUMBER);
    }

    public void testFromStringCardNameAndSetCode(){
        String requestString = cardName + sep + edition;
        CardRequest request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, cardName);
        assertEquals(request.edition, edition);
        assertEquals(request.artIndex, IPaperCard.DEFAULT_ART_INDEX);
        assertEquals(request.collectorNumber, IPaperCard.NO_COLLECTOR_NUMBER);

        // foil
        requestString = foilCardNameFoil + sep + foilEdition;
        request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, foilCardName);
        assertEquals(request.edition, foilEdition);
        assertEquals(request.artIndex, IPaperCard.DEFAULT_ART_INDEX);
        assertTrue(request.isFoil);
        assertEquals(request.collectorNumber, IPaperCard.NO_COLLECTOR_NUMBER);
    }

    public void testFromStringCardNameAndSetCodeAndArtIndex(){
        String requestString = cardName + sep + edition + sep + 2;
        CardRequest request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, cardName);
        assertEquals(request.edition, edition);
        assertEquals(request.artIndex, 2);
        assertEquals(request.collectorNumber, IPaperCard.NO_COLLECTOR_NUMBER);

        // ArtIndex not valid (as in >= 10) - supposed to be a single digit
        requestString = cardName + sep + edition + sep + 20;
        request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, cardName);
        assertEquals(request.edition, edition);
        assertEquals(request.artIndex, 20);
        assertEquals(request.collectorNumber, IPaperCard.NO_COLLECTOR_NUMBER);


        // foil
        requestString = foilCardNameFoil + sep + foilEdition + sep + IPaperCard.DEFAULT_ART_INDEX;
        request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, foilCardName);
        assertEquals(request.edition, foilEdition);
        assertEquals(request.artIndex, IPaperCard.DEFAULT_ART_INDEX);
        assertTrue(request.isFoil);
        assertEquals(request.collectorNumber, IPaperCard.NO_COLLECTOR_NUMBER);
    }

    public void testFromStringCardNameAndSetCodeAndCollectorNumber(){
        String requestString = cardName + sep + edition + sep + "[" + collNr + "]";
        CardRequest request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, cardName);
        assertEquals(request.edition, edition);
        assertEquals(request.artIndex, IPaperCard.NO_ART_INDEX);
        assertEquals(request.collectorNumber, collNr);

        // Not wrapped collNr
        requestString = cardName + sep + edition + sep + collNr;
        request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, cardName);
        assertEquals(request.edition, edition);
        assertEquals(request.artIndex, IPaperCard.DEFAULT_ART_INDEX);
        assertEquals(request.collectorNumber, IPaperCard.NO_COLLECTOR_NUMBER);

        // foil
        requestString = foilCardNameFoil + sep + foilEdition + sep + "[" + foilCollNr + "]";
        request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, foilCardName);
        assertEquals(request.edition, foilEdition);
        assertEquals(request.artIndex, IPaperCard.NO_ART_INDEX);
        assertTrue(request.isFoil);
        assertEquals(request.collectorNumber, foilCollNr);
    }

    public void fromStringFullInfo(){
        String requestString = cardName + sep + edition + sep + 2 + sep + "[" + collNr + "]";
        CardRequest request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, cardName);
        assertEquals(request.edition, edition);
        assertEquals(request.artIndex, 2);
        assertEquals(request.collectorNumber, collNr);

        // collNr not wrapped in brackets
        requestString = cardName + sep + edition + sep + 2 + sep + collNr;
        request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, cardName);
        assertEquals(request.edition, edition);
        assertEquals(request.artIndex, 2);
        assertEquals(request.collectorNumber, IPaperCard.NO_COLLECTOR_NUMBER);

        // foil
        requestString = foilCardNameFoil + sep + foilEdition + sep + 3 + sep +"[" + foilCollNr + "]";
        request = CardRequest.fromString(requestString);
        assertEquals(request.cardName, foilCardName);
        assertEquals(request.edition, foilEdition);
        assertEquals(request.artIndex,3);
        assertTrue(request.isFoil);
        assertEquals(request.collectorNumber, foilCollNr);
    }

    @Test
    public void testCreatingCardRequestUsingAnotherRequestStringAsCardName(){
        String requestString = CardRequest.compose(cardName, edition, 1);
        CardRequest request = CardRequest.fromString(requestString);

        assertEquals(request.cardName, cardName);
        assertEquals(request.edition, edition);
        assertEquals(request.artIndex, 1);

        String newRequestString = CardRequest.compose(requestString, foilEdition, 2);
        CardRequest newRequest = CardRequest.fromString(newRequestString);

        assertEquals(newRequest.cardName, cardName);
        assertEquals(newRequest.edition, foilEdition);
        assertEquals(newRequest.artIndex, 2);

        assertEquals(request.cardName, newRequest.cardName);
        assertNotEquals(request.edition, newRequest.edition);
        assertNotEquals(request.artIndex, newRequest.artIndex);
    }

    @Test
    public void testCreatingCardRequestWithArtIndexGreaterThanNine(){
        String requestString = CardRequest.compose("Island", "SLD", 13);
        CardRequest request = CardRequest.fromString(requestString);

        assertEquals(request.cardName, "Island");
        assertEquals(request.edition, "SLD");
        assertEquals(request.artIndex, 13);
        assertEquals(request.collectorNumber, IPaperCard.NO_COLLECTOR_NUMBER);
    }

}