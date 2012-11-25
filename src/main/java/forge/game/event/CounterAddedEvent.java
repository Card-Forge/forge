package forge.game.event;

/** 
 * 
 *
 */
public class CounterAddedEvent extends Event {
    public final int Amount;

    public CounterAddedEvent(int n) {
        Amount = n;
    }
}

