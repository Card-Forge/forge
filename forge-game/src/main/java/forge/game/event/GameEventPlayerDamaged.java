package forge.game.event;

import forge.game.card.Card;
import forge.game.player.Player;

public record GameEventPlayerDamaged(Player target, Card source, int amount, boolean combat, boolean infect) implements GameEvent {

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + target + " took " + amount + (infect ? " infect" : combat ? " combat" : "") + " damage from " + source;
    }
}
