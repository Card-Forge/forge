package forge.game.event;

import forge.Card;
import forge.game.player.Player;

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


