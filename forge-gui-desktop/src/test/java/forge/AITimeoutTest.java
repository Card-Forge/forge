package forge;

import forge.ai.PlayerControllerAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.util.ThreadUtil;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

public class AITimeoutTest extends SimulationTest {
    @Test
    public void timeoutTestLethal() throws Exception {
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

        int duration = Math.toIntExact(TimeUnit.NANOSECONDS.toSeconds(endTime - startTime));
        int attackingCreatures = combat.getAttackers().size();

        // NOTE: This exact test fails at approximately 6.3 - 6.6 seconds average while on local it's a lot faster, seems the external
        // deadlinenanos and cdl await have some delay after the task have run (ie hit a sync block that cannot be cancelled can induce
        // more time). Adding a 2 second buffer may be better, but if this simple test is way beyond then there's something blocking the cancel method
        AssertJUnit.assertTrue("AI Timeout should be less than or equal",
                duration <= (game.getAITimeout() + 2));
        AssertJUnit.assertEquals("AI should attack with all 50 bears for lethal",
                50, attackingCreatures);

        // No tasks should be running
        AssertJUnit.assertEquals(0, ThreadUtil.AIExecutor.getActiveCount());

        // Wait longer than keepAlive so idle threads terminate.. 1.5 Seconds
        Thread.sleep(1500);

        // Pool should shrink to zero, meaning all worker threads have terminated
        AssertJUnit.assertEquals("Pool should shrink to zero after idle timeout",
                0, ThreadUtil.AIExecutor.getPoolSize());
    }

}
