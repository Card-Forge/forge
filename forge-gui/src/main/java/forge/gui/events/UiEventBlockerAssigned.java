package forge.gui.events;

import forge.Card;

public class UiEventBlockerAssigned extends UiEvent {

    public final Card blocker;
    public final Card attackerBeingBlocked; 

    public UiEventBlockerAssigned(Card card, Card currentAttacker) {
        blocker = card;
        attackerBeingBlocked = currentAttacker;
    }

    @Override
    public <T> T visit(IUiEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
    

}