package forge.game.event;

import forge.game.card.Card;

public record GameEventCardPhased(Card card, boolean phaseState) implements GameEvent {

    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return card != null ? card.toString() : "(unknown)" + " changed its phased-out state to " + phaseState; 
    }
}

