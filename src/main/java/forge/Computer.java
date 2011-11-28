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
package forge;

/**
 * <p>
 * Computer interface.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public interface Computer {
    /**
     * <p>
     * main1.
     * </p>
     */
    void main1();

    /**
     * <p>
     * begin_combat.
     * </p>
     */
    void beginCombat();

    /**
     * <p>
     * declare_attackers.
     * </p>
     */
    void declareAttackers();

    /**
     * <p>
     * declare_attackers_after.
     * </p>
     */
    void declareAttackersAfter(); // can play Instants and Abilities

    /**
     * <p>
     * declare_blockers.
     * </p>
     */
    void declareBlockers(); // this is called after when the Human or Computer
                            // blocks

    /**
     * <p>
     * declare_blockers_after.
     * </p>
     */
    void declareBlockersAfter(); // can play Instants and Abilities

    /**
     * <p>
     * end_of_combat.
     * </p>
     */
    void endOfCombat();

    /**
     * <p>
     * main2.
     * </p>
     */
    void main2();

    /**
     * <p>
     * end_of_turn.
     * </p>
     */
    void endOfTurn(); // end of Human's turn

    /**
     * <p>
     * stack_not_empty.
     * </p>
     */
    void stackNotEmpty();

}
