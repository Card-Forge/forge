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

import org.apache.commons.lang.math.IntRange;

/**
 * GameType is an enum to determine the type of current game. :)
 */
public enum GameType {

    //           Limited  Main board: allowed size             SB: allowed size   Max distinct non basic cards
    Constructed ( false,  new IntRange(60, Integer.MAX_VALUE), new IntRange(15),  4),
    Sealed      ( true,   new IntRange(40, Integer.MAX_VALUE), null,              Integer.MAX_VALUE),
    Draft       ( true,   new IntRange(40, Integer.MAX_VALUE), null,              Integer.MAX_VALUE),
    Commander   ( false,  new IntRange(99 /* +cmndr aside */), new IntRange(0),   1),
    Quest       ( true,   new IntRange(40, Integer.MAX_VALUE), new IntRange(15),  4),
    Vanguard    ( false,  new IntRange(60, Integer.MAX_VALUE), new IntRange(0),   4),
    Planechase  ( false,  new IntRange(60, Integer.MAX_VALUE), new IntRange(0),   4),
    Archenemy   ( false,  new IntRange(60, Integer.MAX_VALUE), new IntRange(0),   4),
    Gauntlet    ( true,   new IntRange(40, Integer.MAX_VALUE), null,              Integer.MAX_VALUE);

    private final boolean bLimited;
    private final IntRange mainRange;
    private final IntRange sideRange; // null => no check
    private final int maxCardCopies;

    
    /**
     * Checks if is limited.
     * 
     * @return true, if is limited
     */
    public final boolean isLimited() {
        return this.bLimited;
    }


    /**
     * Instantiates a new game type.
     * 
     * @param isLimited
     *            the is limited
     */
    GameType(final boolean isLimited, IntRange main, IntRange side, int maxCopies) {
        this.bLimited = isLimited;
        mainRange = main;
        sideRange = side;
        maxCardCopies = maxCopies;
    }

    /**
     * Smart value of.
     *
     * @param value the value
     * @param defaultValue the default value
     * @return the game type
     */
    public static GameType smartValueOf(final String value, GameType defaultValue) {
        if (null == value) {
            return defaultValue;
        }

        final String valToCompate = value.trim();
        for (final GameType v : GameType.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new IllegalArgumentException("No element named " + value + " in enum GameType");
    }


    /**
     * @return the sideRange
     */
    public IntRange getSideRange() {
        return sideRange;
    }


    /**
     * @return the mainRange
     */
    public IntRange getMainRange() {
        return mainRange;
    }


    /**
     * @return the maxCardCopies
     */
    public int getMaxCardCopies() {
        return maxCardCopies;
    }
}
