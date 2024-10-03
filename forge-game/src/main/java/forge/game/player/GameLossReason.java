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
package forge.game.player;

/**
 * The Enum GameLossReason.
 */
public enum GameLossReason {
    /** The Conceded. */
    Conceded, // rule 104.3a
    /** The Life reached zero. */
    LifeReachedZero, // rule 104.3b
    /** The Milled. */
    Milled, // 104.3c
    /** The Poisoned. */
    Poisoned, // 104.3d

    // 104.3e and others
    /** The Spell effect. */
    SpellEffect,
    
    CommanderDamage,

    OpponentWon,

    IntentionalDraw // Not a real "game loss" as such, but a reason not to continue playing.
    ;

    /**
     * Parses a string into an enum member.
     * @param string to parse
     * @return enum equivalent
     */
    public static GameLossReason smartValueOf(String value) {
        final String valToCompate = value.trim();
        for (final GameLossReason v : GameLossReason.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new RuntimeException("Element " + value + " not found in GameLossReason enum");
    }
}
