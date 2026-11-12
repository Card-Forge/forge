package forge.ai.simulation;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/**
 * The strength settings of Flat Monte Carlo (PR #12508): the phone settings (mobileLite), the desktop settings
 * (maximized), the per-decision lookahead rule (forDecision) and the device scaling (affordableLines).
 * No game and no cards: only the numbers.
 */
public class FlatMonteCarloConfigTest {

    // ===================================================================================== mobileLite

    @Test
    public void liteRaisesASmallProfileToThePhoneMinimums() {
        final FlatMonteCarloPicker.Config c = new FlatMonteCarloPicker.Config();
        c.maxCandidates = 1;
        c.rolloutsPerCandidate = 1;
        c.budgetNanos = 0;
        c.turnBudgetNanos = 0;
        c.opponentLookaheadPercent = 0;
        c.mobileLite();
        assertEquals(c.maxCandidates, FlatMonteCarloPicker.MAX_CANDIDATES_MOBILE);
        assertEquals(c.rolloutsPerCandidate, FlatMonteCarloPicker.MIN_ROLLOUTS_MOBILE);
        assertEquals(c.budgetNanos, TimeUnit.MILLISECONDS.toNanos(FlatMonteCarloPicker.MOBILE_DECISION_MS));
        assertEquals(c.turnBudgetNanos, TimeUnit.MILLISECONDS.toNanos(FlatMonteCarloPicker.MOBILE_TURN_MS));
        assertEquals(c.opponentLookaheadPercent, FlatMonteCarloPicker.LOOKAHEAD_PERCENT_MOBILE);
        assertEquals(c.lookaheadMaxCandidates, FlatMonteCarloPicker.MOBILE_LOOKAHEAD_MAX_CANDIDATES);
        assertTrue(c.pruning);
    }

    @Test
    public void liteNeverLowersWhatTheProfileAlreadyHas() {
        final FlatMonteCarloPicker.Config c = new FlatMonteCarloPicker.Config();
        c.maxCandidates = 9;
        c.rolloutsPerCandidate = 12;
        c.budgetNanos = TimeUnit.SECONDS.toNanos(10);
        c.turnBudgetNanos = TimeUnit.SECONDS.toNanos(30);
        c.opponentLookaheadPercent = 80;
        c.mobileLite();
        assertEquals(c.maxCandidates, 9);
        assertEquals(c.rolloutsPerCandidate, 12);
        assertEquals(c.budgetNanos, TimeUnit.SECONDS.toNanos(10));
        assertEquals(c.turnBudgetNanos, TimeUnit.SECONDS.toNanos(30));
        assertEquals(c.opponentLookaheadPercent, 80);
    }

    @Test
    public void liteTimeLimitsStayBelowTheDesktopLimits() {
        assertTrue(FlatMonteCarloPicker.MOBILE_DECISION_MS < FlatMonteCarloPicker.MAX_DECISION_MS_DESKTOP,
                "a phone must not be allowed more time per decision than a desktop");
        assertTrue(FlatMonteCarloPicker.MOBILE_TURN_MS < FlatMonteCarloPicker.MAX_TURN_MS_DESKTOP);
        assertTrue(FlatMonteCarloPicker.LOOKAHEAD_PERCENT_MOBILE < FlatMonteCarloPicker.LOOKAHEAD_PERCENT_DESKTOP);
    }

    // ===================================================================================== maximized

    @Test
    public void maximizedLooksAtTheOpponentsTurnOnEveryDecision() {
        final FlatMonteCarloPicker.Config c = new FlatMonteCarloPicker.Config();
        c.maximized();
        assertTrue(c.opponentLookaheadPercent >= FlatMonteCarloPicker.LOOKAHEAD_PERCENT_DESKTOP);
        assertEquals(c.lookaheadMaxCandidates, 0, "0 = always: no limit on the number of lines");
    }

    @Test
    public void maximizedKeepsALargerLookahead() {
        final FlatMonteCarloPicker.Config c = new FlatMonteCarloPicker.Config();
        c.opponentLookaheadPercent = 90;
        c.maximized();
        assertEquals(c.opponentLookaheadPercent, 90);
    }

    // ===================================================================================== forDecision

