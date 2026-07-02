package forge.game.event;

import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.PlayerView;

public record GameEventPlayerCounters(PlayerView receiver, CounterType type, int oldValue, int amount) implements GameEvent {

    public GameEventPlayerCounters(Player receiver, CounterType type, int oldValue, int amount) {
        this(PlayerView.get(receiver), type, oldValue, amount);
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
        return "" + receiver + " got " + oldValue + " plus " + amount + " " + type;
    }
}
