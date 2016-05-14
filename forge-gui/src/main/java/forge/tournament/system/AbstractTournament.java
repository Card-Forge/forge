package forge.tournament.system;

import com.google.common.collect.Lists;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import forge.LobbyPlayer;
import forge.deck.DeckGroup;
import forge.game.player.RegisteredPlayer;
import forge.player.GamePlayerUtil;
import forge.util.MyRandom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SuppressWarnings("serial")
public abstract class AbstractTournament implements Serializable {
    protected int activeRound;
    protected int totalRounds;
    protected int playersInPairing = 2;
    protected boolean initialized = false;
    protected boolean continualPairing = true;
    protected final List<TournamentPlayer> allPlayers = new ArrayList<>();
    protected transient final List<TournamentPlayer> remainingPlayers = new ArrayList<>();
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

    public int getActiveRound() { return activeRound; }
    public int getTotalRounds() { return totalRounds; }

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
    abstract public boolean reportMatchCompletion(TournamentPairing pairing);

    public boolean completeRound() {
        if (activeRound < totalRounds) {
            if (continualPairing) {
                generateActivePairings();
            }
            return true;
        } else {
            endTournament();
            return false;
        }
    }

    public void finishMatch(TournamentPairing pairing) {
        activePairings.remove(pairing);
        completedPairings.add(pairing);
    }

    abstract public void endTournament();

    public boolean isTournamentOver() {
        return (initialized && activeRound == totalRounds && activePairings.isEmpty());
    }

    public void sortAllPlayers(String sortType) {
        if (sortType.equals("score")) {
            Collections.sort(allPlayers, new Comparator<TournamentPlayer>() {
                @Override
                public int compare(TournamentPlayer o1, TournamentPlayer o2) {
                    return o2.getScore() - o1.getScore();
                }
            });
        } else if (sortType.equals("index")) {
            Collections.sort(allPlayers, new Comparator<TournamentPlayer>() {
                @Override
                public int compare(TournamentPlayer o1, TournamentPlayer o2) {
                    return o2.getIndex() - o1.getIndex();
                }
            });
        } else if (sortType.equals("swiss")) {
            Collections.sort(allPlayers, new Comparator<TournamentPlayer>() {
                @Override
                public int compare(TournamentPlayer o1, TournamentPlayer o2) {
                    return o2.getSwissScore() - o1.getSwissScore();
                }
            });
        }
    }

    public void outputTournamentResults() {
        sortAllPlayers("score");
        //System.out.println("Name\t\tScore\tW(By)\tL\tT");
        for(TournamentPlayer tp : allPlayers) {
            System.out.println(String.format("%s\t\t%d Wins(%d Byes)-%d Losses-%d Ties\t=>\t%d Points", tp.getPlayer().getName(),
                    tp.getWins(), tp.getByes(), tp.getLosses(), tp.getTies(), tp.getScore()));
        }
    }

    public static List<RegisteredPlayer> registerTournamentPlayers(TournamentPairing pairing, DeckGroup decks) {
        List<RegisteredPlayer> registered = Lists.newArrayList();
        for (TournamentPlayer pl : pairing.getPairedPlayers()) {
            if (pl.getIndex() == -1) {
                registered.add(new RegisteredPlayer(decks.getHumanDeck()).setPlayer(pl.getPlayer()));
            } else {
                registered.add(new RegisteredPlayer(decks.getAiDecks().get(pl.getIndex())).setPlayer(pl.getPlayer()));
            }
        }
        return registered;
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
