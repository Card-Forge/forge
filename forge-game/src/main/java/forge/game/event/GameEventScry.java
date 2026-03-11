package forge.game.event;

import forge.game.player.PlayerView;

public record GameEventScry(PlayerView player, int toTop, int toBottom) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + player + " scried " + toTop + " to top, " + toBottom + " to bottom";
    }
}
