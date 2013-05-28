package forge.game.event;

import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameRestartedEvent extends Event {

    public final Player whoRestarted; 
    
    public GameRestartedEvent(Player playerTurn) {
        whoRestarted = playerTurn;
    }

}
