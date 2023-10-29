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
package forge.game;

/**
 * The Enum GlobalRuleChange.
 */
public enum GlobalRuleChange {

    attackerChoosesBlockers ("The attacking player chooses how each creature blocks each combat."),
    manaBurn ("A player losing unspent mana causes that player to lose that much life."),
    noNight ("It can't become night."),
    onlyOneBlocker ("No more than one creature can block each combat."),
    onlyOneBlockerPerOpponent ("Each opponent can't block with more than one creature."),
    onlyTwoBlockers ("No more than two creatures can block each combat.");

    private final String ruleText;

    GlobalRuleChange(String text) {
        ruleText = text;
    }

    public static GlobalRuleChange fromString(String text) {
        for (final GlobalRuleChange v : GlobalRuleChange.values()) {
            if (v.ruleText.compareToIgnoreCase(text) == 0) {
                return v;
            }
        }

        throw new RuntimeException("Element " + text + " not found in GlobalRuleChange enum");
    }
}
