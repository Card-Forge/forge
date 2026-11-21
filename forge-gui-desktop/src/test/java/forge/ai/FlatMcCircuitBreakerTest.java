package forge.ai;

import java.lang.reflect.Method;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import forge.ai.simulation.FlatMcTestBase;
import forge.ai.simulation.FlatMonteCarloPicker;

/**
 * The circuit breaker and the strength settings of Flat Monte Carlo (PR #12508).
 * <ul>
 *   <li>errors switch Monte Carlo off for the rest of the game (a bug does not go away);</li>
 *   <li>slowness never does: four slow decisions in a row pause it for 3 turns, the next time for 6, then 9, and the
 *       search comes back smaller; 25 good decisions in a row give one step of it back;</li>
 *   <li>phones get the lite settings, desktops the maximum, and the maximum wins when both are set.</li>
 * </ul>
 * setUpGame() puts the game at turn 5.
 */
public class FlatMcCircuitBreakerTest extends FlatMcTestBase {

    private void slow() {
        for (int i = 0; i < 4; i++) {
            brain().noteFlatMcOutcome("too-slow");
        }
    }

    private void good(final int times) {
        for (int i = 0; i < times; i++) {
            brain().noteFlatMcOutcome("rollouts");
        }
    }

    /** How many steps of the search a slow spell has taken away (private field: the defaults are too small to see it in the config). */
    private int trips() {
        try {
            final java.lang.reflect.Field f = AiController.class.getDeclaredField("flatMcSlowTrips");
            f.setAccessible(true);
            return f.getInt(brain());
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("cannot read the slow trips: " + e);
        }
    }

    private FlatMonteCarloPicker.Config config() {
        try {
            final Method m = AiController.class.getDeclaredMethod("flatMonteCarloConfig");
            m.setAccessible(true);
            return (FlatMonteCarloPicker.Config) m.invoke(brain());
        } catch (final ReflectiveOperationException e) {
            throw new AssertionError("cannot read the configuration: " + e);
        }
    }

    // ===================================================================================== errors

    @Test
    public void threeErrorsSwitchItOffForTheGame() {
        setUpGame();
        brain().noteFlatMcOutcome("error");
        brain().noteFlatMcOutcome("error");
        assertFalse(brain().isFlatMonteCarloTripped(), "two errors are still forgiven");
        brain().noteFlatMcOutcome("error");
        assertTrue(brain().isFlatMonteCarloTripped());
        setTurn(500);
        assertTrue(brain().isFlatMonteCarloTripped(), "an error is a bug: waiting does not fix it");
    }

    @Test
    public void goodDecisionsDoNotForgiveErrors() {
        setUpGame();
        brain().noteFlatMcOutcome("error");
        good(100);
        brain().noteFlatMcOutcome("error");
        brain().noteFlatMcOutcome("error");
        assertTrue(brain().isFlatMonteCarloTripped());
    }

    // ===================================================================================== slowness

    @Test
    public void threeSlowDecisionsAreNotYetAProblem() {
        setUpGame();
        for (int i = 0; i < 3; i++) {
            brain().noteFlatMcOutcome("too-slow");
        }
        assertFalse(brain().isFlatMonteCarloTripped());
    }

    @Test
    public void noTimeCountsAsSlow() {
        setUpGame();
        for (int i = 0; i < 4; i++) {
            brain().noteFlatMcOutcome("no-time");
        }
        assertTrue(brain().isFlatMonteCarloTripped(), "paused");
    }

    @Test
    public void aGoodDecisionBreaksTheSlowStreak() {
        setUpGame();
        for (int i = 0; i < 3; i++) {
            brain().noteFlatMcOutcome("too-slow");
        }
        good(1);
        for (int i = 0; i < 3; i++) {
            brain().noteFlatMcOutcome("too-slow");
        }
        assertFalse(brain().isFlatMonteCarloTripped(), "3 + 3 with a good one between is not 4 in a row");
    }

    @Test
    public void fourSlowDecisionsPauseForThreeTurnsThenItComesBack() {
        setUpGame(); // turn 5
        slow();
        for (final int turn : new int[] { 5, 6, 7 }) {
            setTurn(turn);
            assertTrue(brain().isFlatMonteCarloTripped(), "paused on turn " + turn);
        }
        setTurn(8);
        assertFalse(brain().isFlatMonteCarloTripped(), "back on turn 8");
    }

    @Test
    public void everyFurtherPauseIsLonger() {
        setUpGame();
        slow(); // 3 turns
        setTurn(8);
        slow(); // 6 turns
        setTurn(13);
        assertTrue(brain().isFlatMonteCarloTripped(), "second pause: until turn 14");
        setTurn(14);
        assertFalse(brain().isFlatMonteCarloTripped());
        slow(); // 9 turns
        setTurn(22);
        assertTrue(brain().isFlatMonteCarloTripped(), "third pause: until turn 23");
        setTurn(23);
        assertFalse(brain().isFlatMonteCarloTripped());
    }

    @Test
    public void slownessNeverSwitchesItOffForGood() {
        setUpGame();
        int turn = 5;
        for (int i = 0; i < 8; i++) {
            setTurn(turn);
            slow();
            turn += 20; // far beyond any pause
            setTurn(turn);
            assertFalse(brain().isFlatMonteCarloTripped(), "after slow spell " + (i + 1));
        }
    }

