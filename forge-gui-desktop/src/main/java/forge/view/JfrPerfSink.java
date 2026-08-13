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
package forge.view;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;
import jdk.jfr.Timespan;

import forge.util.perf.DecisionRecord;
import forge.util.perf.PerfCounter;
import forge.util.perf.PerfSink;
import forge.util.perf.PerfTimer;

/**
 * Emits one Java Flight Recorder event per AI decision.
 *
 * <p>This lives in the desktop module on purpose. {@code jdk.jfr} does not exist on Android, so
 * referencing it from {@code forge-core}, {@code forge-game} or {@code forge-ai} would break the
 * mobile builds at class-load time. {@link PerfSink} is the seam that keeps it out of them.</p>
 *
 * <p>The event carries the same counters the JSON report does, so a JFR recording taken with
 * {@code -XX:StartFlightRecording=settings=profile} can be correlated with method-level CPU and
 * allocation samples <em>and</em> normalised by how much work each decision actually faced. Without
 * those counts, a slower decision cannot be told apart from a bigger one.</p>
 */
public final class JfrPerfSink implements PerfSink {
    @Name("forge.AiDecision")
    @Label("Forge AI decision")
    @Category({"Forge", "AI"})
    @Description("One AI decision, with the work counts needed to normalise its duration")
    @StackTrace(false)
    static final class AiDecisionEvent extends Event {
        @Label("Decision id")
        long decisionId;
        @Label("Kind")
        String kind;
        @Label("Player")
        String player;
        @Label("Turn")
        int turn;
        @Label("Phase")
        String phase;
        // JFR derives duration from begin()/end(); the record is only complete once the decision
        // has finished, so the measured span is carried explicitly instead.
        @Label("Duration")
        @Timespan(Timespan.NANOSECONDS)
        long durationNanos;

        @Label("Candidate abilities")
        long candidateAbilities;
        @Label("Can-play checks")
        long canPlayChecks;
        @Label("Can-pay-cost checks")
        long canPayCostChecks;
        @Label("Target candidates materialised")
        long targetCandidates;
        @Label("Aggregate zone queries")
        long zoneQueries;
        @Label("Static ability checks")
        long staticChecks;
        @Label("Replacement lookups")
        long replacementLookups;
        @Label("Card trait cache hits")
        long traitCacheHits;
        @Label("Card trait cache rebuilds")
        long traitCacheRebuilds;
        @Label("Game copies")
        long gameCopies;
        @Label("Cards copied")
        long copiedCards;
        @Label("Simulation branches")
        long simulationBranches;
        @Label("Score evaluations")
        long scoreEvaluations;

        @Label("Game copy nanos")
        long gameCopyNanos;
        @Label("Score evaluation nanos")
        long scoreEvaluationNanos;
        @Label("Static ability nanos")
        long staticAbilityNanos;
        @Label("Replacement lookup nanos")
        long replacementLookupNanos;
    }

    /**
     * Registers a JFR sink, or returns null when this JVM has no usable JFR (some JDK builds ship
     * without it). A missing recorder is never fatal: the JSON report is the primary artefact.
     */
    public static JfrPerfSink createIfAvailable() {
        try {
            // Touch the event class so a missing jdk.jfr module fails here rather than mid-game.
            new AiDecisionEvent();
            return new JfrPerfSink();
        } catch (final Throwable t) {
            System.out.println("JFR events unavailable, continuing without them: " + t);
            return null;
        }
    }

    private JfrPerfSink() {
    }

    @Override
    public void onDecision(final DecisionRecord record) {
        final AiDecisionEvent event = new AiDecisionEvent();
        if (!event.isEnabled()) {
            return;
        }
        event.decisionId = record.getId();
        event.kind = record.getKind().jsonName();
        event.player = record.getPlayer();
        event.turn = record.getTurn();
        event.phase = record.getPhase();
        event.durationNanos = record.getDurationNanos();

        event.candidateAbilities = record.get(PerfCounter.CANDIDATE_ABILITIES);
        event.canPlayChecks = record.get(PerfCounter.CAN_PLAY_CHECKS);
        event.canPayCostChecks = record.get(PerfCounter.CAN_PAY_COST_CHECKS);
        event.targetCandidates = record.get(PerfCounter.TARGET_CANDIDATES_MATERIALIZED);
        event.zoneQueries = record.get(PerfCounter.ZONE_AGGREGATE_QUERIES);
        event.staticChecks = record.get(PerfCounter.STATIC_ABILITY_CHECKS);
        event.replacementLookups = record.get(PerfCounter.REPLACEMENT_LOOKUPS);
        event.traitCacheHits = record.get(PerfCounter.TRAIT_CACHE_HITS);
        event.traitCacheRebuilds = record.get(PerfCounter.TRAIT_CACHE_REBUILDS);
        event.gameCopies = record.get(PerfCounter.GAME_COPIES);
        event.copiedCards = record.get(PerfCounter.GAME_COPY_CARDS);
        event.simulationBranches = record.get(PerfCounter.SIMULATION_BRANCHES);
        event.scoreEvaluations = record.get(PerfCounter.SCORE_EVALUATIONS);

        event.gameCopyNanos = record.getNanos(PerfTimer.GAME_COPY);
        event.scoreEvaluationNanos = record.getNanos(PerfTimer.SCORE_EVALUATION);
        event.staticAbilityNanos = record.getNanos(PerfTimer.STATIC_ABILITIES);
        event.replacementLookupNanos = record.getNanos(PerfTimer.REPLACEMENT_LOOKUP);

        event.commit();
    }
}
