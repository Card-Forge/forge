package forge.ai.simulation;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.testng.annotations.Test;

import forge.ai.PlayerControllerAi;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/** How rollouts are judged and compared, and the opponent's answers inside a rollout. */
public class FlatMonteCarloRolloutRulesTest extends FlatMcTestBase {

    @Test
    public void onlyTheSamplesThatEveryLineHasAreShared() {
        final int[] count = { 4, 3, 1, 2 };
        final boolean[] invalid = { false, false, true, false };
        final boolean[] pruned = { false, false, false, true };
        assertEquals(FlatMonteCarloPicker.sharedSamples(count, invalid, pruned, 4), 3);
        assertEquals(FlatMonteCarloPicker.meanOfFirst(new long[] { 10, 20, 30, 1000 }, 3), 20.0);
    }

    @Test
    public void twoSamplesAreNotEnoughToDropALine() {
        final long[][] vals = { { 500, 500, 500 }, { 100, 100, 100 } };
        final boolean[] pruned = new boolean[2];
        assertEquals(FlatMonteCarloPicker.pruneBehind(vals, new int[] { 2, 2 }, new boolean[2], pruned, 2, 80), 0);
        assertEquals(FlatMonteCarloPicker.pruneBehind(vals, new int[] { 3, 3 }, new boolean[2], pruned, 2, 80), 1);
        assertTrue(pruned[1]);
    }

    @Test
    public void dyingOnTheOpponentsTurnCostsTheFullPenalty() {
        assertEquals(FlatMonteCarloPicker.deathOnOpponentTurn(1000), 1000 - FlatMonteCarloPicker.LOOKAHEAD_DEATH_PENALTY);
        assertTrue(FlatMonteCarloPicker.deathOnOpponentTurn(-19000) > FlatMonteCarloPicker.LOSS_VALUE);
    }

    /** A lethal Lava Axe into an untapped Counterspell: every card the opponent can be dealt is one. */
    private FlatMonteCarloPicker.Result axeIntoCounterspell(final boolean opponentResponses) {
        setUpGame();
        put("Mountain", 5, ai, ZoneType.Battlefield);
        put("Island", 2, opp, ZoneType.Battlefield);
        final SpellAbility axe = put("Lava Axe", ai, ZoneType.Hand).getFirstSpellAbility();
        put("Counterspell", opp, ZoneType.Hand);
        put("Mountain", 12, ai, ZoneType.Library);
        put("Counterspell", 12, opp, ZoneType.Library);
        opp.setLife(5, null);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        axe.setActivatingPlayer(ai);

        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(
                ((PlayerControllerAi) ai.getController()).getAi(), ai);
        cfg.opponentResponses = opponentResponses;
        cfg.budgetNanos = TimeUnit.SECONDS.toNanos(10);
        // on the AI's turn passing is only compared when there is an instant to hold, so RESPOND mode (which always
        // compares passing) is not wanted here: a second play gives the comparison its other line
        final SpellAbility bolt = put("Shock", ai, ZoneType.Hand).getFirstSpellAbility();
        bolt.setActivatingPlayer(ai);
        FlatMonteCarloPicker.applyTarget(bolt, opp);
        FlatMonteCarloPicker.applyTarget(axe, opp);
        cfg.targetVariants = 0;
        return FlatMonteCarloPicker.choose(ai, Arrays.asList(axe, bolt), cfg, null);
    }

    @Test(timeOut = 120000)
    public void withoutResponsesTheBurnSpellAlwaysWins() {
        final FlatMonteCarloPicker.Result r = axeIntoCounterspell(false);
        assertEquals(r.reason, "wins", String.valueOf(r));
        assertEquals(r.rollouts, FlatMonteCarloPicker.WIN_CONFIRM_SAMPLES, "a win is confirmed: " + r);
    }

    @Test(timeOut = 120000)
    public void theOpponentCountersItInTheRollouts() {
        final FlatMonteCarloPicker.Result r = axeIntoCounterspell(true);
        assertNotEquals(r.reason, "wins", String.valueOf(r));
    }
}
