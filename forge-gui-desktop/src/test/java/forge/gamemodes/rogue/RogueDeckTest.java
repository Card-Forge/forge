package forge.gamemodes.rogue;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

import forge.card.CardDbCardMockTestCase;
import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.testng.annotations.Test;

public class RogueDeckTest extends CardDbCardMockTestCase {

    @Test
    public void testDiscardedCardsReturnAfterReshuffle() {
        List<PaperCard> cards = takeCards(card -> true, 3);
        RogueDeck deck = createDeck(cards);

        List<PaperCard> firstDraw = deck.drawRewardOptions(2, null);
        deck.discardRewardOptions(firstDraw);

        List<PaperCard> reshuffledDraw = deck.drawRewardOptions(3, null);

        assertEquals(firstDraw.size(), 2);
        assertEquals(reshuffledDraw.size(), 3);
        assertEquals(new HashSet<>(reshuffledDraw), new HashSet<>(cards));
    }

    @Test
    public void testRemoveFromCardPoolsRemovesDiscardedCardsPermanently() {
        List<PaperCard> cards = takeCards(card -> true, 3);
        RogueDeck deck = createDeck(cards);

        List<PaperCard> firstDraw = deck.drawRewardOptions(2, null);
        PaperCard removedCard = firstDraw.get(0);
        deck.discardRewardOptions(firstDraw);
        deck.removeFromCardPools(List.of(removedCard));

        List<PaperCard> remainingCards = deck.drawRewardOptions(3, null);

        assertEquals(remainingCards.size(), 2);
        assertFalse(remainingCards.contains(removedCard));
    }

    @Test
    public void testFilteredDrawCanReturnFewerCardsThanRequestedAfterReshuffle() {
        PaperCard mythic = takeCards(PaperCardPredicates.IS_MYTHIC_RARE, 1).get(0);
        RogueDeck deck = createDeck(List.of(mythic));

        List<PaperCard> shownMythic = deck.drawRewardOptions(1, PaperCardPredicates.IS_MYTHIC_RARE);
        deck.discardRewardOptions(shownMythic);

        List<PaperCard> mythics = deck.drawRewardOptions(2, PaperCardPredicates.IS_MYTHIC_RARE);

        assertEquals(mythics.size(), 1);
        assertEquals(mythics.get(0), mythic);
    }

    private RogueDeck createDeck(List<PaperCard> cards) {
        RogueDeck deck = new RogueDeck();
        CardPool rewardPool = new CardPool();
        rewardPool.add(cards);
        deck.setRewardPool(rewardPool);
        return deck;
    }

    private List<PaperCard> takeCards(Predicate<PaperCard> predicate, int count) {
        List<PaperCard> result = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (PaperCard card : cardDb.getAllCards(predicate)) {
            if (names.add(card.getName())) {
                result.add(card);
                if (result.size() == count) {
                    break;
                }
            }
        }
        assertEquals(result.size(), count);
        return result;
    }
}
