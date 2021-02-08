package forge.game.event;

import forge.game.GameOutcome;

import java.util.Collection;

public class GameEventGameOutcome extends GameEvent {
    public final GameOutcome result;
    public final Collection<GameOutcome> history;

    public GameEventGameOutcome(GameOutcome lastOne, Collection<GameOutcome> history) {
        this.result = lastOne;
        this.history = history;
    }
    
    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}