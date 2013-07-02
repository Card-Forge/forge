package forge.game.event;

import forge.Card;
import forge.CounterType;

public class GameEventCardCounters extends GameEvent {
    public final Card target;
    public final CounterType type;
    public final int oldValue;
    public final int newValue;

    public GameEventCardCounters(Card card, CounterType counterType, int old, int newValue) {
        target = card;
        type = counterType;
        this.oldValue = old;
        this.newValue = newValue;
    }
    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}