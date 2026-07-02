package forge.game.event;

import forge.game.card.Card;
import forge.game.card.CardView;

public record GameEventCardTapped(CardView card, boolean tapped) implements GameEvent {

    public GameEventCardTapped(Card card, boolean tapped) {
        this(CardView.get(card), tapped);
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
        return "" + card.getController() + (tapped ? " tapped " : " untapped ") + card;
    }
}
