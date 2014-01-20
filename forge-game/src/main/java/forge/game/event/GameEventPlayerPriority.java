package forge.game.event;

import forge.game.phase.PhaseType;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventPlayerPriority extends GameEvent {

    public final Player turn;
    public final PhaseType phase;
    public final Player priority;
    
    public GameEventPlayerPriority(Player playerTurn, PhaseType phase, Player priorityPlayer) {
        turn = playerTurn;
        this.phase = phase;
        priority = priorityPlayer;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return String.format("Priority - %s", priority.getName());
    }
}
