package forge.game.event;

import forge.game.player.Player;

/** 
 * 
 *
 */
public class GameEventPlayerPoisoned extends GameEvent {
    public final Player receiver;
    public final Player source;
    public final int oldValue;
    public final int amount;

    public GameEventPlayerPoisoned(Player recv, Player src, int old, int num) {
        receiver = recv;
        source = src;
        oldValue = old;
        amount = num;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
