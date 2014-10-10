package forge.events;

import forge.game.card.CardView;

public class UiEventBlockerAssigned extends UiEvent {
    public final CardView blocker;
    public final CardView attackerBeingBlocked; 

    public UiEventBlockerAssigned(final CardView card, final CardView currentAttacker) {
        blocker = card;
        attackerBeingBlocked = currentAttacker;
    }

    @Override
    public <T> T visit(final IUiEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}