package forge.game.event;

public class GameEventCounterAdded extends GameEvent {
    public final int Amount;

    public GameEventCounterAdded(int n) {
        Amount = n;
    }
    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}