    @Test
    public void thePauseStopsGrowingAtNineTurns() {
        setUpGame();
        for (int i = 0; i < 6; i++) {
            setTurn(5);
            slow();
        }
        setTurn(5 + 9 - 1);
        assertTrue(brain().isFlatMonteCarloTripped());
        setTurn(5 + 9);
        assertFalse(brain().isFlatMonteCarloTripped(), "never longer than nine turns");
    }

    // ===================================================================================== the search shrinks and recovers

    @Test
    public void afterASlowSpellTheSearchIsSmallerAndHasNoLookahead() {
        setUpGame();
        final FlatMonteCarloPicker.Config before = config();
        assertNotNull(before);
        slow();
        setTurn(100);
        final FlatMonteCarloPicker.Config after = config();
        assertEquals(after.maxCandidates, Math.max(3, before.maxCandidates - 2));
        assertEquals(after.rolloutsPerCandidate, Math.max(2, before.rolloutsPerCandidate - 1));
        assertTrue(after.extraRounds <= 1);
        assertEquals(after.opponentLookaheadPercent, 0, "the look at the opponent's turn costs most");
    }

    @Test
    public void everySlowSpellShrinksItFurtherUpToThree() {
        setUpGame();
        final FlatMonteCarloPicker.Config base = config();
        for (int trips = 1; trips <= 5; trips++) {
            setTurn(5);
            slow();
            setTurn(1000);
            final int effective = Math.min(trips, 3);
            assertEquals(trips(), effective, "steps taken after " + trips + " slow spells");
            final FlatMonteCarloPicker.Config c = config();
            assertEquals(c.maxCandidates, Math.max(3, base.maxCandidates - 2 * effective), "after " + trips + " slow spells");
            assertEquals(c.rolloutsPerCandidate, Math.max(2, base.rolloutsPerCandidate - effective));
        }
    }

    @Test
    public void twentyFiveGoodDecisionsGiveOneStepBack() {
        setUpGame();
        final FlatMonteCarloPicker.Config base = config();
        slow();
        setTurn(100);
        good(24);
        assertEquals(trips(), 1, "24 is not enough");
        assertEquals(config().maxCandidates, Math.max(3, base.maxCandidates - 2));
        good(1);
        assertEquals(trips(), 0, "25 earn the step back");
        assertEquals(config().maxCandidates, base.maxCandidates);
    }

    @Test
    public void aNewSlowSpellRestartsTheRecovery() {
        setUpGame();
        final FlatMonteCarloPicker.Config base = config();
        slow();
        setTurn(100);
        good(20);
        slow(); // second trip: the 20 good decisions are forgotten
        setTurn(200);
        good(24);
        assertEquals(trips(), 2, "still two steps down: the 20 good decisions before the second slow spell are forgotten");
        assertEquals(config().maxCandidates, Math.max(3, base.maxCandidates - 4));
        good(1);
        assertEquals(trips(), 1, "one step back");
        assertEquals(config().maxCandidates, Math.max(3, base.maxCandidates - 2));
    }

    @Test
    public void goodDecisionsBeforeAnySlowSpellChangeNothing() {
        setUpGame();
        final FlatMonteCarloPicker.Config base = config();
        good(200);
        assertEquals(config().maxCandidates, base.maxCandidates);
    }

    // ===================================================================================== strength settings

    @Test
    public void lowerStrengthIsTheDefaultOnTheGameFlags() {
        setUpGame();
        assertFalse(game.getAIFlatMonteCarloLite(), "off until the match switches it on");
        assertEquals(config().lookaheadMaxCandidates, 0);
    }

    @Test
    public void liteSettingsGiveAPhoneTheCheapLookahead() {
        setUpGame();
        game.AI_FLAT_MONTE_CARLO_LITE = true;
        final FlatMonteCarloPicker.Config c = config();
        assertEquals(c.lookaheadMaxCandidates, 3 /* MOBILE_LOOKAHEAD_MAX_CANDIDATES */);
        assertTrue(c.opponentLookaheadPercent >= 35 /* LOOKAHEAD_PERCENT_MOBILE */);
        assertTrue(c.maxCandidates >= 5 /* MAX_CANDIDATES_MOBILE */);
        assertTrue(c.pruning);
    }

    @Test
    public void maximumSettingsBeatLite() {
        setUpGame();
        game.AI_FLAT_MONTE_CARLO_LITE = true;
        game.AI_FLAT_MONTE_CARLO_MAX = true;
        final FlatMonteCarloPicker.Config c = config();
        assertTrue(c.opponentLookaheadPercent >= 50 /* LOOKAHEAD_PERCENT_DESKTOP */);
        assertEquals(c.lookaheadMaxCandidates, 0, "a desktop looks ahead on every decision");
    }

    @Test
    public void attackSearchIsPartOfLiteAndMax() {
        setUpGame();
        game.AI_FLAT_MONTE_CARLO_LITE = true;
        assertTrue(game.getAIFlatMonteCarloLite());
        // the attack refinement itself is private: this guards the switch it reads
        game.AI_FLAT_MONTE_CARLO_LITE = false;
        assertFalse(game.getAIFlatMonteCarloLite());
    }
}