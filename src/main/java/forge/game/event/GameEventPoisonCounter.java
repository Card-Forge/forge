package forge.game.event;

import forge.Card;
import forge.game.player.Player;

/** 
 * 
 *
 */
public class GameEventPoisonCounter extends GameEvent {
    public final Player Receiver;
    public final Card Source;
    public final int Amount;

    public GameEventPoisonCounter(Player recv, Card src, int n) {
        Receiver = recv;
        Source = src;
        Amount = n;
    }

    public GameEventPoisonCounter(Player recv, Card src) {
        this(recv, src, 1);
    }
    
    
    @Override
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
}
