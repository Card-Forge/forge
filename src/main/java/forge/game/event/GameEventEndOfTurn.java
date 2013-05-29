package forge.game.event;

public class GameEventEndOfTurn extends GameEvent {
    
    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
