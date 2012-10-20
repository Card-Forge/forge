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
 * The Enum GameEndReason.
 */
public enum GameEndReason {
    /** The All opponents lost. */
    AllOpponentsLost,
    // Noone won
    /** The Draw. */
    Draw, // Having little idea how they can reach a draw, so I didn't enumerate
          // possible reasons here
    // Special conditions, they force one player to win and thus end the game

    /** The Wins game spell effect. */
    WinsGameSpellEffect, // ones that could be both hardcoded (felidar) and
                        // scripted ( such as Mayael's Aria )
    
    /** Used to end multiplayer games where the all humans have lost or conceded while AIs cannot end match by themselves*/
    AllHumansLost,
}
