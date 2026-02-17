package forge.game.event;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.game.zone.Zone;
import forge.game.zone.ZoneView;
import forge.util.TextUtil;

public record GameEventCardChangeZone(CardView card, ZoneView from, ZoneView to) implements GameEvent {

    public GameEventCardChangeZone(Card card, Zone zoneFrom, Zone zoneTo) {
        this(CardView.get(card),
             zoneFrom == null ? null : new ZoneView(PlayerView.get(zoneFrom.getPlayer()), zoneFrom.getZoneType()),
             zoneTo == null ? null : new ZoneView(PlayerView.get(zoneTo.getPlayer()), zoneTo.getZoneType()));
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
        final String fromStr = from != null ? "" + from.zoneType() : "null";
        final String toStr = to != null ? "" + to.zoneType() : "null";
        return TextUtil.concatWithSpace("" + card, ":", TextUtil.enclosedBracket(fromStr), "->", TextUtil.enclosedBracket(toStr));
    }
}
