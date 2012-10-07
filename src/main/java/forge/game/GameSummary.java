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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * GameInfo class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */

// This class might be divided in two parts: the very summary (immutable with
// only getters) and
// GameObserver class - who should be notified of any considerable ingame event
public final class GameSummary {

    /** The player winner. */
    private String playerWinner = "Nobody";

    /** The player got first turn. */
    // private String playerGotFirstTurn = "Nobody";

    /** The last turn number. */
    private int lastTurnNumber = 0;

    /** The win condition. */
    private GameEndReason winCondition;

    /** The spell effect win. */
    private String spellEffectWin;

    /** The player rating. */
    private final Map<String, GamePlayerRating> playerRating = new HashMap<String, GamePlayerRating>();

    /**
     * Instantiates a new game summary.
     * 
     * @param names
     *            the names
     */
    public GameSummary(final String... names) {
        this(Arrays.asList(names));
    }

    public GameSummary(final Iterable<String> list) {
        for (final String n : list) {
            this.playerRating.put(n, new GamePlayerRating());
        }
    }    
    
    /**
     * End.
     * 
     * @param condition
     *            the condition
     * @param winner
     *            the winner
     * @param spellEffect
     *            the spell effect
     */
    public void end(final GameEndReason condition, final String winner, final String spellEffect) {
        this.winCondition = condition;
        this.playerWinner = winner;
        this.spellEffectWin = spellEffect;
    }

    /**
     * Checks if is draw.
     * 
     * @return true, if is draw
     */
    public boolean isDraw() {
        return null == this.playerWinner;
    }

    /**
     * Checks if is winner.
     * 
     * @param name
     *            the name
     * @return true, if is winner
     */
    public boolean isWinner(final String name) {
        return (name != null) && name.equals(this.playerWinner);
    }

    /**
     * Gets the winner.
     * 
     * @return the winner
     */
    public String getWinner() {
        return this.playerWinner;
    }

    /**
     * Gets the win condition.
     * 
     * @return the win condition
     */
    public GameEndReason getWinCondition() {
        return this.winCondition;
    }

    /**
     * Gets the player rating.
     * 
     * @param name
     *            the name
     * @return the player rating
     */
    public GamePlayerRating getPlayerRating(final String name) {
        return this.playerRating.get(name);
    }

    /**
     * Gets the turn game ended.
     * 
     * @return the turn game ended
     */
    public int getLastTurnNumber() {
        return this.lastTurnNumber;
    }

    /**
     * Sets the player who got first turn.
     * 
     */
    /*
     * public void setPlayerWhoGotFirstTurn(final String playerName) {
     * this.playerGotFirstTurn = playerName; }
     */

    /**
     * Notify next turn.
     */
    public void notifyNextTurn() {
        this.lastTurnNumber++;
    }

    /**
     * Gets the win spell effect.
     * 
     * @return the win spell effect
     */
    public String getWinSpellEffect() {
        return this.spellEffectWin;
    }

}
