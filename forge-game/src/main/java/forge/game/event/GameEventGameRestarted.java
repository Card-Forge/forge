package forge.game.event;

import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventGameRestarted extends GameEvent {

    public final Player whoRestarted; 

    public GameEventGameRestarted(Player playerTurn) {
        whoRestarted = playerTurn;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
