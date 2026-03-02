package forge.ai.sacrifice;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Tests for AI sacrifice decision-making when facing lethal consequences.
 */
public class AiSacrificeDecisionTest extends AITest {

    @Test
    public void testAiSacrificesCreatureToAvoidLethalLifeLoss() {
        Game game = initAndCreateGame();

        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(1, null);

        // Player 1 controls Fandaniel
        addCard("Fandaniel, Telophoroi Ascian", p);

        // Player 1 has a sorcery in graveyard (causes 2 life loss per sorcery/instant)
        addCardToZone("The Crystal's Chosen", p, ZoneType.Graveyard);

        // Opponent controls a creature they can sacrifice
        Card adelbert = addCard("Adelbert Steiner", opponent);

        // Fill libraries to prevent draw-death
        for (int i = 0; i < 10; i++) {
            addCardToZone("Plains", p, ZoneType.Library);
            addCardToZone("Plains", opponent, ZoneType.Library);
        }

        // Play until next turn (after Fandaniel's end step trigger resolves)
        this.playUntilNextTurn(game);

        // The game should NOT be over - opponent should have sacrificed to survive
        AssertJUnit.assertFalse("Game should not be over - AI should have sacrificed to survive", game.isGameOver());

        // Adelbert should be in graveyard (sacrificed)
        AssertJUnit.assertTrue("Adelbert Steiner should be in graveyard after being sacrificed",
                opponent.getZone(ZoneType.Graveyard).contains(adelbert));

        // Opponent should still have 1 life (didn't lose life because they sacrificed)
        AssertJUnit.assertEquals("Opponent should still have 1 life after sacrificing", 1, opponent.getLife());
    }


    @Test
    public void testAiDoesNotSacrificeWhenLifeLossIsNotLethal() {
        Game game = initAndCreateGame();

        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(10, null);

        // Player 1 controls Fandaniel
        addCard("Fandaniel, Telophoroi Ascian", p);

        // Player 1 has a sorcery in graveyard
        addCardToZone("The Crystal's Chosen", p, ZoneType.Graveyard);

        // Opponent controls a creature
        Card adelbert = addCard("Adelbert Steiner", opponent);

        // Fill libraries
        for (int i = 0; i < 10; i++) {
            addCardToZone("Plains", p, ZoneType.Library);
            addCardToZone("Plains", opponent, ZoneType.Library);
        }

        // Play until next turn (after Fandaniel's end step trigger resolves)
        this.playUntilNextTurn(game);

        // Game should not be over
        AssertJUnit.assertFalse("Game should not be over", game.isGameOver());

        // Adelbert should still be on battlefield (not sacrificed)
        AssertJUnit.assertTrue("Adelbert Steiner should still be on battlefield when life loss isn't lethal",
                opponent.getZone(ZoneType.Battlefield).contains(adelbert));

        // Opponent should have 8 life (lost 2 from not sacrificing)
        AssertJUnit.assertEquals("Opponent should have lost 2 life from not sacrificing", 8, opponent.getLife());
    }


    @Test
    public void testAiSacrificesToPillarTombsToAvoidLethalLifeLoss() {
        Game game = initAndCreateGame();

        Player p = game.getPlayers().get(1);
        p.setTeam(0);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        opponent.setLife(4, null);

        // Player 1 controls Pillar Tombs of Aku
        addCard("Pillar Tombs of Aku", p);

        // Opponent controls a creature they can sacrifice
        Card bear = addCard("Runeclaw Bear", opponent);

        // Fill libraries to prevent draw-death
        for (int i = 0; i < 10; i++) {
            addCardToZone("Plains", p, ZoneType.Library);
            addCardToZone("Plains", opponent, ZoneType.Library);
        }

        // Play two turns - first ends Player 1's turn, second plays through opponent's upkeep
        // where Pillar Tombs triggers and the AI must decide whether to sacrifice
        this.playUntilNextTurn(game);
        this.playUntilNextTurn(game);

        // The game should NOT be over - AI should have sacrificed to survive
        AssertJUnit.assertFalse("Game should not be over - AI should have sacrificed to survive", game.isGameOver());

        // Bear should be in graveyard (sacrificed)
        AssertJUnit.assertTrue("Runeclaw Bear should be in graveyard after being sacrificed",
                opponent.getZone(ZoneType.Graveyard).contains(bear));

        // Opponent should still have 4 life (didn't lose life because they sacrificed)
        AssertJUnit.assertEquals("Opponent should still have 4 life after sacrificing", 4, opponent.getLife());
    }
}
