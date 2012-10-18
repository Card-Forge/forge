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
 * The Class GamePlayerRating.
 * 
 * @author Max
 */
public class PlayerStatistics {

    /** The opening hand size. */
    private int openingHandSize = 7;

    /** The times mulliganed. */
    private int timesMulliganed = 0;

    private int turnsPlayed = 0;

    
    private PlayerOutcome outcome;

    /**
     * Gets the opening hand size.
     * 
     * @return the opening hand size
     */
    public final int getOpeningHandSize() {
        return this.openingHandSize;
    }

    /**
     * Notify has mulliganed.
     */
    public final void notifyHasMulliganed() {
        this.timesMulliganed++;
    }

    /**
     * Gets the mulligan count.
     * 
     * @return the mulligan count
     */
    public final int getMulliganCount() {
        return this.timesMulliganed;
    }

    /**
     * Notify opening hand size.
     * 
     * @param newHand
     *            the new hand
     */
    public final void notifyOpeningHandSize(final int newHand) {
        this.openingHandSize = newHand;
    }

    public int getTurnsPlayed() {
        return turnsPlayed;
    }

    public void nextTurn() {
        this.turnsPlayed++; 
    }

    public PlayerOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(PlayerOutcome gameOutcome) {
        this.outcome = gameOutcome; 
    }

}
