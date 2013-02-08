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

    alwaysWither ("All damage is dealt as though it's source had wither."),
    manapoolsDontEmpty ("Mana pools don't empty as steps and phases end."),
    noCycling ("Players can't cycle cards."),
    noCreatureETBTriggers ("Creatures entering the battlefield don't cause abilities to trigger."),
    noLegendRule ("The legend rule doesn't apply."),
    noPrevention ("Damage can't be prevented."),
    onlyOneAttackerATurn ("No more than one creature can attack each turn."),
    onlyOneAttackerACombat ("No more than one creature can attack each combat."),
    onlyOneBlocker ("No more than one creature can block each combat."),
    toughnessAssignsDamage ("Each creature assigns combat damage equal to its toughness rather than its power."),
    blankIsChaos("Each blank roll of the planar dice is a {C} roll.");
    
    private final String ruleText;

    private GlobalRuleChange(String text) {
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
