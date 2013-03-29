package forge.game.event;

import forge.game.player.LobbyPlayer;

public class DuelOutcomeEvent extends Event {
    public final LobbyPlayer winner;

    public DuelOutcomeEvent(LobbyPlayer winner) {
        this.winner = winner;
    }
}
