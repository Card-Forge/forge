package forge.game.event;

import forge.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.PlayerController;

public record GameEventPlayerControl(Player player, LobbyPlayer oldLobbyPlayer, PlayerController oldController, LobbyPlayer newLobbyPlayer, PlayerController newController) implements GameEvent {

    @Override
    public <T> T visit(final IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + player + " controlled by " + player.getControllingPlayer();
    }
}