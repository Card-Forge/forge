package forge.game.event;

public class GameEventCounterAdded extends GameEvent {
    public final int Amount;

    public GameEventCounterAdded(int n) {
        Amount = n;
    }
    
    @Override
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
}