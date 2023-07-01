package forge.gamemodes.tournament.system;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class TournamentBracket extends AbstractTournament {
    // Bracket implies single elimination. For non-single elimination, use Swiss or RoundRobin
    public TournamentBracket(int ttlRnds, int pairingAmount) {
        super(ttlRnds);
        this.playersInPairing = pairingAmount;
        // Don't initialize the tournament if no players are available
    }

    public TournamentBracket(List<TournamentPlayer> allPlayers, int pairingAmount) {
        super((int)(Math.ceil(Math.log(allPlayers.size())/Math.log(2))), allPlayers);
        this.playersInPairing = pairingAmount;
    }

    public TournamentBracket(int ttlRnds, List<TournamentPlayer> allPlayers) {
        super(ttlRnds, allPlayers);
        initializeTournament();
    }

    public TournamentBracket(int ttlRnds, List<TournamentPlayer> allPlayers, int pairingAmount) {
        super(ttlRnds, allPlayers);
        this.playersInPairing = pairingAmount;
        initializeTournament();
    }

    @Override
    public void generateActivePairings() {
        activeRound++;

        int numByes = 0;
        if (activeRound == 1) {
            // Determine how many first round byes there should be.
            int fullBracketSize = (int)(Math.pow(2, Math.ceil(Math.log(this.remainingPlayers.size())/Math.log(2))));
            numByes = fullBracketSize - this.remainingPlayers.size();
        }

        // The first X remaining players will receive the required first round Byes
        // Since this is a bracket, this should "even" the bracket out.
        // Preferably our brackets will always have 2^X amount of players
        List<TournamentPlayer> pair = new ArrayList<>();
        int count = 0;
        for (TournamentPlayer tp : this.remainingPlayers) {
            pair.add(tp);
            count++;
            if (count == this.playersInPairing || numByes > 0) {
                count = 0;
                TournamentPairing pairing = new TournamentPairing(activeRound, pair);
                if (numByes > 0) {
                    numByes--;
                    pairing.setBye(true);
                }

                activePairings.add(pairing);
                pair = new ArrayList<>();
            }
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
                    remainingPlayers.remove(tp);
                    tp.setActive(false);
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

    public int getFurthestRound(int index) {
        // If won in round 3, furthest round is 4
        // If lost in any round, that's the furthest round
        for (int i = completedPairings.size(); --i >= 0;) {
            TournamentPairing pairing = completedPairings.get(i);
            for(TournamentPlayer player : pairing.getPairedPlayers()) {
                if (player.getIndex() == index) {
                    int roundAdjustment = pairing.getWinner().equals(player) ? 1 : 0;
                    return pairing.getRound() + roundAdjustment;
                }
            }
        }
        // Really shouldn't get here
        return 0;
    }

    public static TournamentBracket importFromXML() {
        TournamentBracket bracket = new TournamentBracket(3, 2);
        return bracket;
    }
}
