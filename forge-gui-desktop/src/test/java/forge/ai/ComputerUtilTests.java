package forge.ai;

import forge.game.Game;
import forge.game.zone.ZoneType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;

public class ComputerUtilTests extends AITest {
    // Mulligan scoring relies on deck composition.
    private void setupDeck(Player p) {
        // Standard 60-card deck: 24 lands, 36 spells
        // Minus 7 for starting hand (3 lands, 4 spells) = 21 lands, 32 spells
        for (int i = 0; i < 21; i++) {
            addCardToZone("Forest", p, ZoneType.Library);
        }
        for (int i = 0; i < 32; i++) {
            addCardToZone("Grizzly Bears", p, ZoneType.Library);
        }
    }

    @Test
    public void testReturnsEmptyWhenNothingToReturn() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        setupDeck(p);

        CardCollection hand = new CardCollection();
        hand.add(createCard("Forest", p));
        hand.add(createCard("Island", p));

        CardCollectionView result = ComputerUtil.chooseBestCardsToReturn(p, hand, 0);
        assertEquals(0, result.size());
    }

    @Test
    public void testReturnsFullHandWhenAllMustBeReturned() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        setupDeck(p);

        CardCollection hand = new CardCollection();
        hand.add(createCard("Forest", p));
        hand.add(createCard("Island", p));

        CardCollectionView result = ComputerUtil.chooseBestCardsToReturn(p, hand, 2);
        assertEquals(2, result.size());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThrowsWhenRequestingMoreCardsThanInHand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        setupDeck(p);

        CardCollection hand = new CardCollection();
        hand.add(createCard("Forest", p));

        ComputerUtil.chooseBestCardsToReturn(p, hand, 2);
    }

    @Test
    public void testReturnsCorrectNumberOfCards() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        setupDeck(p);

        CardCollection hand = new CardCollection();
        hand.add(createCard("Forest", p));
        hand.add(createCard("Island", p));
        hand.add(createCard("Mountain", p));
        hand.add(createCard("Mountain", p));
        hand.add(createCard("Mountain", p));
        hand.add(createCard("Raging Goblin", p));
        hand.add(createCard("Nest Robber", p));

        CardCollectionView result = ComputerUtil.chooseBestCardsToReturn(p, hand, 2);
        assertEquals(2, result.size());
    }

    @Test
    public void testPrefersReturningExcessLands() {
        // A hand of 6 lands and 1 spell: should return lands, not the spell
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        setupDeck(p);

        CardCollection hand = new CardCollection();
        for (int i = 0; i < 6; i++) {
            hand.add(createCard("Forest", p));
        }
        Card bear = createCard("Runeclaw Bear", p);
        hand.add(bear);

        CardCollectionView result = ComputerUtil.chooseBestCardsToReturn(p, hand, 2);
        assertFalse("Should not return the only spell", result.contains(bear));
    }

    @Test
    public void testPrefersReturningHighCostSpellsWhenLandLight() {
        // A hand with 2 lands and a mix of cheap/expensive spells: should return expensive ones
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        setupDeck(p);

        CardCollection hand = new CardCollection();
        hand.add(createCard("Mountain", p));
        hand.add(createCard("Mountain", p));
        Card expensive = createCard("Stone Golem", p); // 5 CMC
        hand.add(expensive);
        hand.add(createCard("Raging Goblin", p)); // 1 CMC
        hand.add(createCard("Raging Goblin", p)); // 1 CMC
        hand.add(createCard("Nest Robber", p));   // 2 CMC
        hand.add(createCard("Nest Robber", p));   // 2 CMC

        CardCollectionView result = ComputerUtil.chooseBestCardsToReturn(p, hand, 1);
        assertTrue("Should return the highest CMC card when land-light, but returned: " + result, result.contains(expensive));
    }
}