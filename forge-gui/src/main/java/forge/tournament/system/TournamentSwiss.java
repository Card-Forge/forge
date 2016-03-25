package forge.tournament.system;

import java.util.List;

public class TournamentSwiss extends AbstractTournament {

    public TournamentSwiss(int ttlRnds, List<TournamentPlayer> allPlayers) {
        super(ttlRnds, allPlayers);
        initializeTournament();
    }

    @Override
    public void generateActivePairings() {

    }

    @Override
    public void reportMatchCompletion(TournamentPairing pairing) {

    }

    @Override
    public boolean completeRound() {
        return false;
    }

    @Override
    public void endTournament() {

    }
}
