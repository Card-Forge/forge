package forge.game.event;

import java.util.Map;

import forge.Card;
import forge.GameEntity;
import forge.util.maps.MapOfLists;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventBlockersDeclared extends GameEvent {

    public final Map<GameEntity, MapOfLists<Card, Card>> blockers;
    
    public GameEventBlockersDeclared(Map<GameEntity, MapOfLists<Card, Card>> blockers) {
        this.blockers = blockers;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        // TODO Auto-generated method stub
        return visitor.visit(this);
    }

}
