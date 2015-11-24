package forge.deck.io;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.LobbyPlayer;
import forge.ai.LobbyPlayerAi;
import forge.game.GameOutcome;
import forge.game.Match;
import forge.game.player.RegisteredPlayer;

@SuppressWarnings("unused")
public class DeckRecords {
    private static final Map<String, DeckRecords> recordLookup = new HashMap<String, DeckRecords>();

    public static void recordMatchOutcome(Match match) {
        for (RegisteredPlayer p1 : match.getPlayers()) {
            for (RegisteredPlayer p2 : match.getPlayers()) {
                if (p1 != p2) {
                }
            }
        }
    }

    private final List<DeckMatch> matches = new ArrayList<DeckMatch>();

    public void addMatch(Match match, LobbyPlayer player) {
        matches.add(new DeckMatch(match, player));
    }

    private static class DeckMatch {
        private final long timestamp;
        private final boolean isAi;
        private final int results;

        private DeckMatch(Match match, LobbyPlayer player) {
            timestamp = new Date().getTime();
            isAi = (player instanceof LobbyPlayerAi);

            int results0 = 0;
            int bit = 1;
            for (GameOutcome outcome : match.getOutcomes()) {
                if (outcome.isWinner(player)) {
                    results0 += bit;
                }
                else if (outcome.isDraw()) {
                    
                }
                bit *= 2;
            }
            results = results0;
        }
    }
}
