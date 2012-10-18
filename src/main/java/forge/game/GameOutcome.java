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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerStatistics;

/**
 * <p>
 * GameInfo class.
 * </p>
 * 
 * @author Forge
 * @version $Id: GameOutcome.java 17559 2012-10-18 07:59:42Z Max mtg $
 */

// This class might be divided in two parts: the very summary (immutable with
// only getters) and
// GameObserver class - who should be notified of any considerable ingame event
public final class GameOutcome implements Iterable<Entry<LobbyPlayer, PlayerStatistics>>  {


    /** The player got first turn. */
    // private String playerGotFirstTurn = "Nobody";

    /** The last turn number. */
    private int lastTurnNumber = 0;

    /** The player rating. */
    private final Map<LobbyPlayer, PlayerStatistics> playerRating = new HashMap<LobbyPlayer, PlayerStatistics>(4);

    private GameEndReason winCondition;

    /**
     * Instantiates a new game summary.
     * 
     * @param names
     *            the names
     */
    public GameOutcome(GameEndReason reason, final Player... names) {
        this(reason, Arrays.asList(names));
    }

    public GameOutcome(GameEndReason reason, final Iterable<Player> list) {
        winCondition = reason;
        for (final Player n : list) {
            this.playerRating.put(n.getLobbyPlayer(), n.getStats());
        }
    }    
    

    /**
     * Checks if is draw.
     * 
     * @return true, if is draw
     */
    public boolean isDraw() {
        for( PlayerStatistics pv : playerRating.values())
        {
            if ( pv.getOutcome().hasWon() )
            return false;
        }
        return true;
    }

    /**
     * Checks if is winner.
     * 
     * @param name
     *            the name
     * @return true, if is winner
     */
    public boolean isWinner(final LobbyPlayer who) {
        PlayerStatistics stats =  playerRating.get(who);
        return stats.getOutcome().hasWon();
    }

    /**
     * Gets the winner.
     * 
     * @return the winner
     */
    public LobbyPlayer getWinner() {
        for( Entry<LobbyPlayer, PlayerStatistics> ps : playerRating.entrySet())
        {
            if ( ps.getValue().getOutcome().hasWon() )
            return ps.getKey();
        }
        return null;
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
    public PlayerStatistics getStatistics(final LobbyPlayer name) {
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
     * Gets the win spell effect.
     * 
     * @return the win spell effect
     */
    public String getWinSpellEffect() {
        for( PlayerStatistics pv : playerRating.values())
        {
            if ( pv.getOutcome().hasWon() )
            return pv.getOutcome().altWinSourceName;
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Entry<LobbyPlayer, PlayerStatistics>> iterator() {
        return playerRating.entrySet().iterator();
    }

}
