package forge.game.event;

import forge.Card;
import forge.game.player.Player;

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

