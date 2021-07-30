package forge.game.event;

import com.google.common.collect.Multimap;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventAttackersDeclared extends GameEvent {

    public final Player player;
    public final Multimap<GameEntity, Card> attackersMap;

    public GameEventAttackersDeclared(Player playerTurn, Multimap<GameEntity, Card> attackersMap) {
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
