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
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
