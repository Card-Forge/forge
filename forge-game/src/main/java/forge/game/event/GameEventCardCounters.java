package forge.game.event;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CounterType;

public record GameEventCardCounters(CardView card, CounterType type, int oldValue, int newValue) implements GameEvent {
    public GameEventCardCounters(Card card, CounterType type, int oldValue, int newValue) {
        this(CardView.get(card), type, oldValue, newValue);
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
        return "" + card + " " + type + " counters: " + oldValue + " -> " + newValue;
    }
}