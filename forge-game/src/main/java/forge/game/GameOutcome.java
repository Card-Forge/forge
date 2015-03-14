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

import forge.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerOutcome;
import forge.game.player.PlayerStatistics;
import forge.item.PaperCard;

import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * GameInfo class.
 * </p>
 * 
 * @author Forge
 * @version $Id: GameOutcome.java 17608 2012-10-20 22:27:27Z Max mtg $
 */

// This class might be divided in two parts: the very summary (immutable with
// only getters) and
// GameObserver class - who should be notified of any considerable ingame event
public final class GameOutcome implements Iterable<Pair<LobbyPlayer, PlayerStatistics>> {
    public static class AnteResult implements Serializable {
        private static final long serialVersionUID = 5087554550408543192L;

        public final List<PaperCard> lostCards;
        public final List<PaperCard> wonCards;
        
        private AnteResult(List<PaperCard> cards, boolean won) {
            // Need empty lists for other results for addition of change ownership cards
            if (won) {
                this.wonCards = cards;
                this.lostCards = new ArrayList<>();
            } else {
                this.lostCards = cards;
                this.wonCards = new ArrayList<>();
            }
        }

        public void addWon(List<PaperCard> cards) {
            this.wonCards.addAll(cards);
        }

        public void addLost(List<PaperCard> cards) {
            this.lostCards.addAll(cards);
        }

        public static AnteResult won(List<PaperCard> cards) { return new AnteResult(cards, true); }
        public static AnteResult lost(List<PaperCard> cards) { return new AnteResult(cards, false); }
    }

    private int lastTurnNumber = 0;
    private int lifeDelta = 0;
    private final List<Pair<LobbyPlayer, PlayerStatistics>> playerRating = new ArrayList<Pair<LobbyPlayer, PlayerStatistics>>(2);
    private final Iterable<Player> players;
    public final Map<Player, AnteResult> anteResult = new TreeMap<>();
    private GameEndReason winCondition;

    public GameOutcome(GameEndReason reason, final Iterable<Player> list) {
        winCondition = reason;
        players = list;
        for (final Player n : list) {
            this.playerRating.add(Pair.of(n.getLobbyPlayer(), n.getStats()));
        }
        calculateLifeDelta();
    }

    private void calculateLifeDelta() {
        int opponentsHealth = 0;
        int winnersHealth = 0;
        
        for (Player p : players) {
            if (p == this.getWinningPlayer()) {
                winnersHealth = p.getLife();
            }
            else {
                opponentsHealth += p.getLife();
            }
        }
        
        lifeDelta = Math.max(0, winnersHealth -= opponentsHealth);
    }

    public boolean isDraw() {
        for (Pair<LobbyPlayer, PlayerStatistics> pv : playerRating) {
            if (pv.getValue().getOutcome().hasWon()) {
                return false;
            }
        }
        return true;
    }

    public boolean isWinner(final LobbyPlayer who) {
        for (Pair<LobbyPlayer, PlayerStatistics> pv : playerRating)
            if (pv.getValue().getOutcome().hasWon() && pv.getKey() == who )
                return true;
        return false;
    }

    /**
     * Gets the winner.
     * 
     * @return the winner
     */
    public LobbyPlayer getWinningLobbyPlayer() {
        for (Entry<LobbyPlayer, PlayerStatistics> ps : playerRating) {
            if (ps.getValue().getOutcome().hasWon()) {
                return ps.getKey();
            }
        }
        return null;
    }

    /**
     * Gets winning {@code Player}.
     * <p>
     * Alternative to {@link getWinningLobbyPlayer()} which does not
     * distinguish between human player names (a problem for hotseat games).
     */
    public Player getWinningPlayer() {
        for (Player p: players) {
            if (p.getOutcome().hasWon()) {
                return p;
            }
        }
        return null;
    }

    public int getWinningTeam() {
        for (Player p: players) {
            if (p.getOutcome().hasWon() && winCondition == GameEndReason.AllOpposingTeamsLost) {
                return p.getTeam();
            }
        }
        return -1;
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
     * Gets the turn game ended.
     * 
     * @return the turn game ended
     */
    public int getLastTurnNumber() {
        return this.lastTurnNumber;
    }
    
    /**
     * 
     * @return The difference in life totals between the winner and losers. 
     */
    public int getLifeDelta() {
        return lifeDelta;
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
        for (Pair<LobbyPlayer, PlayerStatistics> pv : playerRating) {
            PlayerOutcome po = pv.getValue().getOutcome();
            if (po.hasWon()) {
                return po.altWinSourceName;
            }
        }
        return null;
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Pair<LobbyPlayer, PlayerStatistics>> iterator() {
        return playerRating.iterator();
    }

    /**
     * TODO: Write javadoc for this method.
     * @param turnNumber
     */
    public void setTurnsPlayed(int turnNumber) {
        lastTurnNumber = turnNumber;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public int getNumPlayers() {
        return playerRating.size();
    }

    public List<Player> getPlayers() {
        return (List<Player>)players;
    }
}
