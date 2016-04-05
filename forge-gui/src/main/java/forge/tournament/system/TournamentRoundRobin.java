package forge.tournament.system;

import java.util.List;

@SuppressWarnings("serial")
public class TournamentRoundRobin extends AbstractTournament {
    // Round Robin tournaments where you play everyone in your group/pod. Declare winner or break to top X
    public TournamentRoundRobin(int ttlRnds, List<TournamentPlayer> allPlayers) {
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
