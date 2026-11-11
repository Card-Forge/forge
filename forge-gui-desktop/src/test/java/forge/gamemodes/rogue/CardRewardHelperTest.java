package forge.gamemodes.rogue;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import forge.card.CardDbCardMockTestCase;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.testng.annotations.Test;

public class CardRewardHelperTest extends CardDbCardMockTestCase {

    @Test
    public void testRunRewardRerollThenChooseCardRemovesItFromLaterRewards() {
        RogueRun run = createRun(createStandardRewardPool(), CardRewardHelper.REROLL_BASE_COST);
        List<List<PaperCard>> offers = new ArrayList<>();
        AtomicInteger calls = new AtomicInteger();

        List<PaperCard> chosenCards = CardRewardHelper.runReward(run, (title, cards, maxSelections,
                rerollLabel, rerollEnabled, gold) -> {
            offers.add(new ArrayList<>(cards));
            return calls.getAndIncrement() == 0 ? null : List.of(cards.get(0));
        }, false, null, null);

        PaperCard chosenCard = chosenCards.get(0);
        assertEquals(run.getCurrentGold(), 0);
        assertEquals(run.getCurrentDeck().getMain().count(chosenCard), 1);

        List<List<PaperCard>> laterOffers = new ArrayList<>();
        List<PaperCard> skippedCards = CardRewardHelper.runReward(run, (title, cards, maxSelections,
                rerollLabel, rerollEnabled, gold) -> {
            laterOffers.add(new ArrayList<>(cards));
            return new ArrayList<>();
        }, false, null, null);

        assertTrue(skippedCards.isEmpty());
        assertEquals(offers.size(), 2);
        assertEquals(laterOffers.size(), 1);
        assertFalse(laterOffers.get(0).contains(chosenCard));
    }

    @Test
    public void testRunRewardSkipDiscardsOfferAndAllowsLaterReshuffle() {
        RogueRun run = createRun(createSingleOfferPool(), 0);
        List<List<PaperCard>> firstOffers = new ArrayList<>();

        List<PaperCard> skippedCards = CardRewardHelper.runReward(run, (title, cards, maxSelections,
                rerollLabel, rerollEnabled, gold) -> {
            firstOffers.add(new ArrayList<>(cards));
            return new ArrayList<>();
        }, false, null, null);

        List<List<PaperCard>> secondOffers = new ArrayList<>();
        List<PaperCard> skippedAgain = CardRewardHelper.runReward(run, (title, cards, maxSelections,
                rerollLabel, rerollEnabled, gold) -> {
            secondOffers.add(new ArrayList<>(cards));
            return new ArrayList<>();
        }, false, null, null);

        assertTrue(skippedCards.isEmpty());
        assertTrue(skippedAgain.isEmpty());
        assertEquals(firstOffers.size(), 1);
        assertEquals(secondOffers.size(), 1);
        assertEquals(new HashSet<>(secondOffers.get(0)), new HashSet<>(firstOffers.get(0)));
    }

    private RogueRun createRun(CardPool rewardPool, int gold) {
        RogueDeck rogueDeck = new RogueDeck();
        rogueDeck.setCommanderCardName("Test Commander");
        rogueDeck.setStartDeck(new Deck("Test Deck"));
        rogueDeck.setRewardPool(rewardPool);

        RogueRun run = new RogueRun(rogueDeck);
        run.setCurrentGold(gold);
        return run;
    }

    private CardPool createSingleOfferPool() {
        CardPool rewardPool = new CardPool();
        rewardPool.add(takeCards(PaperCardPredicates.IS_MYTHIC_RARE.negate(), 6));
        rewardPool.add(takeCards(PaperCardPredicates.IS_MYTHIC_RARE, 1));
        return rewardPool;
    }

    private CardPool createStandardRewardPool() {
        CardPool rewardPool = createSingleOfferPool();
        rewardPool.add(takeCards(PaperCardPredicates.IS_MYTHIC_RARE, 2).subList(1, 2));
        return rewardPool;
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
