package forge.ai.simulation;

import forge.ai.simulation.GameStateEvaluator.Score;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * A win is a win, but a win now is worth more than a win in ten turns, and a loss we can put off is
 * better than one we cannot. Scoring both as flat extremes made every winning line look identical
 * to the simulation AI, so it had no reason to take the faster one.
 */
public class TerminalScoreDiscountTest extends SimulationTest {

    private Score scoreAfterGameOverOnTurn(int turn, boolean aiWins) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai, turn);

        // end the game outright
        (aiWins ? opp : ai).setLife(0, null);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("the game should be over", game.isGameOver());

        return new GameStateEvaluator().getScoreForGameState(game, ai);
    }

    @Test
    public void winsSoonerScoreHigher() {
        Score early = scoreAfterGameOverOnTurn(2, true);
        Score late = scoreAfterGameOverOnTurn(12, true);
        AssertJUnit.assertTrue("a win on turn 2 should beat a win on turn 12",
                early.value > late.value);
    }

    @Test
    public void lossesLaterScoreHigher() {
        Score early = scoreAfterGameOverOnTurn(2, false);
        Score late = scoreAfterGameOverOnTurn(12, false);
        AssertJUnit.assertTrue("dying on turn 12 should beat dying on turn 2",
                late.value > early.value);
    }

    /** Any win still has to outrank any loss, however far off it is. */
    @Test
    public void everyWinBeatsEveryLoss() {
        AssertJUnit.assertTrue(scoreAfterGameOverOnTurn(999, true).value
                > scoreAfterGameOverOnTurn(1, false).value);
    }

    /** GameSimulator uses Integer.MIN_VALUE for "could not simulate", so a loss must stay above it. */
    @Test
    public void aLossIsNotMistakenForAFailedSimulation() {
        AssertJUnit.assertTrue("a real loss must score above the failed-simulation sentinel",
                scoreAfterGameOverOnTurn(1, false).value > Integer.MIN_VALUE);
    }

    /** The search stops recursing once it has found a win, whichever turn that win lands on. */
    @Test
    public void winsStillStopTheSearch() {
        AssertJUnit.assertTrue(GameStateEvaluator.isWinning(scoreAfterGameOverOnTurn(1, true).value));
        AssertJUnit.assertTrue(GameStateEvaluator.isWinning(scoreAfterGameOverOnTurn(40, true).value));
        AssertJUnit.assertFalse(GameStateEvaluator.isWinning(scoreAfterGameOverOnTurn(1, false).value));
        AssertJUnit.assertFalse(GameStateEvaluator.isWinning(0));
    }
}
