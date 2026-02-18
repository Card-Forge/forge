package forge.game.event;

import forge.game.card.CardView;
import forge.game.player.PlayerView;

public record GameEventLandPlayed(PlayerView player, CardView land) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + player + " played " + land;
    }
}
