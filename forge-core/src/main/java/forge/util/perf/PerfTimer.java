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
 * Wall-clock spans recorded for a single AI decision.
 *
 * <p>Spans are <em>inclusive</em>: {@link #SCORE_EVALUATION} contains any {@link #GAME_COPY} the
 * evaluator makes for combat lookahead, so timers must not be summed across the enum and treated as
 * a decision breakdown. Re-entrant occurrences of the <em>same</em> timer are folded into the
 * outermost span by {@link PerfProbe}, so a recursive rules path is counted once rather than
 * multiplying its own elapsed time.</p>
 */
public enum PerfTimer {
    /** The entire priority decision, measured at the AI controller boundary. */
    DECISION("decision"),
    /** Building the candidate card/ability lists for a conventional decision. */
    CANDIDATE_GENERATION("candidateGeneration"),
    /** Sorting candidates with the spell ability comparator. */
    CANDIDATE_SORT("candidateSort"),
    /** The candidate evaluation loop, including alternate-cost expansion and heuristics. */
    CANDIDATE_EVALUATION("candidateEvaluation"),
    /** A whole attack declaration. */
    DECLARE_ATTACKERS("declareAttackers"),
    /** A whole block assignment. */
    DECLARE_BLOCKERS("declareBlockers"),
    /** The gang-block search inside a block assignment. */
    GANG_BLOCKS("gangBlocks"),
    /** {@code AttackConstraints} construction. */
    ATTACK_CONSTRAINTS("attackConstraints"),
    /** A single {@code GameCopier.makeCopy}. */
    GAME_COPY("gameCopy"),
    /** A single {@code GameStateEvaluator.getScoreForGameState}, including combat lookahead. */
    SCORE_EVALUATION("scoreEvaluation"),
    /** One simulated target/mode branch, including its copy, resolution and scoring. */
    SIMULATION_BRANCH("simulationBranch"),
    /** {@code GameAction.checkStaticAbilities}. */
    STATIC_ABILITIES("staticAbilities"),
    /** {@code GameAction.checkStateEffects}. */
    STATE_EFFECTS("stateEffects"),
    /** {@code TriggerHandler.resetActiveTriggers}. */
    TRIGGER_RESET("triggerReset"),
    /** {@code ReplacementHandler.getReplacementList}. */
    REPLACEMENT_LOOKUP("replacementLookup"),
    /** {@code TargetRestrictions.getAllCandidates}. */
    TARGET_CANDIDATES("targetCandidates");

    private final String jsonName;

    PerfTimer(final String jsonName) {
        this.jsonName = jsonName;
    }

    /** Stable name used in machine-readable output; never derive it from {@link #name()}. */
    public String jsonName() {
        return jsonName;
    }
}
