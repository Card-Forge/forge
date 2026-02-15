package forge.game.event;

import forge.game.player.Player;
import forge.game.player.PlayerView;

public record GameEventCardForetold(PlayerView activatingPlayer) implements GameEvent {

    public GameEventCardForetold(Player activatingPlayer) {
        this(PlayerView.get(activatingPlayer));
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return activatingPlayer.toString() + " has foretold.";
    }
}
