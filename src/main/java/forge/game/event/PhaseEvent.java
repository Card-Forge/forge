package forge.game.event;

import forge.game.phase.PhaseType;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PhaseEvent extends Event {

    public final Player playerTurn;
    public final PhaseType phase;
    public final String phaseDesc;

    public PhaseEvent(Player player, PhaseType ph, String desc) {
        playerTurn = player;
        phase = ph;
        phaseDesc = desc;
    }

}
