package forge.game.event;

import forge.game.card.Card;
import forge.game.zone.Zone;
import forge.util.TextUtil;

public record GameEventCardChangeZone(Card card, Zone from, Zone to) implements GameEvent {

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

