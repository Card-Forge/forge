package forge.tournament.system;

import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import forge.LobbyPlayer;
import forge.player.GamePlayerUtil;
import forge.util.MyRandom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("serial")
public abstract class AbstractTournament implements Serializable {
    protected int activeRound;
    protected int totalRounds;
    protected int playersInPairing = 2;
    protected boolean initialized = false;
    protected boolean continualPairing = true;
    protected final List<TournamentPlayer> allPlayers = new ArrayList<TournamentPlayer>();
    protected transient final List<TournamentPlayer> remainingPlayers = new ArrayList<TournamentPlayer>();
    protected final List<TournamentPairing> completedPairings = new ArrayList<>();
    protected final List<TournamentPairing> activePairings = new ArrayList<>();

    public List<TournamentPairing> getCompletedPairings() {     return completedPairings;   }
    public List<TournamentPairing> getActivePairings() {     return activePairings;   }

    public AbstractTournament(int ttlRnds) {
        activeRound = 0;
        totalRounds = ttlRnds;
    }

    public AbstractTournament(int ttlRnds, List<TournamentPlayer> plrs) {
        activeRound = 0;
        totalRounds = ttlRnds;
        allPlayers.addAll(plrs);
        remainingPlayers.addAll(plrs);
    }

    public void initializeTournament() {
        // "Randomly" seed players to start tournament
        Collections.shuffle(remainingPlayers, MyRandom.getRandom());
        generateActivePairings();
        initialized = true;
    }

    public TournamentPairing getNextPairing() {
        if (activePairings.isEmpty()) {
            return null;
        }
        return activePairings.get(0);
    }

    public boolean isContinualPairing() {   return continualPairing;    }

    public void setContinualPairing(boolean continualPairing) { this.continualPairing = continualPairing;   }

    public boolean isInitialized() { return initialized; }

    public void setInitialized(boolean initialized) {   this.initialized = initialized; }


    public boolean isPlayerRemaining(TournamentPlayer player) {
        return remainingPlayers.contains(player);
    }
    public boolean isPlayerRemaining(int index) {
        for(TournamentPlayer player : remainingPlayers) {
            if (player.getIndex() == index) {
                return true;
            }
        }
        return false;
    }

    abstract public void generateActivePairings();
    abstract public void reportMatchCompletion(TournamentPairing pairing);
    abstract public boolean completeRound();

    public void finishMatch(TournamentPairing pairing) {
        activePairings.remove(pairing);
        completedPairings.add(pairing);
    }

    abstract public void endTournament();

    public boolean isTournamentOver() {
        return (initialized && activeRound == totalRounds && activePairings.isEmpty());
    }

    public void addTournamentPlayer(LobbyPlayer pl) {
        TournamentPlayer player = new TournamentPlayer(pl);
        allPlayers.add(player);
        remainingPlayers.add(player);
    }

    public void addTournamentPlayer(LobbyPlayer pl, int idx) {
        TournamentPlayer player = new TournamentPlayer(pl, idx);
        allPlayers.add(player);
        remainingPlayers.add(player);
    }

    public void createTournamentPlayersForDraft(String[] names, int[] icons) {
        int size = names.length;
        for(int i = 0; i < size; i++) {
            TournamentPlayer player = new TournamentPlayer(GamePlayerUtil.createAiPlayer(names[i], icons[i]), i);
            allPlayers.add(player);
            remainingPlayers.add(player);
        }
    }

    // Probably should be a interface here, not a standalone function
    public void exportToXML(HierarchicalStreamWriter writer) {
        /*
        protected int activeRound;
        protected int totalRounds;
        protected int playersInPairing = 2;
        protected boolean initialized = false;
        protected final List<TournamentPlayer> allPlayers = new ArrayList<TournamentPlayer>();
        protected final List<TournamentPairing> completedPairings = new ArrayList<>();
        protected final List<TournamentPairing> activePairings = new ArrayList<>();
        */
        writer.startNode("activeRound");
        writer.setValue(Integer.toString(activeRound));
        writer.endNode();

        writer.startNode("totalRounds");
        writer.setValue(Integer.toString(totalRounds));
        writer.endNode();

        writer.startNode("playersInPairing");
        writer.setValue(Integer.toString(playersInPairing));
        writer.endNode();

        writer.startNode("initialized");
        writer.setValue(Boolean.toString(initialized));
        writer.endNode();

        writer.startNode("allPlayers");
        for (TournamentPlayer player : allPlayers) {
            writer.startNode("tournamentPlayer");
            writer.setValue(player.toString());
            writer.endNode();
        }
        writer.endNode();

        writer.startNode("completedPairings");
        for (TournamentPairing pairing : completedPairings) {
            writer.startNode("tournamentPairing");
            pairing.exportToXML(writer);
            writer.endNode();
        }
        writer.endNode();

        writer.startNode("completedPairings");
        for (TournamentPairing pairing : activePairings) {
            writer.startNode("tournamentPairing");
            pairing.exportToXML(writer);
            writer.endNode();
        }
        writer.endNode();
    }
}
