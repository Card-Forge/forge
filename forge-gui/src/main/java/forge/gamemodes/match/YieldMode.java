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
package forge.gamemodes.match;

/**
 * Yield modes for extended auto-pass functionality.
 * Used when experimental yield options are enabled.
 */
public enum YieldMode {
    NONE("No auto-yield"),
    UNTIL_NEXT_PHASE("Next Phase"),
    UNTIL_STACK_CLEARS("Clear Stack"),
    UNTIL_END_OF_TURN("Next Turn"),
    UNTIL_YOUR_NEXT_TURN("Your Turn"),
    UNTIL_BEFORE_COMBAT("Combat"),
    UNTIL_END_STEP("End Step"),
    UNTIL_END_STEP_BEFORE_YOUR_TURN("Before Your Turn");

    private final String description;

    YieldMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
