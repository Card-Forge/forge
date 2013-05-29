package forge.game.event;

import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventMulligan extends GameEvent {

    public final Player player;
    public GameEventMulligan(Player p) {
        player = p;
    }
    
    
    @Override
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
}
