package forge.game.event;

import forge.Card;
import forge.game.player.Player;

/** 
 * 
 *
 */
public class GameEventPlayerPoisoned extends GameEvent {
    public final Player receiver;
    public final Card source;
    public final int amount;

    public GameEventPlayerPoisoned(Player recv, Card src, int n) {
        receiver = recv;
        source = src;
        amount = n;
    }

    public GameEventPlayerPoisoned(Player recv, Card src) {
        this(recv, src, 1);
    }
    
    
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
