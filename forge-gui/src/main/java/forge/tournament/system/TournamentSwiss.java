package forge.tournament.system;

import java.util.List;

@SuppressWarnings("serial")
public class TournamentSwiss extends AbstractTournament {
    //http://www.wizards.com/DCI/downloads/Swiss_Pairings.pdf
    public TournamentSwiss(int ttlRnds, int pairingAmount) {
        super(ttlRnds);
        this.playersInPairing = pairingAmount;
        // Don't initialize the tournament if no players are available
    }

    public TournamentSwiss(int ttlRnds, List<TournamentPlayer> allPlayers) {
        super(ttlRnds, allPlayers);
        initializeTournament();
    }

    @Override
    public void generateActivePairings() {

    }

    @Override
    public boolean reportMatchCompletion(TournamentPairing pairing) {
        return false;
    }

    @Override
    public boolean completeRound() {
        return false;
    }

    @Override
    public void endTournament() {

    }
}
