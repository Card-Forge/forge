package forge.game.event;

/** 
 * 
 *
 */
public class GameEventCounterRemoved extends GameEvent {
    public final int Amount;

    public GameEventCounterRemoved(int n) {
        Amount = n;
    }
    
    @Override
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
}
