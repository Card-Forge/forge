package forge.gui.events;

import forge.gamemodes.match.NextGameDecision;
import forge.player.PlayerControllerHuman;

public record UiEventNextGameDecision(PlayerControllerHuman controller, NextGameDecision decision) implements UiEvent {

    @Override
    public <T> T visit(IUiEventVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
