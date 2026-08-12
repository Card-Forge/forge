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
import forge.game.GameStateDigest;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import forge.util.perf.DecisionKind;
import forge.util.perf.DecisionRecord;
import forge.util.perf.PerfCounter;
import forge.util.perf.PerfProbe;
import forge.util.perf.PerfReport;
import forge.util.perf.PerfSink;
import forge.util.perf.PerfTimer;
import forge.util.perf.TracingRandom;

/**
 * The zero-behaviour-change guarantee for the AI measurement probes.
 *
 * <p>The whole performance programme rests on being able to compare a baseline build against an
 * optimised one. That comparison is worthless if turning measurement on is itself a behaviour
 * change, so this test plays the same seeded scenario with probing off and then on and requires the
 * resulting canonical game states to be identical — not merely similar, and not merely
 * "same winner".</p>
 *
 * <p>It also checks the converse: that probing actually recorded the decisions and the work counts
 * it is supposed to, so a silently dead probe cannot pass by producing no difference at all.</p>
 */
public class AiPerfInstrumentationTest extends AITest {
    private static final long SEED = 20260812L;

    @AfterMethod
    public void restoreProbeState() {
        PerfProbe.reset();
        MyRandom.setRandom(new Random());
    }

    /**
     * Builds and plays one deterministic AI turn: a main phase with castable removal, a combat with
     * attackers and blockers on both sides, and a second main phase. That covers all three decision
     * kinds and the rules paths the probes sit on.
     */
    private String playSeededTurn() {
        // TracingRandom emits exactly what new Random(SEED) would, so the two runs share a stream.
        MyRandom.setRandom(new TracingRandom(SEED));

        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);

        opponent.setLife(12, null);
        ai.setLife(12, null);

        for (int i = 0; i < 4; i++) {
            addCard("Mountain", ai);
        }
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Shock", ai, ZoneType.Hand);

        final List<Card> attackers = addCards("Grizzly Bears", 3, ai);
        for (final Card attacker : attackers) {
            attacker.setSickness(false);
        }
        final List<Card> blockers = addCards("Runeclaw Bear", 2, opponent);
        for (final Card blocker : blockers) {
            blocker.setSickness(false);
        }

        fillLibrary(ai, 12);
        fillLibrary(opponent, 12);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);

        return GameStateDigest.digest(game);
    }

    @Test(timeOut = 300000)
    public void probingDoesNotChangeTheResultingGameState() {
        PerfProbe.reset();
        final String withoutProbes = playSeededTurn();

        PerfProbe.reset();
        PerfProbe.setEnabled(true);
        final PerfReport report = new PerfReport("parity");
        PerfProbe.addSink(report);
        final String withProbes;
        try {
            withProbes = playSeededTurn();
        } finally {
            PerfProbe.setEnabled(false);
        }

        Assert.assertEquals(withProbes, withoutProbes,
                "enabling AI measurement must not change the canonical game state");

        // A probe that records nothing would trivially satisfy the assertion above.
        Assert.assertTrue(report.getDecisionCount() > 0, "probing must have recorded decisions");
    }

    @Test(timeOut = 300000)
    public void probingRecordsDecisionsAndTheWorkThatNormalisesThem() {
        PerfProbe.reset();
        PerfProbe.setEnabled(true);
        final PerfReport report = new PerfReport("counters");
        PerfProbe.addSink(report);
        try {
            playSeededTurn();
        } finally {
            PerfProbe.setEnabled(false);
        }

        Assert.assertTrue(report.getDecisionCount(DecisionKind.PRIORITY) > 0,
                "priority decisions must be recorded");

        final var priority = report.getTotals(DecisionKind.PRIORITY);
        Assert.assertNotNull(priority);
        Assert.assertTrue(priority.get(PerfCounter.DECISIONS) > 0L);
        Assert.assertTrue(priority.get(PerfCounter.CANDIDATE_CARDS) > 0L,
                "candidate generation must be counted");
        Assert.assertTrue(priority.get(PerfCounter.ZONE_AGGREGATE_QUERIES) > 0L,
                "aggregate zone queries must be counted");
        Assert.assertTrue(priority.getNanos(PerfTimer.DECISION) > 0L,
                "the decision span must be timed");

        // Latency percentiles are the headline numbers; they must be present and ordered.
        final long median = report.getDurationPercentileNanos(DecisionKind.PRIORITY, 50.0d);
        final long p99 = report.getDurationPercentileNanos(DecisionKind.PRIORITY, 99.0d);
        Assert.assertTrue(median > 0L);
        Assert.assertTrue(p99 >= median);

        // Rules work performed between decisions — phase transitions, stack resolution, combat setup
        // — is genuinely outside any decision window, so it lands only on the global accumulator.
        // Both scopes have to work: per-decision attribution for latency, global totals for a game.
        Assert.assertTrue(PerfProbe.getGlobal().get(PerfCounter.STATIC_ABILITY_CHECKS) > 0L,
                "static ability checks must be counted globally");
        Assert.assertTrue(PerfProbe.getGlobal().get(PerfCounter.STATE_EFFECT_CHECKS) > 0L,
                "state-based action passes must be counted globally");
        Assert.assertTrue(PerfProbe.getGlobal().get(PerfCounter.ATTACK_CONSTRAINTS_BUILT) > 0L,
                "attack constraint construction must be counted");
        Assert.assertTrue(PerfProbe.getGlobal().get(PerfCounter.RANDOM_DRAWS) > 0L,
                "the tracing generator must observe the draws the game takes");
    }

    @Test(timeOut = 300000)
    public void tracingProducesAStableOrderedDecisionTrace() {
        PerfProbe.reset();
        PerfProbe.setTracing(true);
        final TraceCollector first = new TraceCollector();
        PerfProbe.addSink(first);
        try {
            playSeededTurn();
        } finally {
            PerfProbe.setTracing(false);
            PerfProbe.setEnabled(false);
        }

        PerfProbe.reset();
        PerfProbe.setTracing(true);
        final TraceCollector second = new TraceCollector();
        PerfProbe.addSink(second);
        try {
            playSeededTurn();
        } finally {
            PerfProbe.setTracing(false);
            PerfProbe.setEnabled(false);
        }

        Assert.assertFalse(first.entries.isEmpty(), "the scenario must produce trace entries");
        // Exact trace identity is the plan's default pass criterion for every optimisation; if it
        // cannot be reproduced for an unchanged build, it cannot judge a changed one.
        Assert.assertEquals(second.entries, first.entries,
                "two runs of the same fixture must produce an identical decision trace");
    }

    /** Collects every trace entry in decision order. */
    private static final class TraceCollector implements PerfSink {
        private final List<String> entries = new ArrayList<>();

        @Override
        public void onDecision(final DecisionRecord record) {
            entries.addAll(record.getTrace());
        }
    }
}
