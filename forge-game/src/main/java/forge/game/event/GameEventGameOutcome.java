package forge.game.event;

import forge.game.GameOutcome;

import java.util.List;

public class GameEventGameOutcome extends GameEvent {
    public final GameOutcome result;
    public final List<GameOutcome> history;

    public GameEventGameOutcome(GameOutcome lastOne, List<GameOutcome> history) {
        this.result = lastOne;
        this.history = history;
    }
    
    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}