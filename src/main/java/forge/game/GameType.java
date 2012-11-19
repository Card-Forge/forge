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
    Constructed(false, 60),
    /** The Sealed. */
    Sealed(true, 40),
    /** The Draft. */
    Draft(true, 40),
    /** The Commander. */
    Commander(false, 100, 100, true),
    /** The Quest. */
    Quest(true, 40),
    /** The Vanguard. */
    Vanguard(false, 60),
    /** The Planechase. */
    Planechase(false, 60),
    /** The Archenemy. */
    Archenemy(false, 60),    
    /** */
    Gauntlet(true, 40);

    private final boolean bLimited;
    private final int deckMinimum;
    private final Integer deckMaximum;
    private final boolean singleton;

    /**
     * Checks if is limited.
     * 
     * @return true, if is limited
     */
    public final boolean isLimited() {
        return this.bLimited;
    }
    
    public final int getDeckMinimum() {
        return this.deckMinimum;
    }
    
    public final Integer getDeckMaximum() {
        return this.deckMaximum;
    }
    
    public final boolean isSingleton() {
        return this.singleton;
    }

    GameType(final boolean isLimited, int min) {
        this(isLimited, min, null, false);
    }
    
    
    /**
     * Instantiates a new game type.
     * 
     * @param isLimited
     *            the is limited
     */
    GameType(final boolean isLimited, int min, Integer max, boolean singleton) {
        this.bLimited = isLimited;
        this.deckMinimum = min;
        this.deckMaximum = max;
        this.singleton = singleton;
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
}
