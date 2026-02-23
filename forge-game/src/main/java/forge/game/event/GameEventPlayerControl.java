package forge.game.event;

import forge.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.PlayerView;

public record GameEventPlayerControl(PlayerView player, String oldLobbyPlayerName, String newLobbyPlayerName, boolean newControllerIsHuman) implements GameEvent {

    public GameEventPlayerControl(Player player, LobbyPlayer oldLobbyPlayer,
            PlayerController oldController, LobbyPlayer newLobbyPlayer,
            PlayerController newController) {
        this(PlayerView.get(player),
             oldLobbyPlayer != null ? oldLobbyPlayer.getName() : null,
             newLobbyPlayer != null ? newLobbyPlayer.getName() : null,
             newController != null && !newController.isAI());
    }

    @Override
    public <T> T visit(final IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + player + " controlled by " + newLobbyPlayerName;
    }
}
