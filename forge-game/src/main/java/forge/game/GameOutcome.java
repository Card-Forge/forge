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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import forge.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerOutcome;
import forge.game.player.PlayerStatistics;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.item.PaperCard;

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
public final class GameOutcome implements Iterable<Entry<RegisteredPlayer, PlayerStatistics>> {
    public static class AnteResult implements Serializable {
        private static final long serialVersionUID = 5087554550408543192L;

        public final List<PaperCard> lostCards = Lists.newArrayList();
        public final List<PaperCard> wonCards = Lists.newArrayList();

        public AnteResult() {
        }

        public void addWon(List<PaperCard> cards) {
            for(PaperCard c : cards) {
                if(lostCards.contains(c))
                    lostCards.remove(c);
                else
                    wonCards.add(c);
            }
        }

        public void addLost(List<PaperCard> cards) {
            for(PaperCard c : cards) {
                if(wonCards.contains(c))
                    wonCards.remove(c);
                else
                    lostCards.add(c);
            }
        }
    }

    private int lastTurnNumber = 0;
    private int lifeDelta = 0;
    private int winningTeam = -1;

    private final HashMap<RegisteredPlayer, PlayerStatistics> playerRating = new HashMap<>();
    private final HashMap<RegisteredPlayer, String> playerNames = new LinkedHashMap<>();

    private final Map<RegisteredPlayer, AnteResult> anteResult = new HashMap<>();
    private GameEndReason winCondition;

    public GameOutcome(GameEndReason reason, final Iterable<Player> players) {
        winCondition = reason;

        for (final Player p : players) {
            this.playerRating.put(p.getRegisteredPlayer(), p.getStats());
            this.playerNames.put(p.getRegisteredPlayer(), p.getName());

            if (winCondition == GameEndReason.AllOpposingTeamsLost && p.getOutcome().hasWon()) {
                // Only mark the WinningTeam when "Team mode" is on.
                winningTeam = p.getTeam();
            }
        }

        // Unable to calculate lifeDelta between a winning and losing player whe a draw is in place
        if (winCondition == GameEndReason.Draw) return;

        calculateLifeDelta(players);
    }

    private void calculateLifeDelta(Iterable<Player> players) {
        int opponentsHealth = 0;
        int winnersHealth = 0;

        for (Player p : players) {
            if (p.getOutcome().hasWon()) {
                winnersHealth += p.getLife();
            } else {
                opponentsHealth += p.getLife();
            }
        }

        lifeDelta = Math.max(0, winnersHealth - opponentsHealth);
    }

    public boolean isDraw() {
        for (PlayerStatistics stats : playerRating.values()) {
            if (stats.getOutcome().hasWon()) {
                return false;
            }
        }
        return true;
    }

    public boolean isWinner(final LobbyPlayer who) {
        for (Entry<RegisteredPlayer, PlayerStatistics> pv : playerRating.entrySet())
            if (pv.getValue().getOutcome().hasWon() && pv.getKey().getPlayer().equals(who))
                return true;
        return false;
    }

    public boolean isWinner(final RegisteredPlayer who) {
        for (Entry<RegisteredPlayer, PlayerStatistics> pv : playerRating.entrySet())
            if (pv.getValue().getOutcome().hasWon() && pv.getKey().equals(who))
                return true;
        return false;
    }

    /**
     * Gets the winner.
     *
     * @return the winner
     */
    public LobbyPlayer getWinningLobbyPlayer() {
        for (Entry<RegisteredPlayer, PlayerStatistics> ps : playerRating.entrySet()) {
            if (ps.getValue().getOutcome().hasWon()) {
                return ps.getKey().getPlayer();
            }
        }
        return null;
    }

    /**
     * Gets winning {@code Player}.
     * <p>
     * Alternative to getWinningLobbyPlayer() which does not
     * distinguish between human player names (a problem for hotseat games).
     */
    public RegisteredPlayer getWinningPlayer() {
        for (Entry<RegisteredPlayer, PlayerStatistics> pair : playerRating.entrySet()) {
            if (pair.getValue().getOutcome().hasWon()) {
                return pair.getKey();
            }
        }

        return null;
    }

    public int getWinningTeam() {
        return winningTeam;
    }

    public GameEndReason getWinCondition() {
        return this.winCondition;
    }

    public int getLastTurnNumber() {
        return this.lastTurnNumber;
    }

    public int getLifeDelta() {
        return lifeDelta;
    }

    /**
     * Gets the win spell effect.
     *
     * @return the win spell effect
     */
    public String getWinSpellEffect() {
        for (PlayerStatistics stats : playerRating.values()) {
            PlayerOutcome po = stats.getOutcome();
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
    public Iterator<Entry<RegisteredPlayer, PlayerStatistics>> iterator() {
        return playerRating.entrySet().iterator();
    }

    public void setTurnsPlayed(int turnNumber) {
        lastTurnNumber = turnNumber;
    }

    public HashMap<RegisteredPlayer, String> getPlayerNames() {
        return playerNames;
    }

    public List<String> getOutcomeStrings() {
        List<String> outcomes = Lists.newArrayList();
        for (RegisteredPlayer player : playerNames.keySet()) {
            outcomes.add(getOutcomeString(player));
        }
        return outcomes;
    }

    public String getOutcomeString(RegisteredPlayer player) {
        return playerNames.get(player) + " " + playerRating.get(player).getOutcome();
    }

    public void addAnteWon(RegisteredPlayer pl, List<PaperCard> cards) {
        if (!anteResult.containsKey(pl)) {
            anteResult.put(pl, new AnteResult());
        }
        anteResult.get(pl).addWon(cards);
    }

    public void addAnteLost(RegisteredPlayer pl, List<PaperCard> cards) {
        if (!anteResult.containsKey(pl)) {
            anteResult.put(pl, new AnteResult());
        }
        anteResult.get(pl).addLost(cards);
    }

    public AnteResult getAnteResult(RegisteredPlayer pl) {
        return anteResult.get(pl);
    }

    public AnteResult getAnteResult(PlayerView pv) {
        for (Map.Entry<RegisteredPlayer, AnteResult> e : this.anteResult.entrySet()) {
            if (pv.isLobbyPlayer(e.getKey().getPlayer())) {
                return e.getValue();
            }
        }
        return null;
    }
}
