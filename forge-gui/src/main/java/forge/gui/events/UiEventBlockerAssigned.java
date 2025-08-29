package forge.gui.events;

import forge.game.card.CardView;

public record UiEventBlockerAssigned(CardView blocker, CardView attackerBeingBlocked) implements UiEvent {

    @Override
    public <T> T visit(final IUiEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}