    private static FlatMonteCarloPicker.Config limited() {
        final FlatMonteCarloPicker.Config c = new FlatMonteCarloPicker.Config();
        c.opponentLookaheadPercent = 35;
        c.lookaheadMaxCandidates = 3;
        c.maxCandidates = 7;
        return c;
    }

    @Test
    public void smallDecisionsKeepTheLookahead() {
        final FlatMonteCarloPicker.Config c = limited();
        assertSame(c.forDecision(1), c);
        assertSame(c.forDecision(2), c);
        assertSame(c.forDecision(3), c, "at the limit: still looks ahead");
    }

    @Test
    public void bigDecisionsDropTheLookaheadOnACopy() {
        final FlatMonteCarloPicker.Config c = limited();
        final FlatMonteCarloPicker.Config copy = c.forDecision(4);
        assertNotSame(copy, c);
        assertEquals(copy.opponentLookaheadPercent, 0);
        assertEquals(copy.maxCandidates, 7, "everything else is kept");
        assertEquals(c.opponentLookaheadPercent, 35, "the original is not touched: the next decision may be a small one");
    }

    @Test
    public void everyLineOfOneDecisionGetsTheSameSetting() {
        final FlatMonteCarloPicker.Config c = limited();
        final FlatMonteCarloPicker.Config a = c.forDecision(5);
        final FlatMonteCarloPicker.Config b = c.forDecision(5);
        assertEquals(a.opponentLookaheadPercent, b.opponentLookaheadPercent, "lines must be compared like with like");
    }

    @Test
    public void noLimitMeansAlways() {
        final FlatMonteCarloPicker.Config c = limited();
        c.lookaheadMaxCandidates = 0;
        assertSame(c.forDecision(50), c);
    }

    @Test
    public void noLookaheadMeansNothingToDrop() {
        final FlatMonteCarloPicker.Config c = limited();
        c.opponentLookaheadPercent = 0;
        assertSame(c.forDecision(50), c);
    }

    // ===================================================================================== affordableLines

    @Test
    public void aSlowDeviceKeepsAtLeastTwoLines() {
        assertEquals(FlatMonteCarloPicker.affordableLines(0, 1_000_000, 4, 10), 2, "no time left: still a comparison");
        assertEquals(FlatMonteCarloPicker.affordableLines(-5, 1_000_000, 4, 10), 2, "negative time counts as none");
        assertEquals(FlatMonteCarloPicker.affordableLines(10, 1_000_000, 4, 10), 2);
    }

    @Test
    public void aFastDeviceKeepsEveryLine() {
        assertEquals(FlatMonteCarloPicker.affordableLines(TimeUnit.SECONDS.toNanos(10), 1_000_000, 4, 6), 6);
        assertEquals(FlatMonteCarloPicker.affordableLines(1_000_000, 0, 4, 6), 6, "a rollout that took no time at all");
    }

    @Test
    public void everyLineGetsTheRoundsItNeeds() {
        // 10 more rollouts fit, the first has been done: 11 samples in all
        assertEquals(FlatMonteCarloPicker.affordableLines(10_000_000, 1_000_000, 2, 10), 5, "2 samples each");
        assertEquals(FlatMonteCarloPicker.affordableLines(10_000_000, 1_000_000, 3, 10), 3, "3 samples each");
        assertEquals(FlatMonteCarloPicker.affordableLines(10_000_000, 1_000_000, 4, 10), 2, "4 samples each: floor of two lines");
    }

    @Test
    public void oneRoundIsTreatedAsTwo() {
        assertEquals(FlatMonteCarloPicker.affordableLines(10_000_000, 1_000_000, 1, 10),
                FlatMonteCarloPicker.affordableLines(10_000_000, 1_000_000, 2, 10), "one sample has no spread to judge by");
    }

    @Test
    public void neverMoreLinesThanThereAre() {
        assertEquals(FlatMonteCarloPicker.affordableLines(TimeUnit.HOURS.toNanos(1), 1, 2, 3), 3);
    }

    @Test
    public void deviceScalingIsOnByDefault() {
        assertTrue(new FlatMonteCarloPicker.Config().scaleToDevice);
    }
}