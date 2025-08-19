package forge.ai;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
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
}
