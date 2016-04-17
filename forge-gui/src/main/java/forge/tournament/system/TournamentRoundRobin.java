package forge.tournament.system;

import com.google.common.collect.Lists;
import forge.player.GamePlayerUtil;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class TournamentRoundRobin extends AbstractTournament {
    // Round Robin tournaments where you play everyone in your group/pod. Declare winner or break to top X
    public TournamentRoundRobin(int ttlRnds, int pairingAmount) {
        super(ttlRnds);
        this.playersInPairing = pairingAmount;
        // Don't initialize the tournament if no players are available
    }

    public TournamentRoundRobin(int ttlRnds, List<TournamentPlayer> allPlayers) {
        super(ttlRnds, allPlayers);
        initializeTournament();
    }

    public TournamentRoundRobin(List<TournamentPlayer> allPlayers, int pairingAmount) {
        super(allPlayers.size() % 2 == 0 ? allPlayers.size() - 1 : allPlayers.size(), allPlayers);
        this.playersInPairing = pairingAmount;
    }

    @Override
    public void generateActivePairings() {
        int numPlayers = this.remainingPlayers.size();
        List<TournamentPlayer> pair = new ArrayList<>();

        List<TournamentPlayer> roundPairings = Lists.newArrayList(this.remainingPlayers);
        if (numPlayers % 2 == 1) {
            roundPairings.add(new TournamentPlayer(GamePlayerUtil.createAiPlayer("BYE", 0)));
            numPlayers++;
        }

        TournamentPlayer pivot = roundPairings.get(0);
        roundPairings.remove(0);

        for(int i = 0; i < activeRound; i++) {
            // Rotate X amount of players, where X is the current round-1
            TournamentPlayer rotate = roundPairings.get(0);
            roundPairings.remove(0);
            roundPairings.add(rotate);
        }
        roundPairings.add(0, pivot);

        activeRound++;

        for(int i = 0; i < numPlayers/2; i++) {
            boolean bye = false;
            if (roundPairings.get(i).getPlayer().getName().equals("BYE")) {
                bye = true;
            } else {
                pair.add(roundPairings.get(i));
            }

            if (roundPairings.get(numPlayers-i-1).getPlayer().getName().equals("BYE")) {
                bye = true;
            } else {
                pair.add(roundPairings.get(numPlayers-i-1));
            }
            TournamentPairing pairing = new TournamentPairing(activeRound, pair);
            pairing.setBye(bye);
            activePairings.add(pairing);
            pair = new ArrayList<>();
        }
    }

    @Override
    public boolean reportMatchCompletion(TournamentPairing pairing) {
        // Returns whether there are more matches left in this round
        finishMatch(pairing);

        if (!pairing.isBye()) {
            for (TournamentPlayer tp : pairing.getPairedPlayers()) {
                if (!tp.equals(pairing.getWinner())) {
                    tp.addLoss();
                } else {
                    tp.addWin();
                }
            }
        }

        if (activePairings.isEmpty()) {
            completeRound();
            return false;
        }
        return true;
    }

    @Override
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

    @Override
    public void endTournament() {
        this.activePairings.clear();
    }
}
