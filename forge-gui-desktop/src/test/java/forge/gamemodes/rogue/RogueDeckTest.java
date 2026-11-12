package forge.gamemodes.rogue;

import static org.testng.Assert.assertEquals;

import forge.card.CardDb;
import forge.card.CardMockTestCase;
import forge.deck.CardPool;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.model.FModel;
import java.util.List;
import org.testng.annotations.Test;

public class RogueDeckTest extends CardMockTestCase {

    @Test
    public void mythicDrawCanReturnFewerCards() {
        CardDb cards = FModel.getMagicDb().getCommonCards();
        PaperCard mythic = cards.getCard("Atraxa, Grand Unifier", "ONE");
        CardPool rewardPool = new CardPool();
        rewardPool.add(List.of(mythic, cards.getCard("Sol Ring", "MOC")));
        RogueDeck deck = new RogueDeck();
        deck.setRewardPool(rewardPool);

        deck.discardRewardOptions(List.of(mythic));

        assertEquals(deck.drawRewardOptions(2, PaperCardPredicates.IS_MYTHIC_RARE), List.of(mythic));
    }
}
