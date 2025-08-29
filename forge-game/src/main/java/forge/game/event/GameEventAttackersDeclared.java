package forge.game.event;

import com.google.common.collect.Multimap;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;

public record GameEventAttackersDeclared(Player player, Multimap<GameEntity, Card> attackersMap) implements GameEvent {

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
        return "" + player + " declared attackers: " + attackersMap;
    }
}
