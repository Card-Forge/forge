package forge.game.event;

import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class MulliganEvent extends Event {

    public final Player player;
    public MulliganEvent(Player p) {
        player = p;
    }
}
