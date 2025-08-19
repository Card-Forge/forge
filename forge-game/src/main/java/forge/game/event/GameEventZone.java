package forge.game.event;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Lang;
import forge.util.TextUtil;

/**
 * Represents a game event related to a card or ability entering or leaving a zone.
 * Stores information about the affected zone, player, card, and spell ability.
 * Used for tracking zone changes such as casting, moving, or activating cards and abilities.
 */
public class GameEventZone extends GameEvent {

    public final ZoneType zoneType;
    public final Player player;
    public final EventValueChangeType mode;
    public final Card card;
    public final SpellAbility sa;

    public GameEventZone(ZoneType zoneType, Player player, EventValueChangeType added, Card c) {
        this.zoneType = zoneType;
        this.player = player;
        this.mode = added;
        this.card = c;
        this.sa = null;
    }

    public GameEventZone(ZoneType zoneType, SpellAbility sa, EventValueChangeType added) {
        this.zoneType = zoneType;
        this.player = sa.getActivatingPlayer();
        this.mode = added;
        this.card = sa.getHostCard();
        this.sa = sa;
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
        return card == null && sa == null ?
            TextUtil.concatWithSpace(owners, zoneType.toString(), ":", mode.toString()) :
            TextUtil.concatWithSpace(owners, zoneType.toString(), ":", mode.toString(), "" + (sa == null ? card : sa));
    }

}
