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
 * GameType is an enum to determine the type of current game. :)
 */
public enum GameType {

    /** The Constructed. */
    Constructed(false),
    /** The Sealed. */
    Sealed(true),
    /** The Draft. */
    Draft(true),
    /** The Commander. */
    Commander(false),
    /** The Quest. */
    Quest(true);

    private final boolean bLimited;

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
    GameType(final boolean isLimited) {
        this.bLimited = isLimited;
    }

    /**
     * Smart value of.
     * 
     * @param value
     *            the value
     * @return the game type
     */
    public static GameType smartValueOf(final String value) {
        final String valToCompate = value.trim();
        for (final GameType v : GameType.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }

        throw new IllegalArgumentException("No element named " + value + " in enum GameType");
    }
}
