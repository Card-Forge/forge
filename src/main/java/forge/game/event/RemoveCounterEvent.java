package forge.game.event;

/** 
 * 
 *
 */
public class RemoveCounterEvent extends Event {
    public final int Amount;

    public RemoveCounterEvent(int n) {
        Amount = n;
    }
}
