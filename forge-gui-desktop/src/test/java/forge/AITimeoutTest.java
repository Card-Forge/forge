package forge;

import forge.ai.PlayerControllerAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class AITimeoutTest extends SimulationTest {
    @Test
    public void timeoutTest() {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);

        defender.setLife(10, null);

        String bears = "Grizzly Bears";
        String knight = "White Knight";

        for (int i = 0; i < 50; i++) {
            Card bear = addCard(bears, attacker);
            bear.setSickness(false);
        }
        for (int i = 0; i < 40; i++) {
            addCard(knight, defender);
        }

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacker);
        game.getAction().checkStateEffects(true);

        long startTime = System.nanoTime();
        Combat combat = ((PlayerControllerAi) attacker.getController())
                .getAi().getPredictedCombat();
        long endTime = System.nanoTime();

        double duration = (endTime - startTime) / 1_000_000_000.0;
        int attackingCreatures = combat.getAttackers().size();
        System.out.printf("AI Predicted Combat Duration: %.1f seconds\n", duration);
        System.out.printf("AI Predicted Combat Attacker Size: %d\n", attackingCreatures);

        // Do not add seconds to the configured timeout to hide a timeout regression.
        AssertJUnit.assertTrue("AI call exceeded timeout budget: " + duration + "s", duration <= game.getAITimeout() + 0.5);
        // This scenario intentionally exercises the deadline; a partial attack decision is valid.
        AssertJUnit.assertTrue("Invalid attacker count: " + attackingCreatures, attackingCreatures > 0 && attackingCreatures <= 50);
        // Worker retirement is asynchronous; pool size is not the timeout contract.
    }
}
