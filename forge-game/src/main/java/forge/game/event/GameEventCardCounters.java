package forge.game.event;

import forge.game.card.Card;
import forge.game.card.CounterType;

public record GameEventCardCounters(Card card, CounterType type, int oldValue, int newValue) implements GameEvent {
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + card + " " + type + " counters: " + oldValue + " -> " + newValue;
    }
}