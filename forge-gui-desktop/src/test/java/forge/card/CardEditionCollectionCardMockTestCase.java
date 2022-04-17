package forge.card;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.model.FModel;

public class CardEditionCollectionCardMockTestCase extends CardMockTestCase {

    @Test
    public void testGetTheLatestOfAllTheOriginalEditionsOfCardsInPoolWithOriginalSets(){
        CardEdition.Collection editions = FModel.getMagicDb().getEditions();

        CardDb cardDb = FModel.getMagicDb().getCommonCards();
        String[] cardNames = {"Shivan Dragon", "Animate Wall", "Balance", "Blessing", "Force of Will"};
        String[] expectedSets = {"LEA", "LEA", "LEA", "LEA", "ALL"};
        List<PaperCard> cards = new ArrayList<>();
        for (int i=0; i < 5; i++){
            String cardName = cardNames[i];
            String expectedSet = expectedSets[i];
            PaperCard card = cardDb.getCardFromEditions(cardName, CardDb.CardArtPreference.ORIGINAL_ART_ALL_EDITIONS);
            assertEquals(card.getEdition(), expectedSet);
            cards.add(card);
        }

        CardPool pool = new CardPool();
        pool.add(cards);
        CardEdition ed = editions.getTheLatestOfAllTheOriginalEditionsOfCardsIn(pool);
        assertEquals(ed.getCode(), "ALL");
    }

    @Test
    public void testGetTheLatestOfAllTheOriginalEditionsOfCardsInPoolWithLatestArtSets(){
        CardEdition.Collection editions = FModel.getMagicDb().getEditions();

        CardDb cardDb = FModel.getMagicDb().getCommonCards();
        String[] cardNames = {"Shivan Dragon", "Animate Wall", "Balance", "Blessing", "Force of Will"};
        String[] expectedSets = {"M20", "MED", "SLD", "M14", "2XM"};
        List<PaperCard> cards = new ArrayList<>();
        for (int i=0; i < 5; i++){
            String cardName = cardNames[i];
            String expectedSet = expectedSets[i];
            PaperCard card = cardDb.getCardFromEditions(cardName, CardDb.CardArtPreference.LATEST_ART_ALL_EDITIONS);
            assertEquals(card.getEdition(), expectedSet, "Assertion Failed for "+cardName);
            cards.add(card);
        }

        CardPool pool = new CardPool();
        pool.add(cards);
        CardEdition ed = editions.getTheLatestOfAllTheOriginalEditionsOfCardsIn(pool);
        assertEquals(ed.getCode(), "ALL");
    }
}
