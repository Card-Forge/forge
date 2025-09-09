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
public record GameEventZone(ZoneType zoneType, Player player, EventValueChangeType mode, Card card, SpellAbility sa) implements GameEvent {

    public GameEventZone(ZoneType zoneType, Player player, EventValueChangeType added, Card c) {
        this(zoneType, player, added, c, null);
    }

    public GameEventZone(ZoneType zoneType, SpellAbility sa, EventValueChangeType added) {
        this(zoneType, sa.getActivatingPlayer(), added, sa.getHostCard(), sa);
    }

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
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
