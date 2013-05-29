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
    public <T, U> U visit(IGameEventVisitor<T, U> visitor, T params) {
        return visitor.visit(this, params);
    }
}
