package forge.game.event;

import forge.game.card.Card;
import forge.game.player.Player;

public record GameEventCardPlotted(Card card, Player activatingPlayer) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return activatingPlayer.getName() + " has plotted " + (card != null ? card.toString() : "(unknown)");
    }
}
