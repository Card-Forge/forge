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
package forge.game.phase;

import forge.game.GameState;

/**
 * <p>
 * Handles "until your next turn" effects and Cleanup hardcoded triggers.
 * </p>
 * 
 * @author Forge
 */
public class Cleanup extends Phase {

    
    /** Constant <code>serialVersionUID=-6993476643509826990L</code>. */
    private static final long serialVersionUID = -6993476643509826990L;

    public Cleanup(final GameState game) { super(game); }
    
} // end class Cleanup
