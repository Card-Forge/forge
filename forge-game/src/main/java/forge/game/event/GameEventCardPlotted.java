package forge.game.event;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.Player;
import forge.game.player.PlayerView;

public record GameEventCardPlotted(CardView card, PlayerView activatingPlayer) implements GameEvent {

    public GameEventCardPlotted(Card card, Player activatingPlayer) {
        this(CardView.get(card), PlayerView.get(activatingPlayer));
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
        return activatingPlayer.toString() + " has plotted " + (card != null ? card.toString() : "(unknown)");
    }
}
