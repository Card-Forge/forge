package forge.game.event;

import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

public record GameEventCardChangeZone(CardView card, ZoneType from, ZoneType to) implements GameEvent {

    public GameEventCardChangeZone(Card card, Zone zoneFrom, Zone zoneTo) {
        this(CardView.get(card), zoneFrom == null ? null : zoneFrom.getZoneType(), zoneTo == null ? null : zoneTo.getZoneType());
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
        return TextUtil.concatWithSpace("" + card, ":", TextUtil.enclosedBracket("" + from), "->", TextUtil.enclosedBracket("" + to));
    }
}

