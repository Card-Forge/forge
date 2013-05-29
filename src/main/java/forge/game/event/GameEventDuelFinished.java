package forge.game.event;

public class GameEventDuelFinished extends GameEvent {
    
    
    
    @Override
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
    
} // need this class to launch after log was built via previous event