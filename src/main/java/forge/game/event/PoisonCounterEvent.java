package forge.game.event;

import forge.Card;
import forge.game.player.Player;

/** 
 * 
 *
 */
public class PoisonCounterEvent {
    public final Player Reciever;
    public final Card Source;
    public final int Amount;
    
    public PoisonCounterEvent(Player recv, Card src, int n) {
        Reciever = recv;
        Source = src;
        Amount = n;
    }
    
    public PoisonCounterEvent(Player recv, Card src) { 
        this(recv, src, 1);
    }
}
