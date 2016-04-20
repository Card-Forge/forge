package forge.tournament.system;

import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import forge.LobbyPlayer;

import java.util.ArrayList;
import java.util.List;

public class TournamentPlayer {
    private LobbyPlayer player;
    // Ties don't really happen with AI simulations, because there's no time limit
    private int wins = 0,
            losses = 0,
            ties = 0,
            byes = 0;
            //gameWins = 0,
            //gameLosses = 0;
    private boolean active = true;
    // A list of indexes of previous opponents
    private List<Integer> previousOpponents = new ArrayList<>();

    private int index = -1;

    public TournamentPlayer(LobbyPlayer plyr) {
        player = plyr;
    }

    public TournamentPlayer(LobbyPlayer plyr, int idx) {
        player = plyr;
        index = idx;
    }

    public LobbyPlayer getPlayer() {
        return player;
    }

    public void setPlayer(LobbyPlayer player) {
        this.player = player;
    }

    /*
    public void addMatchResult(boolean wonMatch, int wonGames, int lostGames, boolean isBye) {
        if (wonMatch) {
            wins++;
        } else {
            losses++;
        }
        if (isBye) {
            byes++;
        }
        gameWins += wonGames;
        gameLosses += lostGames;
    }
    */

    public void addOpponentIndex(int oppIndex) {
        previousOpponents.add(oppIndex);
    }

    public void addLoss() { losses++; }
    public void addWin() { wins++; }
    public void addTie() { ties++; }
    public void addBye() { byes++; }

    public int getScore() { return (wins+byes)*3+ties; }

    public int getSwissScore() { return (wins+byes)*30+ties*10+byes; }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getTies() {
        return ties;
    }

    public void setTies(int ties) {
        this.ties = ties;
    }

    public int getByes() { return byes; }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getIndex() {     return index;   }

    public void setIndex(int index) {   this.index = index; }

    public List<Integer> getPreviousOpponents() { return previousOpponents; }

    public void exportToXML(HierarchicalStreamWriter writer) {

    }
}
