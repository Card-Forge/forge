package forge.game.event;

import forge.Card;
import forge.game.player.Player;

/** 
 * 
 *
 */
public class PoisonCounterEvent extends Event {
    public final Player Receiver;
    public final Card Source;
    public final int Amount;
    
    public PoisonCounterEvent(Player recv, Card src, int n) {
        Receiver = recv;
        Source = src;
        Amount = n;
    }
    
    public PoisonCounterEvent(Player recv, Card src) { 
        this(recv, src, 1);
    }
}
