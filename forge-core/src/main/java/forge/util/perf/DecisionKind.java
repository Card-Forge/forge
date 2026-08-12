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

/** The kind of AI decision a {@link DecisionRecord} describes. */
public enum DecisionKind {
    /** Choosing a spell ability to play when holding priority. */
    PRIORITY("priority"),
    /** Declaring attackers. */
    ATTACK("attack"),
    /** Declaring blockers. */
    BLOCK("block");

    private final String jsonName;

    DecisionKind(final String jsonName) {
        this.jsonName = jsonName;
    }

    /** Stable name used in machine-readable output; never derive it from {@link #name()}. */
    public String jsonName() {
        return jsonName;
    }
}
