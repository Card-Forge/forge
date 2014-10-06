package forge.game.event;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.util.Lang;
import forge.util.maps.MapOfLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventBlockersDeclared extends GameEvent {

    public final Map<GameEntity, MapOfLists<Card, Card>> blockers;
    public final Player defendingPlayer;
    
    public GameEventBlockersDeclared(Player who, Map<GameEntity, MapOfLists<Card, Card>> blockers) {
        this.blockers = blockers;
        defendingPlayer = who;
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        // TODO Auto-generated method stub
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        List<Card> blockerCards = new ArrayList<Card>();
        for (MapOfLists<Card, Card> vv : blockers.values()) {
            for (Collection<Card> cc : vv.values()) {
                blockerCards.addAll(cc);
            }
        }
        return String.format("%s declared %d blockers: %s", defendingPlayer.getName(), blockerCards.size(), Lang.joinHomogenous(blockerCards) );
    }
}
