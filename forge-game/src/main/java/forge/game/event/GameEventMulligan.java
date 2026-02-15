package forge.game.event;

import forge.game.player.Player;
import forge.game.player.PlayerView;

public record GameEventMulligan(PlayerView player) implements GameEvent {

    public GameEventMulligan(Player player) {
        this(PlayerView.get(player));
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
        return "" + player + " mulligans";
    }
}
