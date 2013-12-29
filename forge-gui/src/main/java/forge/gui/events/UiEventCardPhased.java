package forge.gui.events;

import forge.game.card.Card;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class UiEventCardPhased extends UiEvent {

    public final Card phasingCard; 
    public final boolean phaseState;
    
    public UiEventCardPhased(Card card, boolean state) {
        phasingCard = card;
        phaseState = state;
    }

    @Override
    public <T> T visit(IUiEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return phasingCard != null ? phasingCard.toString() : "(unknown)" + " changed its phased-out state to " + phaseState; 
    }
}

