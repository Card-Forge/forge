package forge.gui.events;

import forge.Card;
import forge.GameEntity;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class UiEventAttackerDeclared extends UiEvent {

    public final Card attacker; 
    public final GameEntity defender;
    
    public UiEventAttackerDeclared(Card card, GameEntity currentDefender) {
        attacker = card;
        defender = currentDefender;
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
        return attacker.toString() + ( defender == null ? " removed from combat" : "declared to attack " + defender.getName() ); 
    }
}
