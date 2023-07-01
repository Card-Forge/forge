package forge.game.event;

import forge.game.card.Card;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class GameEventCardPhased extends GameEvent {

    public final Card card; 
    public final boolean phaseState;

    public GameEventCardPhased(Card card, boolean state) {
        this.card = card;
        phaseState = state;
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
        return card != null ? card.toString() : "(unknown)" + " changed its phased-out state to " + phaseState; 
    }
}

