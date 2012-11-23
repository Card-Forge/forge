package forge.game.event;

/** 
 * 
 *
 */
public class AddCounterEvent extends Event {
    public final int Amount;
    
    public AddCounterEvent(int n) {
        Amount = n;
    }
}

