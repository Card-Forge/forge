package forge.tournament.system;

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
        List<TournamentPlayer> pair = new ArrayList<>();
        int count = 0;
        for (TournamentPlayer tp : this.remainingPlayers) {
            pair.add(tp);
            count++;
            if (count == this.playersInPairing) {
                count = 0;
                activePairings.add(new TournamentPairing(activeRound, pair));
                pair = new ArrayList<>();
            }
        }

        if (count >= 1) {
            // Leftover players. Really shouldn't happen in a Bracket.
            TournamentPairing pairing = new TournamentPairing(activeRound, pair);
            if (count == 1) {
                pairing.setBye(true);
            }
            activePairings.add(pairing);
        }
    }

    @Override
    public void reportMatchCompletion(TournamentPairing pairing) {
        finishMatch(pairing);

        for (TournamentPlayer tp : pairing.getPairedPlayers()) {
            if (!tp.equals(pairing.getWinner())) {
                remainingPlayers.remove(tp);
                tp.setActive(false);
            }
        }

        if (activePairings.isEmpty()) {
            completeRound();
        }
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
