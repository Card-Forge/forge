package forge.game.event;

import forge.game.card.Card;
import forge.game.zone.Zone;
import forge.util.TextUtil;

public class GameEventCardChangeZone extends GameEvent {
    
    public final Card card;
    public final Zone from;
    public final Zone to;

    public GameEventCardChangeZone(Card c, Zone zoneFrom, Zone zoneTo) {
        card = c;
        from = zoneFrom;
        to = zoneTo;
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
        return TextUtil.concatWithSpace(card.toString(),":", TextUtil.enclosedBracket(from.toString()),"->", TextUtil.enclosedBracket(to.toString()));
    }
}

