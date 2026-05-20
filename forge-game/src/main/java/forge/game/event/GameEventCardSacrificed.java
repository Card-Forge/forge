package forge.game.event;

import forge.game.card.CardView;

public record GameEventCardSacrificed(CardView card) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "" + card.getController() + " sacrificed " + card;
    }
}
