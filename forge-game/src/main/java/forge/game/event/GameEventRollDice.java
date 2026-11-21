package forge.game.event;

import java.util.List;

public record GameEventRollDice(int sides, List<Integer> results) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}