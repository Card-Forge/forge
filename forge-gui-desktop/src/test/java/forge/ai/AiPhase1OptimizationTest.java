/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.GameTraceDescriptors;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.cost.CostPayment;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.perf.PerfCounter;
import forge.util.perf.PerfProbe;

/**
 * Behaviour-equivalence tests for the phase 1 AI performance work.
 *
 * <p>Every optimisation here claims to reach the same answer by doing less. Each test therefore
 * compares the cheap path against the expensive one it replaced on a real game state, rather than
 * checking that the cheap path merely returns something plausible. Where an optimisation can silently
 * turn itself off, the test also asserts that it actually engaged, so a disabled fast path cannot
 * pass by falling back to the code it was measured against.</p>
 */
public class AiPhase1OptimizationTest extends AITest {
    @AfterMethod
    public void restoreProbeState() {
        PerfProbe.reset();
        MyRandom.setRandom(new Random());
    }

    /**
     * A board with players, creatures, an artifact and an enchantment on both sides, and a hand of
     * spells whose targeting restrictions differ from one another.
     */
    private Game targetRichGame() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);

        for (final Player p : game.getPlayers()) {
            addCards("Grizzly Bears", 3, p);
            addCard("Mountain", p);
            addCard("Forest", p);
            addCard("Sol Ring", p);
            addCard("Pacifism", p);
        }
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Doom Blade", ai, ZoneType.Hand);
        addCardToZone("Disenchant", ai, ZoneType.Hand);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        addCardToZone("Shock", opponent, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        return game;
    }

    /** Every targeting ability the AI could look at on that board. */
    private List<SpellAbility> targetingAbilities(final Game game, final Player ai) {
        final CardCollection cards = ComputerUtilAbility.getAvailableCards(game, ai);
        final List<SpellAbility> targeting = new ArrayList<>();
        for (final SpellAbility sa : ComputerUtilAbility.getSpellAbilities(cards, ai)) {
            sa.setActivatingPlayer(ai);
            SpellAbility part = sa;
            while (part != null) {
                if (part.usesTargeting()) {
                    part.setActivatingPlayer(ai);
                    targeting.add(part);
                }
                part = part.getSubAbility();
            }
        }
        return targeting;
    }

    /**
     * The bounded traversal must answer exactly what counting every candidate would have answered,
     * for every threshold, not just for the "is there one" case its callers happen to use today.
     */
    @Test(timeOut = 300000)
    public void boundedTargetCountingAgreesWithFullCounting() {
        final Game game = targetRichGame();
        final Player ai = game.getPlayers().get(1);

        final List<SpellAbility> targeting = targetingAbilities(game, ai);
        Assert.assertFalse(targeting.isEmpty(), "the fixture must produce abilities that target");

        int nonEmpty = 0;
        for (final SpellAbility sa : targeting) {
            final TargetRestrictions tgt = sa.getTargetRestrictions();
            final int actual = tgt.getNumCandidates(sa);
            if (actual > 0) {
                nonEmpty++;
            }
            // -1 and 0 are trivially satisfied; above the real count it must give up and say no.
            for (int required = -1; required <= actual + 2; required++) {
                Assert.assertEquals(tgt.hasAtLeastCandidates(sa, required), actual >= required,
                        "hasAtLeastCandidates(" + required + ") disagreed with getNumCandidates()=" + actual
                                + " for " + GameTraceDescriptors.describe(sa));
            }
            // and the full count itself must be unchanged by the bounded traversal having run
            Assert.assertEquals(tgt.getNumCandidates(sa), actual,
                    "bounded traversal changed the candidate count for " + GameTraceDescriptors.describe(sa));
        }
        Assert.assertTrue(nonEmpty > 0, "the fixture must produce abilities that have candidates");
    }

    /**
     * The comparator facts are a cache, so the ordered candidate list has to come out byte for byte
     * the same as the one the uncached comparators produce. Comparing the chosen ability would not
     * be enough: a different order can still choose the same first playable ability today and a
     * different one tomorrow.
     */
    @Test(timeOut = 300000)
    public void perSortFactsProduceTheSameOrderedCandidateList() {
        final Game game = targetRichGame();
        final Player ai = game.getPlayers().get(1);

        final CardCollection cards = ComputerUtilAbility.getAvailableCards(game, ai);
        final List<SpellAbility> source = ComputerUtilAbility.getSpellAbilities(cards, ai);
        for (final SpellAbility sa : source) {
            sa.setActivatingPlayer(ai);
        }
        Assert.assertTrue(source.size() > 1, "the fixture must produce something worth sorting");

        final List<SpellAbility> withoutFacts = new ArrayList<>(source);
        withoutFacts.sort(ComputerUtilAbility.saEvaluator);
        ComputerUtilAbility.sortCreatureSpells(withoutFacts);

        final List<SpellAbility> withFacts = new ArrayList<>(source);
        AiController.sortCandidates(withFacts);

        Assert.assertEquals(describeAll(withFacts), describeAll(withoutFacts),
                "the per-decision comparator facts changed the candidate ordering");
    }

    private static List<String> describeAll(final List<SpellAbility> abilities) {
        final List<String> described = new ArrayList<>(abilities.size());
        for (final SpellAbility sa : abilities) {
            described.add(GameTraceDescriptors.describe(sa));
        }
        return described;
    }

    /**
     * Reusing one structural cost adjustment across the two halves of a feasibility check must not
     * change what the AI believes it can pay for. Assertions are on under Surefire, so every reuse
     * this exercises is additionally shadow-checked against adjusting a second time.
     */
    @Test(timeOut = 300000)
    public void reusedCostAdjustmentGivesTheSamePayableVerdict() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);

        for (int i = 0; i < 3; i++) {
            addCard("Mountain", ai);
            addCard("Forest", ai);
        }
        addCard("Grizzly Bears", ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Shock", ai, ZoneType.Hand);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        addCardToZone("Ancestral Vision", ai, ZoneType.Hand);
        addCardToZone("Emrakul, the Aeons Torn", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        PerfProbe.reset();
        PerfProbe.setEnabled(true);
        int checked = 0;
        try {
            final CardCollection cards = ComputerUtilAbility.getAvailableCards(game, ai);
            for (final SpellAbility sa : ComputerUtilAbility.getSpellAbilities(cards, ai)) {
                if (sa.isManaAbility()) {
                    continue;
                }
                sa.setActivatingPlayer(ai);
                final Cost cost = sa.getPayCosts();

                final boolean optimised = ComputerUtilCost.canPayCost(sa, ai, false);
                // the shape the check had before the adjustment was shared between its two halves
                final boolean asBefore = ComputerUtilMana.canPayManaCost(cost, sa, ai, 0, false)
                        && CostPayment.canPayAdditionalCosts(cost, sa, false, ai);

                Assert.assertEquals(optimised, asBefore,
                        "cost feasibility changed for " + GameTraceDescriptors.describe(sa));
                checked++;
            }
        } finally {
            PerfProbe.setEnabled(false);
        }

        Assert.assertTrue(checked > 0, "the fixture must produce abilities with a cost to check");
        Assert.assertTrue(PerfProbe.getGlobal().get(PerfCounter.COST_ADJUSTMENT_REUSES) > 0L,
                "no adjustment was actually reused, so this proved nothing");
    }

    /**
     * Forced attackers used to be worked out on the common pool, declaring into the live combat from
     * several threads at once; which creatures ended up attacking, and in what order, depended on how
     * the pool interleaved. Running the loop serially has to produce one answer, every time.
     */
    @Test(timeOut = 300000)
    public void forcedAttackersAreDeclaredDeterministically() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        opponent.setLife(20, null);

        final List<Card> forced = addCards("Grizzly Bears", 6, ai);
        for (final Card c : forced) {
            c.setSickness(false);
            c.setSVar("MustAttack", "True");
        }
        // something for the defender to make blocks with, so the attack is not trivially forced
        for (final Card c : addCards("Runeclaw Bear", 3, opponent)) {
            c.setSickness(false);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        List<String> firstRun = null;
        for (int i = 0; i < 5; i++) {
            final Combat combat = new Combat(ai);
            new AiAttackController(ai).declareAttackers(combat);

            final List<String> declared = new ArrayList<>();
            for (final Card attacker : combat.getAttackers()) {
                declared.add(GameTraceDescriptors.describe(attacker) + " -> "
                        + GameTraceDescriptors.describe(combat.getDefenderByAttacker(attacker)));
            }
            if (firstRun == null) {
                Assert.assertEquals(declared.size(), forced.size(),
                        "every creature that must attack has to be declared");
                firstRun = declared;
            } else {
                Assert.assertEquals(declared, firstRun,
                        "the same board declared a different attack on run " + i);
            }
        }
    }

    /**
     * The watchdog boundary is still there, and it no longer costs a thread per decision.
     *
     * <p>The bound is deliberately loose: the point is that the worker count tracks how many
     * evaluations are in flight at once — a small number — instead of how many decisions the game
     * has made.</p>
     */
    @Test(timeOut = 300000)
    public void candidateEvaluationReusesItsWorker() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);

        for (int i = 0; i < 4; i++) {
            addCard("Mountain", ai);
        }
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Shock", ai, ZoneType.Hand);
        for (final Card c : addCards("Grizzly Bears", 2, ai)) {
            c.setSickness(false);
        }
        addCards("Runeclaw Bear", 2, opponent);
        fillLibrary(ai, 12);
        fillLibrary(opponent, 12);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);

        PerfProbe.reset();
        PerfProbe.setEnabled(true);
        final long decisions;
        try {
            playUntilNextTurn(game);
            decisions = PerfProbe.getGlobal().get(PerfCounter.DECISIONS);
        } finally {
            PerfProbe.setEnabled(false);
        }

        Assert.assertTrue(decisions > 4L, "the fixture must make several priority decisions");
        Assert.assertTrue(AiEvaluationExecutor.getWorkerCount() <= 8,
                "evaluation workers should be reused across decisions, found "
                        + AiEvaluationExecutor.getWorkerCount() + " for " + decisions + " decisions");
        Assert.assertEquals(PerfProbe.getGlobal().get(PerfCounter.EVAL_WORKERS_ABANDONED), 0L,
                "no evaluation should have had to be abandoned");
    }
}
