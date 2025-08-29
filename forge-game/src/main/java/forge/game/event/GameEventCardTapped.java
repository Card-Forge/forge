package forge.game.event;

import forge.game.card.Card;

public record GameEventCardTapped(Card card, boolean tapped) implements GameEvent {

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
