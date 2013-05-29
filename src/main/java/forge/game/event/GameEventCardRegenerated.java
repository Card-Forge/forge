package forge.game.event;

public class GameEventCardRegenerated extends GameEvent {
    
    @Override
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
}
