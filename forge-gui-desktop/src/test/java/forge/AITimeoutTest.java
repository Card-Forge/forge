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

        double duration = (endTime - startTime) / 1_000_000_000.0;
        int attackingCreatures = combat.getAttackers().size();

        // NOTE: This exact test fails at approximately 6.3 - 6.6 seconds average while on local it's a lot faster, seems the external
        // deadlinenanos and cdl await have some delay after the task have run (ie hit a sync block that cannot be cancelled can induce
        // more time). Adding a 2 second buffer may be better, but if this simple test is way beyond then there's something blocking the cancel method
        System.out.printf("AI Predicted Combat Duration: %.1f seconds\n", duration);
        AssertJUnit.assertTrue("AI Timeout should be less than or equal",
                duration <= (game.getAITimeout() + 2));
        AssertJUnit.assertEquals("AI should attack with all 50 bears for lethal",
                50, attackingCreatures);

        // No tasks should be running
        AssertJUnit.assertEquals("No tasks should be running", 0, ThreadUtil.AIExecutor.getActiveCount());

        // We poll up to 2.0 seconds total to account for slow CI environments.
        int retries = 20;
        while (retries > 0 && ThreadUtil.AIExecutor.getPoolSize() > 0) {
            Thread.sleep(100);
            retries--;
        }

        if (ThreadUtil.AIExecutor.getPoolSize() > 0) {
            // Look at the thread if it is stuck somehow
            java.lang.management.ThreadMXBean threadMXBean = java.lang.management.ManagementFactory.getThreadMXBean();
            java.lang.management.ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);

            for (java.lang.management.ThreadInfo info : threadInfos) {
                if (info.getThreadName().contains("AI ThreadPool")) {
                    System.err.println(info);
                }
            }
        }

        // Pool should now be strictly verified at zero
        AssertJUnit.assertEquals("Pool should shrink to zero after idle timeout", 0, ThreadUtil.AIExecutor.getPoolSize());
    }
}
