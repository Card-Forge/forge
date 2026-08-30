package forge.game.event;

import forge.game.player.PlayerView;

public record GameEventPlayerControl(PlayerView player, String newLobbyPlayerName, boolean newControllerIsHuman) implements GameEvent {

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
