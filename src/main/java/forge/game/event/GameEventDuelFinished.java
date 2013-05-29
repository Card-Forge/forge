package forge.game.event;

public class GameEventDuelFinished extends GameEvent {
    
    
    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
} // need this class to launch after log was built via previous event