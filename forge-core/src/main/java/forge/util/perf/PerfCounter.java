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
package forge.util.perf;

/**
 * Work counters recorded for a single AI decision.
 *
 * <p>These exist so that timings can be normalised: a decision that took twice as long because it
 * saw twice as many candidates is not the same finding as one that took twice as long over the same
 * input. Every counter therefore describes an amount of work performed or of data materialised, not
 * a duration.</p>
 *
 * <p>Counters are diagnostic only. Nothing in the rules or AI engine may read them, and recording
 * them must never influence a decision.</p>
 */
public enum PerfCounter {
    /** Priority decisions started (one per {@code AiController.chooseSpellAbilityToPlay}). */
    DECISIONS("decisions"),

    // ---- conventional candidate generation ----
    /** Cards offered as action sources by {@code ComputerUtilAbility.getAvailableCards}. */
    CANDIDATE_CARDS("candidateCards"),
    /** Spell abilities collected from those cards. */
    CANDIDATE_ABILITIES("candidateAbilities"),
    /** Abilities after alternate/optional cost expansion. */
    CANDIDATE_ABILITIES_WITH_ALT_COSTS("candidateAbilitiesWithAltCosts"),
    /** Full heuristic play evaluations ({@code canPlayAndPayFor}). */
    CAN_PLAY_CHECKS("canPlayChecks"),
    /** {@code ComputerUtilCost.canPayCost} invocations. */
    CAN_PAY_COST_CHECKS("canPayCostChecks"),
    /** {@code ComputerUtilMana.canPayManaCost} invocations. */
    MANA_FEASIBILITY_CHECKS("manaFeasibilityChecks"),
    /** Structural {@code CostAdjustment.adjust} results reused across one feasibility call. */
    COST_ADJUSTMENT_REUSES("costAdjustmentReuses"),
    /** AI evaluation watchdog timeouts. */
    EVAL_TIMEOUTS("evalTimeouts"),
    /** Evaluation runs abandoned because they ignored cancellation. */
    EVAL_WORKERS_ABANDONED("evalWorkersAbandoned"),

    // ---- candidate ordering ----
    /** Per-decision comparator facts computed for a spell ability. */
    SORT_FACTS_COMPUTED("sortFactsComputed"),
    /** Comparator fact lookups answered from the per-decision cache. */
    SORT_FACT_HITS("sortFactHits"),

    // ---- targeting ----
    /** {@code TargetRestrictions.getAllCandidates} invocations. */
    TARGET_CANDIDATE_QUERIES("targetCandidateQueries"),
    /** Target candidates actually placed into a returned list. */
    TARGET_CANDIDATES_MATERIALIZED("targetCandidatesMaterialized"),
    /** {@code TargetRestrictions.hasAtLeastCandidates} invocations. */
    TARGET_THRESHOLD_QUERIES("targetThresholdQueries"),
    /** Entities examined by those bounded traversals before they stopped. */
    TARGET_CANDIDATES_VISITED("targetCandidatesVisited"),

    // ---- zone aggregation ----
    /** Aggregate {@code Game.getCardsIn(ZoneType)} queries. */
    ZONE_AGGREGATE_QUERIES("zoneAggregateQueries"),
    /** Cards observed through those aggregate queries. */
    ZONE_CARDS_MATERIALIZED("zoneCardsMaterialized"),

    // ---- rules-derived state ----
    /** {@code GameAction.checkStaticAbilities} invocations. */
    STATIC_ABILITY_CHECKS("staticAbilityChecks"),
    /** {@code GameAction.checkStateEffects} invocations. */
    STATE_EFFECT_CHECKS("stateEffectChecks"),
    /** {@code TriggerHandler.resetActiveTriggers} invocations. */
    TRIGGER_RESETS("triggerResets"),
    /** {@code TriggerHandler.runWaitingTriggers} invocations. */
    WAITING_TRIGGER_RUNS("waitingTriggerRuns"),
    /** {@code ReplacementHandler.getReplacementList} invocations. */
    REPLACEMENT_LOOKUPS("replacementLookups"),
    /** Replacement effects returned as applicable by those lookups. */
    REPLACEMENT_EFFECTS_FOUND("replacementEffectsFound"),
    /** Derived CardState trait views served from their per-state cache. */
    TRAIT_CACHE_HITS("traitCacheHits"),
    /** Derived CardState trait views rebuilt after first use or invalidation. */
    TRAIT_CACHE_REBUILDS("traitCacheRebuilds"),

    // ---- simulation ----
    /** {@code GameCopier.makeCopy} invocations. */
    GAME_COPIES("gameCopies"),
    /** Cards copied by those invocations. */
    GAME_COPY_CARDS("gameCopyCards"),
    /** Game copies made purely for the state evaluator's combat lookahead. */
    COMBAT_LOOKAHEAD_COPIES("combatLookaheadCopies"),
    /** {@code GameStateEvaluator.getScoreForGameState} invocations. */
    SCORE_EVALUATIONS("scoreEvaluations"),
    /** Root scores taken from the caller instead of being re-evaluated per simulation branch. */
    BASELINE_SCORE_REUSES("baselineScoreReuses"),
    /** Candidate abilities evaluated by the full-simulation picker. */
    SIMULATED_CANDIDATES("simulatedCandidates"),
    /** Individual target/mode branches simulated inside those candidates. */
    SIMULATION_BRANCHES("simulationBranches"),

    // ---- combat ----
    /** {@code AttackConstraints} constructions. */
    ATTACK_CONSTRAINTS_BUILT("attackConstraintsBuilt"),
    /** Possible attackers seen while building those constraints. */
    POSSIBLE_ATTACKERS("possibleAttackers"),
    /** Attack declarations performed by the AI. */
    ATTACK_DECLARATIONS("attackDeclarations"),
    /** Attackers actually declared. */
    ATTACKERS_DECLARED("attackersDeclared"),
    /** Block assignments performed by the AI. */
    BLOCK_DECLARATIONS("blockDeclarations"),
    /** Creatures available as blockers at assignment time. */
    POSSIBLE_BLOCKERS("possibleBlockers"),
    /** {@code AiBlockController.makeGangBlocks} passes. */
    GANG_BLOCK_PASSES("gangBlockPasses"),

    // ---- randomness ----
    /** Primitive draws taken from the process-global generator (only counted with a tracing generator installed). */
    RANDOM_DRAWS("randomDraws");

    private final String jsonName;

    PerfCounter(final String jsonName) {
        this.jsonName = jsonName;
    }

    /** Stable name used in machine-readable output; never derive it from {@link #name()}. */
    public String jsonName() {
        return jsonName;
    }
}
