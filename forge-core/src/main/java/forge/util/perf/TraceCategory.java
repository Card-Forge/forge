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
 * The kinds of entry a decision trace can carry.
 *
 * <p>A decision trace is the parity artefact: two builds are considered behaviour-equivalent when
 * their traces are byte-for-byte identical. Entries must therefore be built from stable descriptors
 * (game-scoped ids, names, ordinals) and never from object hash codes or iteration-order-dependent
 * text.</p>
 */
public enum TraceCategory {
    /** A generated legal candidate, in generation order. */
    CANDIDATE("candidate"),
    /** An alternate/optional cost variant of a candidate. */
    ALT_COST("altCost"),
    /** The heuristic verdict for one candidate. */
    EVALUATION("evaluation"),
    /** One simulated branch and its score. */
    SIMULATION("simulation"),
    /** The ability the AI finally selected. */
    CHOSEN("chosen"),
    /** A declared attacker and its defender. */
    ATTACKER("attacker"),
    /** A declared blocker, in block order. */
    BLOCKER("blocker"),
    /** A trigger considered, applicable or resolved. */
    TRIGGER("trigger"),
    /** A replacement effect considered, applicable or selected. */
    REPLACEMENT("replacement"),
    /** A random draw: category, ordinal and value. */
    RANDOM("random"),
    /** A canonical state digest taken at a checkpoint. */
    STATE_HASH("stateHash"),
    /** A game outcome. */
    OUTCOME("outcome");

    private final String jsonName;

    TraceCategory(final String jsonName) {
        this.jsonName = jsonName;
    }

    /** Stable name used in machine-readable output; never derive it from {@link #name()}. */
    public String jsonName() {
        return jsonName;
    }
}
