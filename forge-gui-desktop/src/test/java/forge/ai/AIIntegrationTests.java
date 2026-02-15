package forge.ai;

import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class AIIntegrationTests extends AITest {
    @Test
    public void testSwingForLethal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Nest Robber", p);
        addCard("Nest Robber", p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        addCard("Runeclaw Bear", opponent);
        opponent.setLife(2, null);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.isGameOver());
    }

    @Test
    public void testSuspendAI() {
        // Test that the AI can cast a suspend spell
        // They should suspend it, then deal three damage to their opponent
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Mountain", p);
        addCardToZone("Rift Bolt", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // Fill deck with lands so we can goldfish a few turns
        for (int i = 0; i < 60; i++) {
            addCardToZone("Island", opponent, ZoneType.Library);
            // Add something they can't cast
            addCardToZone("Stone Golem", p, ZoneType.Library);
        }

        for (int i = 0; i < 3; i++) {
            this.playUntilNextTurn(game);
        }

        AssertJUnit.assertEquals(17, opponent.getLife());
    }

    @Test
    public void testAttackTriggers() {
        // Test that attack triggers actually trigger
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Hellrider", p);
        addCard("Raging Goblin", p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // Fill deck with lands so we can goldfish a few turns
        for (int i = 0; i < 60; i++) {
            addCardToZone("Island", opponent, ZoneType.Library);
            // Add something they can't cast
            addCardToZone("Stone Golem", p, ZoneType.Library);
        }

        this.playUntilNextTurn(game);

        AssertJUnit.assertEquals(14, opponent.getLife());
    }

    @Test
    public void testDoesNotCastRepopulateWhenNoCreaturesInOpponentGraveyard() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // AI has Repopulate in hand, mana to cast, and no other playable cards.
        Zone hand = p.getZone(ZoneType.Hand);
        Card repopulate = addCardToZone("Repopulate", p, ZoneType.Hand);
        AssertJUnit.assertTrue(hand.contains(repopulate));

        // Setup mana
        Card colorlessMana = createCard("Ash Barrens", p);
        Card greenMana = createCard("Forest", p);
        p.getManaPool().addMana(new Mana((byte) ManaAtom.COLORLESS, colorlessMana, null, p));
        p.getManaPool().addMana(new Mana((byte) ManaAtom.GREEN, greenMana, null, p));
        AssertJUnit.assertEquals(2, p.getManaPool().totalMana());

        // Put a non-creature card in opponent's graveyard
        addCardToZone("Swamp", opponent, ZoneType.Graveyard);

        // Opponent has 0 creatures in graveyard.
        AssertJUnit.assertFalse(opponent.getZone(ZoneType.Graveyard).getCards().anyMatch(Card::isCreature));

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue("Repopulate must still be in hand", hand.contains(repopulate));
    }

    @Test
    public void testSylvanLibraryCanBeCast() {
        // Test that the AI will cast Sylvan Library when it has mana available
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Forest", p);
        addCard("Forest", p);
        addCardToZone("Sylvan Library", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // Fill decks with cards
        for (int i = 0; i < 60; i++) {
            addCardToZone("Island", opponent, ZoneType.Library);
            addCardToZone("Forest", p, ZoneType.Library);
        }

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        // Sylvan Library should be on the battlefield
        AssertJUnit.assertEquals("Sylvan Library should be on the battlefield", 1, countCardsWithName(game, "Sylvan Library"));
    }

    @Test
    public void testSylvanLibraryAtLowLifeActsLikeMirrisGuile() {
        // Test that when at low life, the AI uses Sylvan Library to look at cards
        // but puts them back instead of paying life (like Mirri's Guile)
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        p.setLife(5, null); // Low life - can't afford to pay 4 life

        addCard("Sylvan Library", p);
        // Add lands so the AI has mana to cast things
        addCard("Forest", p);
        addCard("Forest", p);
        addCard("Forest", p);
        addCard("Forest", p);
        addCard("Forest", p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        // Set up known library - first added goes on top (index 0)
        // Library order after setup: Forest (top), Forest, Llanowar Elves (bottom of top 3)
        // The AI draws 3 cards (1 normal + 2 from Sylvan Library cost)
        // Then must choose 2 to return. Should keep Llanowar Elves (best) and return 2 Forests
        addCardToZone("Forest", p, ZoneType.Library);         // Top (index 0)
        addCardToZone("Forest", p, ZoneType.Library);         // Middle (index 1)
        addCardToZone("Llanowar Elves", p, ZoneType.Library); // Bottom of top 3 (index 2) - best card

        // Fill rest of deck
        for (int i = 0; i < 57; i++) {
            addCardToZone("Forest", p, ZoneType.Library);
            addCardToZone("Island", opponent, ZoneType.Library);
        }

        // Play through to AI's next main phase (after draw step completes)
        this.playUntilNextTurn(game);  // AI turn 0 -> Opponent turn
        this.playUntilNextTurn(game);  // Opponent turn -> AI turn
        this.playUntilPhase(game, PhaseType.MAIN1);  // Advance through DRAW to MAIN1

        // The AI should still be at 5 life (didn't pay any life)
        AssertJUnit.assertEquals("AI should not have paid life", 5, p.getLife());
        // The AI should have 1 card in hand (kept the best, put 2 back)
        AssertJUnit.assertEquals("AI should have 1 card in hand", 1, p.getZone(ZoneType.Hand).size());
    }
}
