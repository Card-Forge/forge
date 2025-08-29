package forge.game.event;

import forge.game.player.Player;

public record GameEventSpeedChanged(Player player, int oldValue, int newValue) implements GameEvent {
    @Override
    public <T> T visit(IGameEventVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
