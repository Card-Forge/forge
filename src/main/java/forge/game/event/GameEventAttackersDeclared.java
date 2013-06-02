package forge.game.event;

import forge.Card;
import forge.GameEntity;
import forge.game.player.Player;
import forge.util.maps.MapOfLists;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventAttackersDeclared extends GameEvent {

    public final Player player;
    public final MapOfLists<GameEntity, Card> attackersMap;
    
    public GameEventAttackersDeclared(Player playerTurn, MapOfLists<GameEntity, Card> attackersMap) {
        this.player = playerTurn;
        this.attackersMap = attackersMap;
    }

    /* (non-Javadoc)
     * @see forge.game.event.GameEvent#visit(forge.game.event.IGameEventVisitor)
     */
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        // TODO Auto-generated method stub
        return visitor.visit(this);
    }

}
