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
}
