package forge.tournament.system;

import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import forge.LobbyPlayer;
import forge.game.GameOutcome;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TournamentPairing {
    private int round;
    private boolean bye = false;
    private final List<TournamentPlayer> pairedPlayers = new ArrayList<>();
    private final List<GameOutcome> outcomes = new ArrayList<>();
    private TournamentPlayer winner;

    public TournamentPairing(int rnd, List<TournamentPlayer> plyrs) {
        pairedPlayers.addAll(plyrs);
        round = rnd;
        winner = null;
    }

    public int getRound() {     return round; }

    public void setRound(int round) {   this.round = round; }

    public boolean isBye() {    return bye; }

    public void setBye(boolean bye) {   this.bye = bye; }

    public List<TournamentPlayer> getPairedPlayers() {    return pairedPlayers; }

    public List<GameOutcome> getOutcomes() {    return outcomes; }

    public TournamentPlayer getWinner() {   return winner;  }

    public void setWinner(TournamentPlayer winner) {    this.winner = winner; }

    public void setWinnerByIndex(int index) {
        for(TournamentPlayer pl : pairedPlayers) {
            if (pl.getIndex() == index) {
                this.winner = pl;
                return;
            }
        }
    }

    public boolean hasPlayer(LobbyPlayer player) {
        for(TournamentPlayer pl : this.pairedPlayers) {
            if (pl.getPlayer().equals(player)) {
                return true;
            }
        }
        return false;
    }

    public void exportToXML(HierarchicalStreamWriter writer) {

    }
}
