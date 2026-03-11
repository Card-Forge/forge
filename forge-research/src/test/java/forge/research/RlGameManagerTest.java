package forge.research;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import forge.GuiDesktop;
import forge.gui.GuiBase;
import forge.model.FModel;

/**
 * Basic integration test for RlGameManager.
 * Runs a game with a random-policy agent (always picks action 0).
 */
public class RlGameManagerTest {

    private static final String DECK_A = "src/main/resources/decks/ramunap_red.dck";
    private static final String DECK_B = "src/main/resources/decks/bg_constrictor.dck";

    @BeforeClass
    public static void initForge() {
        try {
            GuiBase.setInterface(new GuiDesktop());
            FModel.initialize(null, null);
        } catch (Exception e) {
            System.err.println("FModel init failed (may need res dir): " + e.getMessage());
        }
    }

    @Test
    public void testGameCompletesWithRandomPolicy() {
        RlGameManager manager = new RlGameManager();

        // Reset game
        DecisionContext ctx = manager.resetGame(DECK_A, DECK_B, 0);
        assertNotNull("First decision context should not be null", ctx);

        int maxSteps = 5000;
        int steps = 0;

        while (!ctx.isGameOver() && steps < maxSteps) {
            // Random policy: always pick action 0
            ctx = manager.step(0);
            assertNotNull("Decision context should not be null", ctx);
            steps++;
        }

        assertTrue("Game should complete within " + maxSteps + " steps", ctx.isGameOver());
        System.out.println("Game completed in " + steps + " steps with reward: " + ctx.getReward());
    }
}
