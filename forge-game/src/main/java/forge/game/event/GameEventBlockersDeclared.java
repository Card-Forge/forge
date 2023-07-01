package forge.game.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.util.Lang;
import forge.util.TextUtil;
import forge.util.maps.MapOfLists;

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
        List<Card> blockerCards = new ArrayList<>();
        for (MapOfLists<Card, Card> vv : blockers.values()) {
            for (Collection<Card> cc : vv.values()) {
                blockerCards.addAll(cc);
            }
        }
        return TextUtil.concatWithSpace(defendingPlayer.getName(),"declared", String.valueOf(blockerCards.size()),"blockers:", Lang.joinHomogenous(blockerCards) );
    }
}
