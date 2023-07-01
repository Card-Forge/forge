package forge.game.event;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.TextUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventZone extends GameEvent {

    public final ZoneType zoneType;
    public final Player player;
    public final EventValueChangeType mode;
    public final Card card;

    public GameEventZone(ZoneType zoneType, Player player, EventValueChangeType added, Card c) {
        this.zoneType = zoneType;
        this.player = player;
        this.mode = added;
        this.card = c;
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
        String owners = player == null ? "Game" : Lang.getInstance().getPossesive(player.getName());
        return card == null
                ? TextUtil.concatWithSpace(owners, zoneType.toString(), ":", mode.toString())
                : TextUtil.concatWithSpace(owners, zoneType.toString(), ":", mode.toString(), card.toString()
        );
    }

}
