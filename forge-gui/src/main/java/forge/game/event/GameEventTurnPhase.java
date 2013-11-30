package forge.game.event;

import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.util.Lang;

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
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        String playerName = Lang.getPossesive(playerTurn.getName());
        return String.format("%s turn, %s%s phase", playerName, phaseDesc, phase.nameForUi );
    }
}
