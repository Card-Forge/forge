package forge.gui.events;

import forge.game.GameEntityView;
import forge.game.card.CardView;

public record UiEventAttackerDeclared(CardView attacker, GameEntityView defender) implements UiEvent {

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
