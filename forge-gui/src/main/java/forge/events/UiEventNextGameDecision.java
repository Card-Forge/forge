package forge.events;

import forge.match.NextGameDecision;
import forge.player.PlayerControllerHuman;

public final class UiEventNextGameDecision extends UiEvent {

    private final PlayerControllerHuman controller;
    private final NextGameDecision decision;

    public UiEventNextGameDecision(final PlayerControllerHuman controller, final NextGameDecision decision) {
        this.controller = controller;
        this.decision = decision;
    }

    public PlayerControllerHuman getController() {
        return controller;
    }
    public NextGameDecision getDecision() {
        return decision;
    }

    @Override
    public <T> T visit(IUiEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
