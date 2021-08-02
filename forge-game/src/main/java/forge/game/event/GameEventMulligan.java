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
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
