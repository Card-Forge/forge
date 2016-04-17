package forge.tournament.system;

import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import forge.LobbyPlayer;

public class TournamentPlayer {
    private LobbyPlayer player;
    private int wins = 0,
            losses = 0,
            ties = 0;
    private boolean active = true;

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

    public void addLoss() { losses++; }
    public void addWin() { wins++; }
    public void addTie() { ties++; }

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getIndex() {     return index;   }

    public void setIndex(int index) {   this.index = index; }

    public void exportToXML(HierarchicalStreamWriter writer) {

    }
}
