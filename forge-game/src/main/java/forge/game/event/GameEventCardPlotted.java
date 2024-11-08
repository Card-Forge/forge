package forge.game.event;

import forge.game.card.Card;
import forge.game.player.Player;

public class GameEventCardPlotted extends GameEvent {

    public final Card card;

    public final Player activatingPlayer;

    public GameEventCardPlotted(Card card, Player player) {
        this.card = card;
        activatingPlayer = player;
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
        return activatingPlayer.getName() + " has plotted " + (card != null ? card.toString() : "(unknown)");
    }
}
