package forge.events;

import forge.game.GameEntityView;
import forge.game.card.CardView;

public class UiEventAttackerDeclared extends UiEvent {
    public final CardView attacker; 
    public final GameEntityView defender;

    public UiEventAttackerDeclared(final CardView card, final GameEntityView currentDefender) {
        attacker = card;
        defender = currentDefender;
    }

    @Override
    public <T> T visit(final IUiEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return attacker.toString() + ( defender == null ? " removed from combat" : " declared to attack " + defender ); 
    }
}
