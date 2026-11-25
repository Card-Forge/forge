package forge.gamemodes.rogue;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import forge.card.CardDb;
import forge.card.CardMockTestCase;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.model.FModel;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.testng.annotations.Test;

public class CardRewardHelperTest extends CardMockTestCase {

    @Test
    public void rerollThenClaimReward() {
        List<PaperCard> rewardCards = createRewardCards();
        PaperCard chosenCard = rewardCards.get(0);
        RogueRun run = createRun(rewardCards, CardRewardHelper.REROLL_BASE_COST);
        AtomicBoolean reroll = new AtomicBoolean(true);

        List<PaperCard> chosenCards = CardRewardHelper.runReward(run, (title, cards, maxSelections,
                rerollLabel, rerollEnabled, gold) -> {
            if (reroll.getAndSet(false)) {
                assertTrue(rerollEnabled);
                return null;
            }
            assertTrue(cards.contains(chosenCard));
            return List.of(chosenCard);
        }, false, null, null);

        assertEquals(chosenCards, List.of(chosenCard));
        assertEquals(run.getCurrentGold(), 0);
        assertEquals(run.getCurrentDeck().getMain().count(chosenCard), 1);

        // Draw without the owned-card filter so it cannot hide a failed pool removal.
        List<PaperCard> remaining = run.getSelectedRogueDeck().drawRewardOptions(rewardCards.size(), null);
        assertEquals(remaining.size(), rewardCards.size() - 1);
        assertEquals(Set.copyOf(remaining), Set.copyOf(rewardCards.subList(1, rewardCards.size())));
    }

    @Test
    public void skippedRewardsReturnAfterReshuffle() {
        List<PaperCard> rewardCards = createRewardCards();
        RogueRun run = createRun(rewardCards, 0);

        List<PaperCard> skippedCards = CardRewardHelper.runReward(run, (title, cards, maxSelections,
                rerollLabel, rerollEnabled, gold) -> {
            assertEquals(Set.copyOf(cards), Set.copyOf(rewardCards));
            return List.of();
        }, false, null, null);

        assertTrue(skippedCards.isEmpty());
        RogueDeck deck = run.getSelectedRogueDeck();
        assertTrue(deck.getRewardPoolCards().isEmpty());
        List<PaperCard> reshuffled = deck.drawRewardOptions(rewardCards.size(), null);
        assertEquals(reshuffled.size(), rewardCards.size());
        assertEquals(Set.copyOf(reshuffled), Set.copyOf(rewardCards));
    }

    private RogueRun createRun(List<PaperCard> rewardCards, int gold) {
        CardPool rewardPool = new CardPool();
        rewardPool.add(rewardCards);
        RogueDeck rogueDeck = new RogueDeck();
        // This fixture has no curated commander and should skip Codex tracking.
        rogueDeck.setExperimental(true);
        rogueDeck.setStartDeck(new Deck("Test Deck"));
        rogueDeck.setRewardPool(rewardPool);

        RogueRun run = new RogueRun(rogueDeck);
        run.setCurrentGold(gold);
        return run;
    }

    private List<PaperCard> createRewardCards() {
        CardDb cards = FModel.getMagicDb().getCommonCards();
        // A normal offer draws six nonmythics and one mythic, exhausting this pool.
        return List.of(cards.getCard("Sol Ring", "MOC"), cards.getCard("Hedron Archive", "BFZ"),
            cards.getCard("Runeclaw Bear", "M10"), cards.getCard("Thraben Inspector", "SOI"),
            cards.getCard("Serra Angel", "M10"), cards.getCard("Lightning Bolt", "M10"),
            cards.getCard("Atraxa, Grand Unifier", "ONE"));
    }
}
