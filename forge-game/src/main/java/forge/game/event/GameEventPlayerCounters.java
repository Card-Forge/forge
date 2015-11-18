package forge.game.event;

import forge.game.card.CounterType;
import forge.game.player.Player;

public class GameEventPlayerCounters extends GameEvent {
        public final Player receiver;
        public final CounterType type;
        public final int oldValue;
        public final int amount;

        public GameEventPlayerCounters(Player recv, CounterType t, int old, int num) {
            receiver = recv;
            type = t;
            oldValue = old;
            amount = num;
        }

        @Override
        public <T> T visit(IGameEventVisitor<T> visitor) {
            return visitor.visit(this);
        }
}
