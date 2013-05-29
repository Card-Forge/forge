package forge.game.event;

import forge.game.phase.PhaseType;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventTurnPhase extends GameEvent {

    public final Player playerTurn;
    public final PhaseType phase;
    public final String phaseDesc;

    public GameEventTurnPhase(Player player, PhaseType ph, String desc) {
        playerTurn = player;
        phase = ph;
        phaseDesc = desc;
    }

    
    @Override
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
}
