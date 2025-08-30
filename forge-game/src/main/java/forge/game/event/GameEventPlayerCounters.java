package forge.game.event;

import forge.game.card.CounterType;
import forge.game.player.Player;

public record GameEventPlayerCounters(Player receiver, CounterType type, int oldValue, int amount) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + receiver + " got " + oldValue + " plus " + amount + " " + type;
    }
}
