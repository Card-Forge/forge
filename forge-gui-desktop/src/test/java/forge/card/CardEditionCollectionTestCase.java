package forge.card;

import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.model.FModel;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class CardEditionCollectionTestCase extends ForgeCardMockTestCase {

    @Test
    public void getEarliestEditionWithAllCardsNotWorkingAsExpected(){
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
        CardEdition ed = editions.getEarliestEditionWithAllCards(pool);
        assertEquals(ed.getCode(), "ALL");
    }
}
