package forge.gamemodes.tournament.system;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import forge.LobbyPlayer;
import forge.game.GameOutcome;

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

    public int getRound() { return round; }

    public void setRound(int round) { this.round = round; }

    public boolean isBye() { return bye; }

    public void setBye(boolean bye) { this.bye = bye; }

    public List<TournamentPlayer> getPairedPlayers() { return pairedPlayers; }

    public List<GameOutcome> getOutcomes() { return outcomes; }

    public TournamentPlayer getWinner() { return winner; }

    public void setWinner(TournamentPlayer winner) { this.winner = winner; }

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

    public String outputHeader() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(TournamentPlayer tp : getPairedPlayers()) {
            // Post Record
            if (!first) {
                sb.append("vs ");
            }
            first = false;
            sb.append(tp.getNameAndScore()).append(" ");
        }
        if (isBye()) {
            sb.append("BYE");
        }
        return sb.toString();
    }

    public void exportToXML(HierarchicalStreamWriter writer) {

    }
}
