package forge.game.event;

/** 
 * 
 *
 */
public class CounterRemovedEvent extends Event {
    public final int Amount;

    public CounterRemovedEvent(int n) {
        Amount = n;
    }
}
